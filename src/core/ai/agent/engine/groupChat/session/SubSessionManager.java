package ai.agent.engine.groupChat.session;

import ai.agent.constant.SubSessionConstants;
import ai.agent.dto.groupChat.session.SubSessionDto;
import ai.agent.engine.graph.GraphEngine;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.enums.AgentStatus;
import ai.agent.service.groupChat.InMemoryMessageHistoryManager;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import ai.agent.util.llmCalling.ToolSkipStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class SubSessionManager {

    private static final int MAX_CONCURRENT = SubSessionConstants.MAX_CONCURRENT_SESSIONS;
    private static final int COMPLETED_RETENTION_MINUTES = SubSessionConstants.COMPLETED_RETENTION_MINUTES;

    private final Map<String, SubSessionInstance> sessions = new ConcurrentHashMap<>();
    private final Map<String, GroupChatEngine> engines = new ConcurrentHashMap<>();
    private final Queue<String> pendingQueue = new ConcurrentLinkedQueue<>();

    // ========================= 核心方法 =========================

    // 创建子会话
    public SubSessionInstance createSubSession(String parentSessionId, AgentDefinition agentDefinition,
                                               String sessionName, SubSessionMode sessionMode, GroupChatEngine parentEngine) {

        // 如果 AgentDefinition 没有 LLMConfig，使用父引擎的配置
        if (agentDefinition.getLlmConfig() == null) {
            agentDefinition.setLlmConfig(
                    LLMConfigManager.getAutoLlmConfig(LLMConfigManager.AutoModelType.DEFAULT)
            );
        }

        SubSessionInstance subSession = new SubSessionInstance(
                parentSessionId,
                agentDefinition,
                sessionName,
                sessionMode,
                parentEngine.getGroupChatInstance().getLlmClient()
        );

        subSession.setParentEngine(parentEngine);

        sessions.put(subSession.getInstanceId(), subSession);

        return subSession;
    }

    // 启动子会话
    public GroupChatEngine startSubSession(String sessionId) {
        SubSessionInstance subSession = sessions.get(sessionId);
        if (subSession == null) {
            return null;
        }

        if (isQueuingRequired()) {
            pendingQueue.offer(sessionId);
            // 无论同步还是异步模式，都需要等待引擎初始化完成
            waitForSubSessionToStart(sessionId);
            return subSession.getGroupChatEngine();
        }

        return startSubSessionInternal(sessionId);
    }

    // 是否需要排队
    public boolean isQueuingRequired() {
        return getRunningCount() >= MAX_CONCURRENT;
    }

    // 完成子会话
    public void completeSubSession(String sessionId, String result) {
        SubSessionInstance subSession = sessions.get(sessionId);
        if (subSession != null) {
            synchronized (subSession) {
                subSession.markAsCompleted(result);
                subSession.notifyAll();
            }
        }

        GroupChatEngine engine = engines.remove(sessionId);
        if (engine != null) {
            stopSubSessionEngine(engine);
        }

        tryStartNextPending();
    }

    // 取消（失败）子会话
    public void failSubSession(String sessionId, String error) {
        SubSessionInstance subSession = sessions.get(sessionId);
        if (subSession != null) {
            synchronized (subSession) {
                subSession.markAsFailed(error);
                subSession.notifyAll();
                AgentInstance agentInstance = subSession.getAgentInstance();
                if (agentInstance != null) {
                    agentInstance.interrupt();
                }

            }
        }

        GroupChatEngine engine = engines.remove(sessionId);
        if (engine != null) {
            stopSubSessionEngine(engine);
        }

        tryStartNextPending();
    }

    /**
     * 打断子会话执行（从执行状态转为等待用户回复状态）
     * 不终止会话，只是中断当前执行并等待用户下一步指令
     */
    public void interruptSubSession(String sessionId) {
        SubSessionInstance subSession = sessions.get(sessionId);
        GroupChatEngine engine = engines.get(sessionId);

        if (subSession == null || engine == null) {
            ConsolePrintUtil.printYellowLn("[SubSessionManager] 打断失败：子会话不存在 - " + sessionId);
            return;
        }

        // 检查当前状态是否为 RUNNING
        if (subSession.getSubSessionStatus() != SubSessionStatus.RUNNING) {
            ConsolePrintUtil.printYellowLn("[SubSessionManager] 打断跳过：子会话不在执行状态 - " + subSession.getSubSessionStatus());
            return;
        }

        ConsolePrintUtil.printGreenLn("[SubSessionManager] 开始打断子会话 - " + sessionId);

        // 1. 先发送打断通知消息（确保被压缩时包含）
        GroupChatMessageSender.Notice.hint(engine, null, "用户打断了执行");

        // 2. 中断智能体并更新状态
        AgentInstance agent = subSession.getAgentInstance();
        if (agent != null) {
            agent.interrupt();
            // 更新状态为 IDLE（通知前端清理思考状态）
            // submitAgentMessage 的检查已改为只依赖版本号，不依赖状态
            agent.updateAgentStatus(engine, AgentStatus.IDLE);
        }

        // 3. 中断正在运行的 GraphEngine
        Map<String, GraphEngine> runningGraphs = engine.getRunningGraphEngines();
        if (runningGraphs != null && !runningGraphs.isEmpty()) {
            for (GraphEngine graph : runningGraphs.values()) {
                try {
                    graph.interrupt();
                } catch (Exception e) {
                    ConsolePrintUtil.printRedLn("[SubSessionManager] 中断GraphEngine异常: " + e.getMessage());
                }
            }
        }

        // 4. 标记为等待用户回复（不在这里触发压缩，压缩统一由 ExecutorNode.finally 管理）
        synchronized (subSession) {
            subSession.markAsWaitingReply();
        }

        ConsolePrintUtil.printGreenLn("[SubSessionManager] 子会话已打断，等待用户回复 - " + sessionId);
    }

    // 回复子会话
    public void replyToSubSession(String sessionId, String reply) {
        SubSessionInstance subSession = sessions.get(sessionId);
        GroupChatEngine engine = engines.get(sessionId);

        if (engine != null && subSession != null) {
            synchronized (subSession) {
                subSession.markAsRunning();
            }

            // 等待压缩完成（历史总结会在 ExecutorNode 构建系统提示词时自动加入）
            // 注意：不再等待旧的 GraphEngine 完成，因为 pendingInterrupt 机制会拒绝旧消息
            waitForSummaryCompletion(subSession);

            // 直接发送用户原始消息，历史总结已在系统提示词中
            engine.submitUserMessage(reply);

            subSession.completeUserReply(reply);
        }
    }

    /**
     * 等待压缩任务完成
     * @param subSession 子会话实例
     */
    private void waitForSummaryCompletion(SubSessionInstance subSession) {
        if (subSession.hasPendingSummary()) {
            ConsolePrintUtil.printGreenLn("[SubSessionManager] 等待压缩任务完成...");
            subSession.awaitAndGetAllSummaries(30);
            ConsolePrintUtil.printGreenLn("[SubSessionManager] 压缩任务已完成，历史总结将加入系统提示词");
        }
    }

    // 移除子会话
    public void removeSubSession(String sessionId) {
        SubSessionInstance subSession = sessions.remove(sessionId);
        GroupChatEngine engine = engines.remove(sessionId);

        if (engine != null) {
            stopSubSessionEngine(engine);
        }

        pendingQueue.remove(sessionId);

        tryStartNextPending();
    }


    // 获取子会话
    public SubSessionInstance getSubSession(String sessionId) {
        return sessions.get(sessionId);
    }

    // 获取引擎
    public GroupChatEngine getEngine(String sessionId) {
        return engines.get(sessionId);
    }

    // 获取所有子会话
    public List<SubSessionDto> getAllSubSessionDtos() {
        return sessions.values().stream()
                .map(SubSessionDto::fromInstance)
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // 获取指定父会话的子会话
    public List<SubSessionDto> getSubSessionDtosByParent(String parentSessionId) {
        return sessions.values().stream()
                .filter(s -> s.getParentSessionId().equals(parentSessionId))
                .map(SubSessionDto::fromInstance)
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // 清理完成的子会话
    public void cleanupCompletedSessions() {
        long now = System.currentTimeMillis();
        long retentionTime = COMPLETED_RETENTION_MINUTES * 60 * 1000;

        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, SubSessionInstance> entry : sessions.entrySet()) {
            SubSessionInstance session = entry.getValue();

            if (session.getSubSessionStatus() == SubSessionStatus.COMPLETED ||
                    session.getSubSessionStatus() == SubSessionStatus.FAILED) {

                if (session.getCompletedAt() != null &&
                        now - session.getCompletedAt() > retentionTime) {
                    toRemove.add(entry.getKey());
                }
            }
        }

        for (String sessionId : toRemove) {
            sessions.remove(sessionId);
            engines.remove(sessionId);
        }
    }

    // 停止所有子会话
    public void stopAllSubSessions() {
        for (String sessionId : new ArrayList<>(engines.keySet())) {
            GroupChatEngine engine = engines.get(sessionId);
            if (engine != null) {
                stopSubSessionEngine(engine);
            }
        }

        engines.clear();
        sessions.clear();
        pendingQueue.clear();
    }


    // ========================= 支撑方法 =========================

    private GroupChatEngine startSubSessionInternal(String sessionId) {
        SubSessionInstance subSession = sessions.get(sessionId);
        if (subSession == null) {
            return null;
        }

        if (subSession.getSubSessionStatus() == SubSessionStatus.PENDING) {
            GroupChatEngine engine = new GroupChatEngine(subSession);
            engine.setMessageHistoryManager(new InMemoryMessageHistoryManager());
            engine.setToolSkipStrategy(ToolSkipStrategy.onSubSessionMode);

            if (subSession.getParentEngine() != null) {
                engine.setBusDomain(subSession.getParentEngine().getBusDomain());
            }

            engines.put(sessionId, engine);
            subSession.setGroupChatEngine(engine);
            subSession.markAsRunning();
            engine.start();
        }

        return subSession.getGroupChatEngine();
    }

    private int getRunningCount() {
        return (int) sessions.values().stream()
                .filter(s -> s.getSubSessionStatus() == SubSessionStatus.RUNNING
                        || s.getSubSessionStatus() == SubSessionStatus.WAITING_REPLY)
                .count();
    }

    private void tryStartNextPending() {
        if (getRunningCount() < MAX_CONCURRENT && !pendingQueue.isEmpty()) {
            String sessionId = pendingQueue.poll();
            if (sessionId != null) {
                SubSessionInstance subSession = sessions.get(sessionId);
                if (subSession != null && subSession.getSubSessionStatus() == SubSessionStatus.PENDING) {
                    startSubSessionInternal(sessionId);

                    synchronized (subSession) {
                        subSession.notifyAll();
                    }
                }
            }
        }
    }


    private void waitForSubSessionToStart(String sessionId) {
        SubSessionInstance subSession = sessions.get(sessionId);
        if (subSession == null) return;

        synchronized (subSession) {
            while (subSession.getSubSessionStatus() == SubSessionStatus.PENDING) {
                try {
                    subSession.wait(60000);
                    if (subSession.getSubSessionStatus() == SubSessionStatus.PENDING) {
                        throw new RuntimeException("子会话启动超时");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待子会话启动被中断");
                }
            }
        }
    }

    private void stopSubSessionEngine(GroupChatEngine engine) {
        engine.setRunning(false);

        Map<String, GraphEngine> runningGraphs = engine.getRunningGraphEngines();
        if (runningGraphs != null && !runningGraphs.isEmpty()) {
            for (GraphEngine graphEngine : runningGraphs.values()) {
                try {
                    graphEngine.interrupt();
                } catch (Exception e) {
                }
            }
        }

        Thread engineThread = engine.getCurrentThread();
        if (engineThread != null && engineThread.isAlive()) {
            engineThread.interrupt();
        }
    }
}
