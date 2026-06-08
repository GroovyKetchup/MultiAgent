package ai.agent.service.llmCalling;

import ai.agent.constant.AppConstants;
import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.dto.llmCalling.ToolCall;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.util.ConsolePrintUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import gpf.exception.VerifyException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class HttpLlmClient implements LLMClient {

    public static void main(String[] args) throws InterruptedException {
        LLMConfig llmConfig = LLMConfigManager.getLlmConfig(
                AppConstants.LLM_MODEL_NAME_DEEPSEEK_CHAT
        );

        String hi = new HttpLlmClient().callLlm(llmConfig, "hi 你是谁");
        System.out.println(hi);

    }

    @Override
    public String callLlm(LLMConfig config, String prompt) throws InterruptedException {
        return callChat(config, CollUtil.newArrayList(new LlmMessage(LlmMessage.Role_User, prompt)));
    }

    @Override
    public String callLlm(LLMConfig config, String systemPrompt, String userPrompt) throws InterruptedException {
        if (StrUtil.isAllBlank(systemPrompt, userPrompt)) throw new VerifyException("系统提示词和用户提示词不可都为空");
        List<LlmMessage> msgs = CollUtil.newArrayList(new LlmMessage(LlmMessage.Role_System, systemPrompt));

        if (StrUtil.isNotBlank(userPrompt)) {
            msgs.add(new LlmMessage(LlmMessage.Role_User, userPrompt));
        }

        return callChat(config, msgs);

    }

    @Override
    public String callChat(LLMConfig config, List<LlmMessage> messages) throws InterruptedException {
        return callChatWithFunctions(config, messages, null);
    }

    @Override
    public String callChatWithFunctions(LLMConfig config, List<LlmMessage> messages, List<Map<String, Object>> tools) throws InterruptedException {
        try {
            // 增加调用次数
            // FIXME 不应该写在这里，后续计划执行和智能体的调用统一入口后迁走
//            IGroupChatUserInfoService.get()
//                    .increaseModelCallingNo();
        } catch (Exception ignored) {
        }

        String baseUrl = config.getBaseUrl();
        if (!baseUrl.contains("/v1")) {
            baseUrl += "/v1";
        }


        ConsolePrintUtil.printGreenLn(
                StrUtil.format(
                        "执行模型调用：baseUrl:{},apiKey:{}, targetModel:{}, tools:{}",
                        baseUrl, "*****", config.getModel(), tools != null ? tools.size() : 0
//                        baseUrl, config.getApiKey(), config.getModel()
                )

        );

//        String endpoint = normalizeBase(baseUrl) + "/chat/completions";
        String endpoint = baseUrl + "/chat/completions";
        ConsolePrintUtil.printGreenLn("finalReqUrl:" + endpoint);

        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(300000);
            conn.setReadTimeout(300000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());

            String payload = buildRequestBodyWithTools(config.getModel(), messages, tools);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }


            if (Thread.interrupted()) {
                throw new InterruptedException("线程被打断");
            }


            int code = conn.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String resp = readAll(is);


            if (code < 200 || code >= 300) {
                ConsolePrintUtil.printRedLn(StrUtil.format("大模型调用错误:\n响应码: {}\n请求载荷:{}\n响应内容: {}", code,
                        payload, resp));
                throw new RuntimeException("大模型调用错误，请检查你的配置");
            }


            // 如果有tools，返回原始响应（包含tool_calls）
            // 否则解析content
            if (tools != null && !tools.isEmpty()) {
                ConsolePrintUtil.printGreenLn("返回原始响应(包含tools)");
                return resp;
            }

            String parsed = parseOpenAiContent(resp);
            return parsed != null ? parsed : resp;
        } catch (InterruptedException e) {
            ConsolePrintUtil.printRedLn("模型调用被中断: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            ConsolePrintUtil.printRedLn("模型调用IO异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("模型调用未知异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String normalizeBase(String base) {
        if (base == null || base.isEmpty()) return "";
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }


    private String buildRequestBodyWithTools(String model, List<LlmMessage> messages, List<Map<String, Object>> tools) {
        if (CollUtil.isEmpty(messages)) throw new RuntimeException("要请求的消息不得为空");
        if (messages.size() == 1) {
            // 只有一条消息不能只发送系统指令
            messages.get(0).setRole(LlmMessage.Role_User);
        }


        // 使用FastJSON构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);

        // 构建messages数组
        JSONArray messagesArray = new JSONArray();
        for (LlmMessage m : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", m.getRole());

            // 处理不同角色的消息
            if (LlmMessage.Role_Tool.equals(m.getRole())) {
                // tool角色需要tool_call_id
                msgObj.put("content", m.getContent());
                if (m.getToolCallId() != null) {
                    msgObj.put("tool_call_id", m.getToolCallId());
                }
            } else if (LlmMessage.Role_Assistant.equals(m.getRole()) && m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                // assistant角色如果有tool_calls，需要序列化为OpenAI格式
                msgObj.put("content", m.getContent() != null ? m.getContent() : "");

                JSONArray toolCallsArray = new JSONArray();
                for (ToolCall tc : m.getToolCalls()) {
                    JSONObject toolCallObj = new JSONObject();
                    toolCallObj.put("id", tc.getId());
                    toolCallObj.put("type", "function");

                    JSONObject functionObj = new JSONObject();
                    functionObj.put("name", tc.getToolName());
                    // arguments必须是JSON字符串
                    functionObj.put("arguments", com.alibaba.fastjson2.JSON.toJSONString(tc.getParams()));

                    toolCallObj.put("function", functionObj);
                    toolCallsArray.add(toolCallObj);
                }
                msgObj.put("tool_calls", toolCallsArray);
            } else {
                // 其他角色（system, user, assistant without tool_calls）
                msgObj.put("content", m.getContent());
            }
            
            // DeepSeek思考模式：所有assistant消息都传回reasoning_content（如果有）
            if (LlmMessage.Role_Assistant.equals(m.getRole()) && StrUtil.isNotBlank(m.getReasoningContent())) {
                msgObj.put("reasoning_content", m.getReasoningContent());
                ConsolePrintUtil.printYellowLn(StrUtil.format("[HttpLlmClient] 传回reasoning_content，长度: {}", 
                        m.getReasoningContent().length()));
            }

            messagesArray.add(msgObj);
        }
        requestBody.put("messages", messagesArray);

        // 添加tools参数
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }

        return requestBody.toJSONString();
    }

    // Extremely lightweight JSON extraction; not a full parser.
    private String parseOpenAiContent(String json) {
        if (json == null) return null;
        int idx = json.indexOf("\"message\"");
        if (idx == -1) return null;
        int cIdx = json.indexOf("\"content\"", idx);
        if (cIdx == -1) return null;
        int colon = json.indexOf(':', cIdx);
        if (colon == -1) return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote == -1) return null;
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = firstQuote + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                if (ch == 'n') sb.append('\n');
                else if (ch == 'r') sb.append('\r');
                else if (ch == 't') sb.append('\t');
                else sb.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') break;
            sb.append(ch);
        }
        return sb.toString();
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}

