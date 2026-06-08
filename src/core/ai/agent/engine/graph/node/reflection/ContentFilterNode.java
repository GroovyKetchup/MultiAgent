package ai.agent.engine.graph.node.reflection;

import ai.agent.constant.GraphConstants;
import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.dto.groupChat.TaskEvaluationDto;
import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.engine.graph.AbstractGraphNode;
import ai.agent.engine.graph.GraphContext;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static ai.agent.constant.GraphConstants.NODE_CONTENT_FILTER;
import static ai.agent.constant.GraphConstants.NODE_REFLECTOR;

@Comment("内容过滤节点")
@ClassDeclare(
        label = "ContentFilter",
        what = "过滤用户反馈，判断是否需要进入反思流程",
        why = "屏蔽无关内容，只对需要改进的负面反馈进行反思",
        how = "LLM分析用户反馈内容的有效性",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-15", updateTime = "2025-12-15"
)
public class ContentFilterNode extends AbstractGraphNode {

    @Override
    public String getName() {
        return NODE_CONTENT_FILTER;
    }

    @Override
    public NodeExecutionResult execute(GraphContext ctx) throws Exception {

        AgentInstance agentInstance = ctx.getAgentInstance();
        TaskEvaluationDto taskEvaluation = getTaskEvaluation(ctx);

        if (agentInstance == null || taskEvaluation == null) {
            throw new RuntimeException("智能体实例以及任务评价不得为空");
        }

        try {

//            ConsolePrintUtil.printGreenLn("taskEvaluation:\n" + JSONUtil.toJsonStr(taskEvaluation));

            List<LlmMessage> messages = buildFilterPrompt(taskEvaluation);

            String llmResponse = agentInstance.getLlmClient().callChat(
                    agentInstance.getEffectiveLlmConfig(),
                    messages
            );

            ConsolePrintUtil.printGreenLn(StrUtil.format("[内容过滤节点] LLM响应: {}", llmResponse));

            if (StrUtil.isNotBlank(llmResponse)) {
                FilterResult result = parseFilterResult(llmResponse);
                ConsolePrintUtil.printGreenLn(StrUtil.format("[内容过滤节点] 解析结果: {}", JSONUtil.toJsonStr(result)));

                if (!result.shouldReflect()) {
                    String reason = StrUtil.isNotBlank(result.getReason())
                            ? result.getReason()
                            : "用户反馈不属于需要改进的负面情况，无需进行反思";

                    GroupChatMessageSender.Notice.hint(
                            ctx.getChatEngine(),
                            agentInstance.getDefinition().getAgentId(),
                            reason
                    );

                    ConsolePrintUtil.printYellowLn(StrUtil.format("[内容过滤节点] 过滤内容: {}", reason));

                    return NodeExecutionResult.terminate()
                            .withReason("内容被过滤，终止反思流程")
                            .withSummary(reason);
                }

                ConsolePrintUtil.printCyanLn("[内容过滤节点] 内容通过过滤，进入反思节点");
            }

            return NodeExecutionResult.toNode(NODE_REFLECTOR)
                    .withReason("内容过滤通过，进入反思节点")
                    .withSummary("用户反馈属于需要改进的负面情况");

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[内容过滤节点] 执行异常: {}", ExceptionUtils.getFullStackTrace(e)));
            return NodeExecutionResult.error("内容过滤节点执行异常: " + e.getMessage());
        }
    }


    // ========================= 支撑方法 =========================

    private TaskEvaluationDto getTaskEvaluation(GraphContext ctx) {
        return ctx.getBeginningData(GraphConstants.PARAM_TASK_EVALUATION, TaskEvaluationDto.class);
    }

    private List<LlmMessage> buildFilterPrompt(TaskEvaluationDto taskEvaluation) {
        String outputFormatExample = JSONUtil.toJsonPrettyStr(FilterResult.buildExample());

        String systemPrompt = StrUtil.format(
                "# Role\n" +
                        "你是一个内容过滤器（Content Filter）。你的任务是判断用户反馈是否属于需要智能体改进的负面情况。\n\n" +

                        "# Input Context\n" +
                        "- 选中的对话消息：\n{}\n\n" +
                        "- 情况分类：{}\n" +
                        "- 用户评价：{}\n" +
                        "- 涉及的智能体ID列表：{}\n\n" +

                        "# Task\n" +
                        "判断用户反馈是否属于以下**需要改进的负面情况**：\n" +
                        "1. **智能体意图识别错误** - 智能体误解了用户的真实意图\n" +
                        "2. **工具选择错误** - 智能体选择了错误的工具或方法\n" +
                        "3. **执行过程繁琐** - 智能体的执行流程过于复杂或冗余\n" +
                        "4. **其他明确的改进点** - 用户明确指出的智能体需要调整的地方\n\n" +

                        "**不应该进入反思的情况**（应该过滤掉）：\n" +
                        "- 用户的正面评价或表扬\n" +
                        "- 随意测试、无关内容\n" +
                        "- 空洞评价、没有具体改进建议\n" +
                        "- 与智能体行为无关的反馈\n\n" +

                        "# Output Format (JSON)\n" +
                        "```json\n{}\n```\n\n" +

                        "# Output Constraint\n" +
                        "仅输出JSON格式，不要输出任何解释或多余字符。",
                taskEvaluation.getMessageContext(),
                taskEvaluation.getCategory(),
                taskEvaluation.getTaskComment(),
                JSONUtil.toJsonStr(taskEvaluation.getRelatedAgentIds()),
                outputFormatExample
        );

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage(LlmMessage.Role_System, systemPrompt));
        messages.add(new LlmMessage(LlmMessage.Role_User, "请判断该用户反馈是否需要进入反思流程。"));

        return messages;
    }

    private FilterResult parseFilterResult(String llmResponse) {
        try {
            String jsonStr = extractJson(llmResponse);
            return JSONUtil.toBean(jsonStr, FilterResult.class);

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[内容过滤节点] JSON解析失败: {}", e.getMessage()));
            FilterResult result = new FilterResult();
            result.setShouldReflect(false);
            result.setReason("解析失败，默认过滤该内容");
            return result;
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }


    // ========================= 内部类 =========================

    public static class FilterResult implements Serializable {
        private Boolean shouldReflect;
        private String reason;

        public static FilterResult buildExample() {
            FilterResult example = new FilterResult();
            example.setShouldReflect(true);
            example.setReason("如果shouldReflect为false，此处说明过滤原因；如果为true，说明属于哪种需要改进的情况");
            return example;
        }

        public Boolean shouldReflect() {
            return shouldReflect != null && shouldReflect;
        }

        public Boolean getShouldReflect() {
            return shouldReflect;
        }

        public void setShouldReflect(Boolean shouldReflect) {
            this.shouldReflect = shouldReflect;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

}
