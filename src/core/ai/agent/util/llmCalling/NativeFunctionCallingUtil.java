package ai.agent.util.llmCalling;

import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.dto.llmCalling.LlmUsageStats;
import ai.agent.dto.llmCalling.ToolCall;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.util.ConsolePrintUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 原生Function Calling适配器
 * 
 * 负责将系统的工具定义转换为标准的OpenAI Function Calling格式
 * 并解析模型返回的Function Calling结果
 * 
 * 设计目标：
 * 1. 支持标准的OpenAI Function Calling格式
 * 2. 兼容其他支持Function Calling的模型（如智谱AI、DeepSeek等）
 * 3. 提供统一的工具调用接口
 */
public class NativeFunctionCallingUtil {

    /**
     * 将系统工具转换为OpenAI Function格式
     */
    public static List<Map<String, Object>> convertToolsToFunctions(List<Tool> tools) {
        List<Map<String, Object>> functions = new ArrayList<>();
        
        for (Tool tool : tools) {
            Map<String, Object> function = new HashMap<>();
            function.put("type", "function");
            
            Map<String, Object> functionDef = new HashMap<>();
            functionDef.put("name", tool.getName());
            // 描述中包含中文名称，帮助模型理解
            String description = tool.getCnName() != null && !tool.getCnName().isEmpty() 
                ? tool.getCnName() + " - " + tool.getDescription() 
                : tool.getDescription();
            functionDef.put("description", description);
            
            // 转换参数定义
            if (tool.getParameterSchema() != null) {
                functionDef.put("parameters", tool.getParameterSchema());
            } else {
                // 如果没有参数定义，创建一个空的object schema
                Map<String, Object> emptyParams = new HashMap<>();
                emptyParams.put("type", "object");
                emptyParams.put("properties", new HashMap<>());
                emptyParams.put("required", new ArrayList<>());
                functionDef.put("parameters", emptyParams);
            }
            
            function.put("function", functionDef);
            functions.add(function);
        }
        

        return functions;
    }

    /**
     * 解析模型返回的Function Calling结果
     * 
     * @param llmResponse 模型的原始响应
     * @return FunctionCallingResult 解析结果
     */
    public static FunctionCallingResult parseFunctionCallingResponse(String llmResponse) {
        FunctionCallingResult result = new FunctionCallingResult();
        
        try {
            // 尝试解析JSON格式的响应
            if (llmResponse.trim().startsWith("{")) {
                Map<String, Object> responseMap = JSONUtil.toBean(llmResponse, Map.class);
                
                // 检查是否包含choices字段（OpenAI格式）
                if (responseMap.containsKey("choices")) {
                    FunctionCallingResult openAIResult = parseOpenAIResponse(responseMap);
                    openAIResult.setUsageStats(LlmUsageParser.parseUsage(responseMap));
                    return openAIResult;
                }
                
                // 检查是否包含tool_calls字段
                if (responseMap.containsKey("tool_calls")) {
                    FunctionCallingResult directResult = parseDirectToolCalls(responseMap);
                    directResult.setUsageStats(LlmUsageParser.parseUsage(responseMap));
                    return directResult;
                }
            }
            
            // 如果不是JSON格式，作为纯文本处理
            result.setTextContent(llmResponse);
            result.setToolCalls(new ArrayList<>());
            
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[Function Calling] 解析响应失败: {}", e.getMessage()));
            
            // 解析失败，作为纯文本处理
            result.setTextContent(llmResponse);
            result.setToolCalls(new ArrayList<>());
        }
        
        return result;
    }

    /**
     * 解析OpenAI标准格式的响应
     */
    private static FunctionCallingResult parseOpenAIResponse(Map<String, Object> responseMap) {
        FunctionCallingResult result = new FunctionCallingResult();
        List<ToolCall> toolCalls = new ArrayList<>();
        StringBuilder textContent = new StringBuilder();
        
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                
                if (message != null) {
                    // 提取文本内容
                    Object content = message.get("content");
                    if (content != null) {
                        textContent.append(content.toString());
                    }
                    
                    // 提取DeepSeek思考内容（tool call场景必须传回）
                    Object reasoningContent = message.get("reasoning_content");
                    if (reasoningContent != null) {
                        result.setReasoningContent(reasoningContent.toString());
                        ConsolePrintUtil.printYellowLn(StrUtil.format("[Function Calling] 检测到reasoning_content，长度: {}", 
                                reasoningContent.toString().length()));
                    }
                    
                    // 提取工具调用
                    List<Map<String, Object>> toolCallsData = (List<Map<String, Object>>) message.get("tool_calls");
                    if (toolCallsData != null) {
                        for (Map<String, Object> toolCallData : toolCallsData) {
                            ToolCall toolCall = parseToolCallData(toolCallData);
                            if (toolCall != null) {
                                toolCalls.add(toolCall);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[Function Calling] 解析OpenAI响应失败: {}", e.getMessage()));
        }
        
        result.setTextContent(textContent.toString());
        result.setToolCalls(toolCalls);
        return result;
    }

    /**
     * 解析直接包含tool_calls的响应
     */
    private static FunctionCallingResult parseDirectToolCalls(Map<String, Object> responseMap) {
        FunctionCallingResult result = new FunctionCallingResult();
        List<ToolCall> toolCalls = new ArrayList<>();
        
        try {
            // 提取文本内容
            Object content = responseMap.get("content");
            if (content != null) {
                result.setTextContent(content.toString());
            }
            
            // 提取工具调用
            List<Map<String, Object>> toolCallsData = (List<Map<String, Object>>) responseMap.get("tool_calls");
            if (toolCallsData != null) {
                for (Map<String, Object> toolCallData : toolCallsData) {
                    ToolCall toolCall = parseToolCallData(toolCallData);
                    if (toolCall != null) {
                        toolCalls.add(toolCall);
                    }
                }
            }
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[Function Calling] 解析直接tool_calls失败: {}", e.getMessage()));
        }
        
        result.setToolCalls(toolCalls);
        return result;
    }

    /**
     * 解析单个工具调用数据
     */
    private static ToolCall parseToolCallData(Map<String, Object> toolCallData) {
        try {
            String id = (String) toolCallData.get("id");
            String type = (String) toolCallData.get("type");
            
            if (!"function".equals(type)) {
                return null; // 只处理function类型的调用
            }
            
            Map<String, Object> functionData = (Map<String, Object>) toolCallData.get("function");
            if (functionData == null) {
                return null;
            }
            
            String name = (String) functionData.get("name");
            String argumentsStr = (String) functionData.get("arguments");
            
            Map<String, Object> arguments = new HashMap<>();
            if (StrUtil.isNotBlank(argumentsStr)) {
                try {
                    arguments = JSONUtil.toBean(argumentsStr, Map.class);
                } catch (Exception e) {
                    ConsolePrintUtil.printRedLn(StrUtil.format("[Function Calling] 解析工具参数失败: {}", e.getMessage()));
                }
            }
            
            ToolCall toolCall = new ToolCall(name, arguments);
            toolCall.setId(id); // 设置工具调用ID
            
            ConsolePrintUtil.printGreenLn(StrUtil.format("[Function Calling] 解析到工具调用: {} ({})", 
                name, id));
            
            return toolCall;
            
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[Function Calling] 解析工具调用数据失败: {}", e.getMessage()));
            return null;
        }
    }

    /**
     * 构建包含Function Calling的消息
     */
    public static List<Map<String, Object>> buildFunctionCallingMessages(List<LlmMessage> messages) {
        List<Map<String, Object>> formattedMessages = new ArrayList<>();
        
        for (LlmMessage msg : messages) {
            Map<String, Object> formattedMsg = new HashMap<>();
            formattedMsg.put("role", msg.getRole());
            
            // 处理内容
            if (StrUtil.isNotBlank(msg.getContent())) {
                formattedMsg.put("content", msg.getContent());
            }
            
            // 处理工具调用（assistant消息）
            if ("assistant".equals(msg.getRole()) && msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                
                for (ToolCall toolCall : msg.getToolCalls()) {
                    Map<String, Object> toolCallData = new HashMap<>();
                    toolCallData.put("id", toolCall.getId());
                    toolCallData.put("type", "function");
                    
                    Map<String, Object> functionData = new HashMap<>();
                    functionData.put("name", toolCall.getToolName());
                    functionData.put("arguments", JSONUtil.toJsonStr(toolCall.getParams()));
                    
                    toolCallData.put("function", functionData);
                    toolCalls.add(toolCallData);
                }
                
                formattedMsg.put("tool_calls", toolCalls);
            }
            
            // 处理工具结果（tool消息）
            if ("tool".equals(msg.getRole())) {
                formattedMsg.put("tool_call_id", msg.getToolCallId());
            }
            
            formattedMessages.add(formattedMsg);
        }
        
        return formattedMessages;
    }

    /**
     * Function Calling解析结果
     */
    public static class FunctionCallingResult {
        private String textContent;
        private List<ToolCall> toolCalls;
        private LlmUsageStats usageStats;
        private String reasoningContent;

        public FunctionCallingResult() {
            this.textContent = "";
            this.toolCalls = new ArrayList<>();
        }

        public String getTextContent() {
            return textContent;
        }

        public void setTextContent(String textContent) {
            this.textContent = textContent;
        }

        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }

        public void setToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }

        public boolean hasTextContent() {
            return StrUtil.isNotBlank(textContent);
        }

        public LlmUsageStats getUsageStats() {
            return usageStats;
        }

        public void setUsageStats(LlmUsageStats usageStats) {
            this.usageStats = usageStats;
        }

        public String getReasoningContent() {
            return reasoningContent;
        }

        public void setReasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
        }
    }
}
