package ai.agent.dto.groupChat.plan;

import ai.agent.dto.groupChat.message.Message;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 计划DTO：由<Plan>解析得到，用于驱动执行
 */
public class Plan implements Serializable {

    public enum Status {
        CREATED,     // 计划已创建
        EXECUTING,   // 正在执行
        PAUSED,      // 暂停执行
        COMPLETED,   // 执行完成
        FAILED       // 执行失败
    }

    // 计划目标
    private String goal;

    // 计划执行状态
    private Status status = Status.CREATED;
    // 计划所设计的步骤
    private final List<PlanStep> steps = new ArrayList<PlanStep>();
    // 当前所执行步骤序号
    private int currentStepIndex = 0;
    // 创建时间
    private long createdTime = System.currentTimeMillis();
    // 上次更新时间
    private long updateTime = System.currentTimeMillis();

    public Plan() {
    }

    public Plan(String goal) {
        this.goal = goal;
    }

    public String getGoal() {
        return goal;
    }

    public Plan setGoal(String goal) {
        this.goal = goal;
        return this;
    }

    public List<PlanStep> getSteps() {
        return steps;
    }

    public void addStep(PlanStep s) {
        steps.add(s);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status state) {
        this.status = state;
        this.updateTime = System.currentTimeMillis();
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public void setCurrentStepIndex(int index) {
        this.currentStepIndex = index;
        this.updateTime = System.currentTimeMillis();
    }

    public PlanStep getCurrentStep() {
        if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return null;
    }

    public boolean hasNextStep() {
        return currentStepIndex < steps.size() - 1;
    }

    public void moveToNextStep() {
        if (hasNextStep()) {
            currentStepIndex++;
            updateTime = System.currentTimeMillis();
        }
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public Plan setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public Plan setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
        return this;
    }




    // ========================= 支撑方法 =========================

    // 获取当前步骤的执行上下文
    public String buildExecutionContext() {
        StringBuilder context = new StringBuilder();

        context.append("=== 计划执行上下文 ===\n");
        context.append("计划状态: ").append(status).append("\n");
        context.append("当前步骤: ").append(currentStepIndex + 1).append("/").append(steps.size()).append("\n\n");

        // 添加已完成步骤的结果
        for (int i = 0; i < currentStepIndex; i++) {
            PlanStep step = steps.get(i);
            context.append("步骤 ").append(step.getId()).append(" - ")
                    .append(step.getGoal()).append("\n");
            context.append("状态: ").append(step.getStatus()).append("\n");

            if (step.getExecutionResult() != null) {
                context.append("结果: ").append(step.getExecutionResult()).append("\n");
            }

            if (step.getContextSummary() != null) {
                context.append("摘要: ").append(step.getContextSummary()).append("\n");
            }

            context.append("执行时长: ").append(step.getExecutionDuration()).append("ms\n\n");
        }

        return context.toString();
    }

    // 获取当前步骤的前置执行消息
    public String getPreviousStepsExecuteMessage() {
        if (currentStepIndex == 0) {
            return null;
        }

        StringBuilder results = new StringBuilder();
        results.append("前面步骤的执行结果：\n");

        for (int i = 0; i < currentStepIndex; i++) {
            PlanStep step = steps.get(i);
            results.append("步骤 ").append(step.getId()).append(": ");

            List<Message> executionMessages = step.getExecutionMessages();
            if (CollUtil.isNotEmpty(executionMessages)) {
                for (Message executionMessage : executionMessages) {
                    results.append(JSONUtil.toJsonStr(executionMessage.getPayload()));
                }
            } else {
                results.append("执行完成，状态: ").append(step.getStatus());
            }
            results.append("\n");
        }

        return results.toString();
    }


}

