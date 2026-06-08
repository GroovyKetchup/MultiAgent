package ai.agent.engine.groupChat.adapter;

import ai.agent.dto.groupChat.ExecutionTraceDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.plan.PlanStep;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.util.groupChat.MessageBuilder;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 群组聊天工具上下文适配器
 * 将GroupChatEngine适配为ToolContext接口
 */
public class GroupChatToolContextAdapter implements ToolContext {
    private final GroupChatEngine chatEngine;
    private String currentAgentId; // 当前执行工具的智能体ID
    private PlanStep currentExecutePlanStep; // 当前要执行的计划步骤
    // 执行轨迹
    private List<ExecutionTraceDto> executionTraces;

    public GroupChatToolContextAdapter(GroupChatEngine chatEngine) {
        this.chatEngine = chatEngine;
    }

    public GroupChatToolContextAdapter(GroupChatEngine chatEngine, String currentAgentId) {
        this.chatEngine = chatEngine;
        this.currentAgentId = currentAgentId;
    }

    /**
     * 获取群组实例ID
     */
    public String getGroupInstanceId() {
        return chatEngine.getGroupChatInstance().getInstanceId();
    }

    /**
     * 获取GroupChatEngine实例
     */
    public GroupChatEngine getChatEngine() {
        return chatEngine;
    }

    /**
     * 获取当前智能体ID
     */
    public String getCurrentAgentId() {
        return currentAgentId;
    }

    /**
     * 设置当前智能体ID
     */
    public void setCurrentAgentId(String currentAgentId) {
        this.currentAgentId = currentAgentId;
    }

    public PlanStep getCurrentExecutePlanStep() {
        return currentExecutePlanStep;
    }

    public GroupChatToolContextAdapter setCurrentExecutePlanStep(PlanStep currentExecutePlanStep) {
        this.currentExecutePlanStep = currentExecutePlanStep;
        return this;
    }

    public List<ExecutionTraceDto> getExecutionTraces() {
        return executionTraces;
    }

    public GroupChatToolContextAdapter setExecutionTraces(List<ExecutionTraceDto> executionTraces) {
        this.executionTraces = executionTraces;
        return this;
    }

    // ========================= 帮助方法 =========================


    // 添加执行轨迹
    public void addExecutionTrace(ExecutionTraceDto executionTrace) {
        if (executionTrace == null) return;
        if (this.executionTraces == null) {
            this.executionTraces = new ArrayList<>();
        }
        this.executionTraces.add(executionTrace);
    }


    // 添加步骤执行信息（文本）
    public void addStepExecutionTextMessage(String message) {
        if (StrUtil.isBlank(message)) return;

        String currentAgentId = this.getCurrentAgentId();
        if (StrUtil.isBlank(currentAgentId)) return;

        PlanStep currentExecutePlanStep = this.getCurrentExecutePlanStep();
        if (currentExecutePlanStep == null) return;

        currentExecutePlanStep.addExecutionMessage(
                MessageBuilder.createAgentTextMessage(
                        currentAgentId,
                        message,
                        null
                )
        );
    }


    /**
     * 格式化消息为聊天历史格式
     */
    private String formatMessageForChatHistory(Message message) {
        switch (message.getMessageType()) {
            case TEXT:
                return message.getTextPayload().getText();

            case PLAN:
                StringBuilder planText = new StringBuilder();
                planText.append("制定了计划：").append(message.getPlanPayload().getDescription()).append("\n");
                if (message.getPlanPayload().getPlan() != null) {
                    message.getPlanPayload().getPlan().getSteps().forEach(step -> {
                        planText.append("步骤").append(step.getId()).append(": ").append(step.getGoal()).append("\n");
                    });
                }
                return planText.toString();

            case TOOL_CALL:
                return "调用工具: " + message.getToolCallPayload().getToolCall().getToolName() +
                        " (状态: " + message.getToolCallPayload().getStatus() + ")";

            default:
                return message.toString();
        }
    }

}
