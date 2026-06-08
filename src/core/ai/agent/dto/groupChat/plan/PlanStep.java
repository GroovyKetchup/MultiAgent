package ai.agent.dto.groupChat.plan;

import ai.agent.dto.groupChat.message.Message;

import java.util.ArrayList;
import java.util.List;

public class PlanStep {

    public enum Status {
        PENDING,      // 待执行
        IN_PROGRESS,  // 执行中（保持兼容）
        EXECUTING,    // 执行中（新状态）
        WAITING,      // 等待用户输入
        DONE,         // 已完成（保持兼容）
        COMPLETED,    // 已完成（新状态）
        FAILED,       // 执行失败
        SKIPPED;      // 已跳过

        public boolean isCompleted() {
            return this == COMPLETED || this == DONE;
        }

        public boolean isFailed() {
            return this == FAILED;
        }

        public boolean isExecuting() {
            return this == EXECUTING || this == IN_PROGRESS;
        }

        public boolean isPending() {
            return this == PENDING;
        }

        public boolean isWaiting() {
            return this == WAITING;
        }
    }

    // 步骤编号
    private final String id;
    // 步骤目标
    private final String goal;
    // 步骤介绍
    private final String description;
    // 当前状态
    private Status status = Status.PENDING;
    // 已重试次数
    private int attempts = 0;
    // 执行中的消息
    private final List<Message> executionMessages = new ArrayList<>();

    // 大模型返回的数据
    private final List<String> llmResponds = new ArrayList<>();

    // 开始时间
    private long startTime = 0; // 步骤开始执行时间
    // 结束时间
    private long endTime = 0;   // 步骤完成时间
    // 步骤执行结果摘要
    private String executionResult = null;
    // 步骤上下文摘要
    private String contextSummary = null;
    private String waitingPrompt = null;
    // 是否在等待用户输入
    private boolean waitingForUserInput = false;

    public PlanStep(String id, String goal) {
        this.id = id;
        this.goal = goal;
        this.description = "";

    }

    public PlanStep(String id, String goal, String description) {
        this.id = id;
        this.goal = goal;
        this.description = description; // 根据目标推断步骤类型
    }

    public String getId() {
        return id;
    }

    public String getGoal() {
        return goal;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.EXECUTING && startTime == 0) {
            startTime = System.currentTimeMillis();
        } else if (status.isCompleted() || status.isFailed()) {
            endTime = System.currentTimeMillis();
        }
    }

    public int getAttempts() {
        return attempts;
    }

    public void incAttempts() {
        this.attempts++;
    }

    public List<Message> getExecutionMessages() {
        return executionMessages;
    }

    public void addExecutionMessage(Message message) {
        executionMessages.add(message);
    }

    public void clearExecutionMessages() {
        executionMessages.clear();
    }


    public List<String> getLlmResponds() {
        return llmResponds;
    }

    public void addLlmResponds(String responds) {
        llmResponds.add(responds);
    }

    public void clearLlmResponds() {
        llmResponds.clear();
    }


    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getExecutionDuration() {
        if (startTime == 0) return 0;
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return end - startTime;
    }

    // 执行结果管理
    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String result) {
        this.executionResult = result;
    }

    // 上下文管理
    public String getContextSummary() {
        return contextSummary;
    }

    public void setContextSummary(String context) {
        this.contextSummary = context;
    }

    // 等待管理
    public String getWaitingPrompt() {
        return waitingPrompt;
    }

    public void setWaitingPrompt(String prompt) {
        this.waitingPrompt = prompt;
    }

    public boolean isWaitingForUserInput() {
        return waitingForUserInput;
    }

    public void setWaitingForUserInput(boolean waiting) {
        this.waitingForUserInput = waiting;
    }



}
