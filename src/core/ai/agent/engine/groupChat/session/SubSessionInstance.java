package ai.agent.engine.groupChat.session;

import ai.agent.constant.SubSessionConstants;
import ai.agent.dto.groupChat.canvas.CanvasStatus;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.OperatePayload;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.service.groupChat.ChatSummaryCompressor;
import ai.agent.service.groupChat.MessageHistoryManager;
import ai.agent.service.llmCalling.LLMClient;
import ai.agent.util.ConsolePrintUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SubSessionInstance extends GroupChatInstance {

    private final String parentSessionId;
    private final SubSessionMode sessionMode;
    private String sessionName;
    private SubSessionStatus sessionStatus;
    private String taskResult;
    private Long createdAt;
    private Long startedAt;
    private Long completedAt;
    private CanvasStatus canvasStatus;

    @JSONField(serialize = false)
    private GroupChatEngine groupChatEngine;

    @JSONField(serialize = false)
    private GroupChatEngine parentEngine;

    @JSONField(serialize = false)
    private CompletableFuture<String> userReplyFuture;

    // ========================= 会话压缩总结相关 =========================
    private static final int MAX_SUMMARY_HISTORY = 4;  // 保留最近4轮总结

    @JSONField(serialize = false)
    private final LinkedList<SessionSummary> summaryHistory = new LinkedList<>();

    @JSONField(serialize = false)
    private CompletableFuture<SessionSummary> currentSummaryFuture;

    private int currentRoundStartIndex = 0;  // 当前轮次起始消息索引

    public SubSessionInstance(
            String parentSessionId,
            AgentDefinition agentDefinition,
            String sessionName,
            SubSessionMode sessionMode,
            LLMClient llmClient) {

        super(
                SubSessionConstants.SESSION_ID_PREFIX + IdUtil.fastSimpleUUID(),
                createSingleAgentGroupDefinition(agentDefinition),
                llmClient
        );

        this.parentSessionId = parentSessionId;
        this.sessionName = sessionName;
        this.sessionMode = sessionMode;
        this.sessionStatus = SubSessionStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    private static GroupDefinition createSingleAgentGroupDefinition(AgentDefinition agentDef) {
        return new GroupDefinition(
                SubSessionConstants.SESSION_ID_PREFIX + IdUtil.fastSimpleUUID(),
                "SubSession",
                "子会话",
                Collections.singletonList(agentDef),
                agentDef.getLlmConfig()
        );
    }

    public void markAsRunning() {
        this.sessionStatus = SubSessionStatus.RUNNING;
        this.startedAt = System.currentTimeMillis();
    }

    public void markAsCompleted(String result) {
        this.sessionStatus = SubSessionStatus.COMPLETED;
        this.taskResult = result;
        this.completedAt = System.currentTimeMillis();
    }

    public void markAsFailed(String error) {
        this.sessionStatus = SubSessionStatus.FAILED;
        this.taskResult = error;
        this.completedAt = System.currentTimeMillis();
    }

    public void markAsWaitingReply() {
        this.sessionStatus = SubSessionStatus.WAITING_REPLY;
    }


    // ========================= getter/setter =========================


    public AgentDefinition getAgentDefinition() {
        if (getDefinition() != null && getDefinition().getAgentDefinitions() != null
                && !getDefinition().getAgentDefinitions().isEmpty()) {
            return getDefinition().getAgentDefinitions().get(0);
        }
        return null;
    }


    public AgentInstance getAgentInstance() {
        AgentDefinition agentDefinition = getAgentDefinition();
        if (agentDefinition == null) throw new RuntimeException("智能体定义不得为空");
        return getAgent(agentDefinition.getAgentId());
    }

    public void setTaskResult(String result) {
        this.taskResult = result;
    }

    public MessageHistoryManager getMessageHistoryManager() {
        if (groupChatEngine != null) {
            return groupChatEngine.getMessageHistoryManager();
        }
        return null;
    }

    public String getParentSessionId() {
        return parentSessionId;
    }

    public SubSessionMode getSessionMode() {
        return sessionMode;
    }

    public SubSessionStatus getSubSessionStatus() {
        return sessionStatus;
    }

    public String getTaskResult() {
        return taskResult;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public GroupChatEngine getGroupChatEngine() {
        return groupChatEngine;
    }

    public void setGroupChatEngine(GroupChatEngine groupChatEngine) {
        this.groupChatEngine = groupChatEngine;
    }

    public GroupChatEngine getParentEngine() {
        return parentEngine;
    }

    public void setParentEngine(GroupChatEngine parentEngine) {
        this.parentEngine = parentEngine;
    }

    public CanvasStatus getCanvasStatus() {
        return canvasStatus;
    }

    public void setCanvasStatus(CanvasStatus canvasStatus) {
        this.canvasStatus = canvasStatus;
    }

    public boolean hasCanvas() {
        return canvasStatus != null;
    }

    public CompletableFuture<String> getUserReplyFuture() {
        return userReplyFuture;
    }

    public void setUserReplyFuture(CompletableFuture<String> future) {
        this.userReplyFuture = future;
    }

    public void completeUserReply(String reply) {
        if (userReplyFuture != null && !userReplyFuture.isDone()) {
            userReplyFuture.complete(reply);
        }
    }

    // ========================= 会话压缩总结方法 =========================

    /**
     * 异步启动本轮压缩任务
     */
    public void startSummaryCompressionAsync() {
        ConsolePrintUtil.printGreenLn("[SubSession] 触发异步压缩任务...");
        
        if (groupChatEngine == null || getLlmClient() == null) {
            ConsolePrintUtil.printYellowLn("[SubSession] 无法启动压缩：引擎或LLM客户端为空");
            return;
        }

        int roundIndex = summaryHistory.size() + 1;
        ConsolePrintUtil.printGreenLn("[SubSession] 开始第" + roundIndex + "轮压缩，起始消息索引：" + currentRoundStartIndex);

        this.currentSummaryFuture = ChatSummaryCompressor.compressAsync(
                this,
                getLlmClient(),
                getDefinition().getLlmConfig(),
                currentRoundStartIndex,
                roundIndex
        );

        this.currentSummaryFuture.thenAccept(summary -> {
            if (summary != null) {
                synchronized (summaryHistory) {
                    summaryHistory.addLast(summary);
                    while (summaryHistory.size() > MAX_SUMMARY_HISTORY) {
                        summaryHistory.removeFirst();
                    }
                }
                // 更新下一轮起始索引
                MessageHistoryManager historyManager = getMessageHistoryManager();
                if (historyManager != null) {
                    currentRoundStartIndex = historyManager.getFullHistory().size();
                }
            }
        });
    }

    /**
     * 等待当前压缩完成并获取所有历史总结
     * @param timeoutSeconds 超时秒数
     * @return 所有历史总结的格式化文本，超时或无总结返回null
     */
    public String awaitAndGetAllSummaries(long timeoutSeconds) {
        // 先等待当前压缩完成
        if (currentSummaryFuture != null && !currentSummaryFuture.isDone()) {
            try {
                currentSummaryFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                ConsolePrintUtil.printYellowLn("[SubSession] 等待压缩超时");
            } catch (Exception e) {
                ConsolePrintUtil.printRedLn("[SubSession] 等待压缩异常: " + e.getMessage());
            }
        }

        // 构建历史总结文本
        synchronized (summaryHistory) {
            if (summaryHistory.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (SessionSummary summary : summaryHistory) {
                sb.append(StrUtil.format("[第{}轮会话总结]\n{}\n\n",
                        summary.getRoundIndex(), summary.getSummary()));
            }
            return sb.toString();
        }
    }

    /**
     * 获取总结历史队列（只读）
     */
    public LinkedList<SessionSummary> getSummaryHistory() {
        return summaryHistory;
    }

    /**
     * 获取当前所有历史总结的格式化文本（不等待压缩完成）
     * 用于构建系统提示词
     * @return 历史总结文本，无总结返回null
     */
    public String getAllSummariesForPrompt() {
        synchronized (summaryHistory) {
            if (summaryHistory.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (SessionSummary summary : summaryHistory) {
                sb.append(StrUtil.format("[第{}轮会话总结]\n{}\n\n",
                        summary.getRoundIndex(), summary.getSummary()));
            }
            return sb.toString();
        }
    }

    /**
     * 检查是否有待完成的压缩任务
     */
    public boolean hasPendingSummary() {
        return currentSummaryFuture != null && !currentSummaryFuture.isDone();
    }

    public boolean shouldForwardToParent(Message message) {
        if (!message.isOperateMessage()) {
            return false;
        }

        OperatePayload payload = message.getOperatePayload();
        String payloadType = payload.getOperateName();

        boolean isCanvasOpMsg = payloadType.contains("CANVAS");
        ConsolePrintUtil.printGreenLn(
                StrUtil.format("[子会话画布] 收到画布操作消息 - SessionId:{}, Type:{}, Payload:{}",
                        getInstanceId(), payloadType, payload)
        );

        return isCanvasOpMsg;
    }

    public SubSessionInstance setSessionName(String sessionName) {
        this.sessionName = sessionName;
        return this;
    }

    public String getSessionName() {
        return sessionName;
    }
}
