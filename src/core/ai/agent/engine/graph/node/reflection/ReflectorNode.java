package ai.agent.engine.graph.node.reflection;

import ai.agent.constant.GraphConstants;
import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.dto.groupChat.TaskEvaluationDto;
import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.engine.graph.AbstractGraphNode;
import ai.agent.engine.graph.GraphContext;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.util.ConsolePrintUtil;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static ai.agent.constant.GraphConstants.NODE_CURATOR;
import static ai.agent.constant.GraphConstants.NODE_REFLECTOR;

@Comment("反思节点")
@ClassDeclare(
        label = "Reflector",
        what = "根据任务评价推导反思", why = "提取经验教训", how = "LLM分析对话和反馈",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-11", updateTime = "2025-12-11"
)
public class ReflectorNode extends AbstractGraphNode {

    private static final String PROCESS_DATA_KEY_REFLECTION_RESULT = "$REFLECTION_RESULT";

    @Override
    public String getName() {
        return NODE_REFLECTOR;
    }

    @Override
    public NodeExecutionResult execute(GraphContext ctx) throws Exception {

        AgentInstance agentInstance = ctx.getAgentInstance();
        TaskEvaluationDto taskEvaluation = getTaskEvaluation(ctx);

        if (agentInstance == null || taskEvaluation == null) {
            throw new RuntimeException("智能体实例以及任务评价不得为空");
        }

        try {
            List<LlmMessage> messages = buildReflectorPrompt(taskEvaluation);

            String llmResponse = agentInstance.getLlmClient().callChat(
                    agentInstance.getEffectiveLlmConfig(),
                    messages
            );

            ConsolePrintUtil.printGreenLn(StrUtil.format("[反思节点] LLM响应: {}", llmResponse));

            if (StrUtil.isNotBlank(llmResponse)) {
                ReflectionResult result = parseReflectionResult(llmResponse, taskEvaluation);
                setReflectionResult(ctx, result);
                ConsolePrintUtil.printGreenLn(StrUtil.format("[反思节点] 解析结果: {}", JSONUtil.toJsonStr(result)));
            }

            return NodeExecutionResult.toNode(NODE_CURATOR)
                    .withReason("反思完成，进入策展节点")
                    .withSummary("已生成反思结果");

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[反思节点] 执行异常: {}", ExceptionUtils.getFullStackTrace(e)));
            return NodeExecutionResult.error("反思节点执行异常: " + e.getMessage());
        }
    }


    // ========================= 支撑方法 =========================

    // 获取任务评价
    private TaskEvaluationDto getTaskEvaluation(GraphContext ctx) {
        return ctx.getBeginningData(GraphConstants.PARAM_TASK_EVALUATION, TaskEvaluationDto.class);
    }

    // 设置反思结果
    public void setReflectionResult(GraphContext ctx, ReflectionResult result) {
        ctx.putProcessData(PROCESS_DATA_KEY_REFLECTION_RESULT, result);
    }

    // 获取反思结果
    public static ReflectionResult getReflectionResult(GraphContext ctx) {
        return ctx.getProcessData(PROCESS_DATA_KEY_REFLECTION_RESULT, ReflectionResult.class);
    }

    // 构建反思Prompt
    private List<LlmMessage> buildReflectorPrompt(TaskEvaluationDto taskEvaluation) {
        String outputFormatExample = JSONUtil.toJsonPrettyStr(ReflectionResult.buildExample());

        String systemPrompt = StrUtil.format(
                "# Role\n" +
                        "你是一个经验反思器（Reflector）。你的任务是将用户的反馈转化为智能体可执行的经验教训。\n\n" +

                        "# Input Context\n" +
                        "- 选中的对话消息：\n{}\n\n" +
                        "- 情况分类：{}\n" +
                        "- 用户评价：{}\n" +
                        "- 涉及的智能体ID列表：{}\n\n" +

                        "# Task\n" +
                        "**核心原则：优先采纳用户的具体建议**\n\n" +
                        "1. **如果用户评价中包含明确的改进建议**：\n" +
                        "   - 直接采纳用户的建议作为经验内容\n" +
                        "   - 保持用户建议的原始表述和具体性\n" +
                        "   - 结合对话上下文，补充必要的场景信息（如\"在回应用户招呼时\"）\n" +
                        "   - insight应该是用户建议的精炼总结\n" +
                        "   - agentReflections.content应该包含完整的、可操作的建议\n\n" +

                        "2. **如果用户只指出问题但没有给出建议**：\n" +
                        "   - 分析对话中的问题\n" +
                        "   - 提炼出具体的改进方向\n\n" +

                        "3. **为每个涉及的智能体生成针对性的反思内容**\n" +
                        "   - 确保经验具体、可执行\n" +
                        "   - 包含场景描述和具体做法\n\n" +

                        "# Output Format (JSON)\n" +
                        "```json\n{}\n```\n\n" +

                        "# Output Constraint\n" +
                        "1. 仅输出JSON格式，不要输出任何解释或多余字符\n" +
                        "2. insight应简洁明确（一句话）\n" +
                        "3. agentReflections.content应详细具体，包含场景和做法\n" +
                        "4. 优先保留用户建议的原始表述",
                taskEvaluation.getMessageContext(),
                taskEvaluation.getCategory(),
                taskEvaluation.getTaskComment(),
                JSONUtil.toJsonStr(taskEvaluation.getRelatedAgentIds()),
                outputFormatExample
        );

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage(LlmMessage.Role_System, systemPrompt));
        messages.add(new LlmMessage(LlmMessage.Role_User, "请根据上述信息进行反思分析。"));

        return messages;
    }

    // 解析反思结果
    private ReflectionResult parseReflectionResult(String llmResponse, TaskEvaluationDto taskEvaluation) {
        try {
            String jsonStr = extractJson(llmResponse);
            return JSONUtil.toBean(jsonStr, ReflectionResult.class);

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[反思节点] JSON解析失败，使用原始响应: {}", e.getMessage()));

            ReflectionResult result = new ReflectionResult();
            result.setInsight(llmResponse);
            result.setCategory("Unknown");

            List<AgentReflection> agentReflections = new ArrayList<>();
            for (String agentId : taskEvaluation.getRelatedAgentIds()) {
                AgentReflection reflection = new AgentReflection();
                reflection.setAgentId(agentId);
                reflection.setContent(llmResponse);
                agentReflections.add(reflection);
            }
            result.setAgentReflections(agentReflections);
            return result;
        }
    }

    // 提取JSON字符串
    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }


    // ========================= 内部类 =========================

    // 反思结果
    public static class ReflectionResult implements Serializable {
        private String insight;
        private String category;
        private List<AgentReflection> agentReflections;

        public static ReflectionResult buildExample() {
            ReflectionResult example = new ReflectionResult();
            example.setInsight("总结性的经验教训（一句话）");
            example.setCategory("分类标签，如 Java/Logging、Code/Style 等");

            List<AgentReflection> reflections = new ArrayList<>();
            AgentReflection reflection = new AgentReflection();
            reflection.setAgentId("智能体ID");
            reflection.setContent("针对该智能体的具体反思内容");
            reflections.add(reflection);
            example.setAgentReflections(reflections);

            return example;
        }

        public String getInsight() {
            return insight;
        }

        public void setInsight(String insight) {
            this.insight = insight;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public List<AgentReflection> getAgentReflections() {
            return agentReflections;
        }

        public void setAgentReflections(List<AgentReflection> agentReflections) {
            this.agentReflections = agentReflections;
        }

        public AgentReflection getAgentReflection(String agentId) {
            if (CollUtil.isEmpty(agentReflections) || StrUtil.isBlank(agentId)) return null;
            for (AgentReflection reflection : agentReflections) {
                if (agentId.equals(reflection.getAgentId())) return reflection;
            }
            return null;
        }
    }

    // 智能体反思
    public static class AgentReflection implements Serializable {
        private String agentId;
        private String content;

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

}
