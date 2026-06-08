package ai.agent.engine.graph;

import ai.agent.constant.GraphConstants;
import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.dto.groupChat.TaskEvaluationDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.engine.graph.node.ChatBeginNode;
import ai.agent.engine.graph.node.ChatEndNode;
import ai.agent.engine.graph.node.ExecutorNode;
import ai.agent.engine.graph.node.RouterNode;
import ai.agent.engine.graph.node.canvas.CanvasScenePlaningNode;
import ai.agent.engine.graph.node.canvas.GeneralPlaningNode;
import ai.agent.engine.graph.node.reflection.ContentFilterNode;
import ai.agent.engine.graph.node.reflection.CuratorNode;
import ai.agent.engine.graph.node.reflection.ReflectorNode;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Comment("图-引擎")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-01", updateTime = "2025-12-01"
)
public class GraphEngine {
    // 最大图-节点循环次数
    private static final int MAX_LOOP = 10;
    private static final Logger log = LoggerFactory.getLogger(GraphEngine.class);

    // 后台任务
    private boolean isBackTask;

    // 当前运行线程
    private volatile Thread runnerThread;

    // 入口节点
    private GraphNode entryNode;

    // 节点列表
    private Map<String, GraphNode> graphNodeMap;

    // 初始数据
    private Map<String, Object> beginningData;

    // 只允许Graph.Scene进行构造，或自行创建NodeMap
    private GraphEngine() {
    }

    public GraphEngine(GraphNode entryNode, Map<String, GraphNode> graphNodeMap,
                       Map<String, Object> beginningData, boolean isBackTask) {
        if (entryNode == null) throw new RuntimeException("入口节点不能为空");
        if (graphNodeMap == null || graphNodeMap.isEmpty()) throw new RuntimeException("节点列表不能为空");

        this.entryNode = entryNode;
        this.graphNodeMap = graphNodeMap;
        this.beginningData = beginningData;
        this.isBackTask = isBackTask;
    }


    // ========================= 核心方法 =========================

    // 执行方法
    public void start(GroupChatEngine chatEngine, AgentInstance agentInstance) {

        // 断言全部参数都存在
        assertAllParamExisted(chatEngine, agentInstance);

        // 记录当前执行线程
        this.runnerThread = Thread.currentThread();

        // 初始上下文
        GraphContext ctx = new GraphContext();
        ctx.setChatEngine(chatEngine);
        ctx.setAgentInstance(agentInstance);
        ctx.setBeginningData(this.beginningData);
        ctx.setBackTask(this.isBackTask);

        // 默认启动节点
        ctx.setNextNode(this.entryNode.getName());

        try {
            // 通知对话开始
            if (!this.isBackTask) GroupChatMessageSender.Chat.chatBegin(chatEngine);

            // 将触发消息加入消息队列（子会话场景）
            Message triggerMessage = ctx.getBeginningData(GraphConstants.PARAM_TRIGGER_MESSAGE, Message.class);
            if (triggerMessage != null) {
                chatEngine.getMessageHistoryManager().addMessage(chatEngine.getBusDomain(), triggerMessage);
            }

            // 图引擎-启动
            onRunning(chatEngine, ctx);

        } finally {
            // 通知对话结束
            if (!this.isBackTask) GroupChatMessageSender.Chat.chatEnd(chatEngine);
        }


    }


    // 核心方法
    private void onRunning(GroupChatEngine chatEngine, GraphContext ctx) {
        while (!ctx.isFinished()) {
            String currentNodeName = ctx.getNextNode();
            ConsolePrintUtil.printGreenLn(StrUtil.format("当前节点: {}", currentNodeName));

            if (!onValidating(currentNodeName, ctx)) {
                break;
            }

            GraphNode currentNode = graphNodeMap.get(currentNodeName);
            NodeExecutionResult result = onExecuting(currentNode, ctx);

            if (result == null) {
                ConsolePrintUtil.printRedLn(StrUtil.format("节点 {} 返回null结果", currentNode.getName()));
                break;
            }

            if (onRetrying(currentNodeName, result, ctx)) {
                continue;
            }

            onProcessingResult(currentNodeName, result, ctx);

            if (!onFlowControlling(currentNodeName, result, ctx)) {
                break;
            }
        }
    }

    // 验证节点是否存在且未超过最大循环次数
    private boolean onValidating(String nodeName, GraphContext ctx) {
        if (StrUtil.isBlank(nodeName) || !graphNodeMap.containsKey(nodeName)) {
            ConsolePrintUtil.printRedLn(StrUtil.format("找不到节点: {}", nodeName));
            return false;
        }

        if (ctx.increaseLoopCounter() >= MAX_LOOP) {
            ConsolePrintUtil.printRedLn("超过最大循环次数");
            return false;
        }

        return true;
    }

    // 执行节点并捕获异常
    private NodeExecutionResult onExecuting(GraphNode node, GraphContext ctx) {
        try {
            return node.execute(ctx);

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                ConsolePrintUtil.printRedLn(StrUtil.format("节点 {} 被打断", node.getName()));
                return null;
            }

            ConsolePrintUtil.printRedLn(StrUtil.format("节点 {} 执行异常: {}", node.getName(),
                    ExceptionUtils.getFullStackTrace(e)));
            return NodeExecutionResult.error("节点执行异常: " + e.getMessage());
        }
    }

    // 处理重试逻辑
    private boolean onRetrying(String nodeName, NodeExecutionResult result, GraphContext ctx) {
        if (!result.isShouldRetry()) {
            return false;
        }

        int retryCount = ctx.increaseNodeRetryCount(nodeName);

        if (result.getMaxRetryCount() > 0 && retryCount > result.getMaxRetryCount()) {
            ConsolePrintUtil.printRedLn(StrUtil.format("节点 {} 重试次数超限({}次)",
                    nodeName, result.getMaxRetryCount()));
            ctx.setFinished(true);
            return false;
        }

        if (result.getRetryAfterSeconds() > 0) {
            try {
                ConsolePrintUtil.printYellowLn(StrUtil.format("节点 {} 将在{}秒后重试",
                        nodeName, result.getRetryAfterSeconds()));
                Thread.sleep(result.getRetryAfterSeconds() * 1000L);
            } catch (InterruptedException ie) {
                ConsolePrintUtil.printRedLn("重试等待被打断");
                return false;
            }
        }

        ConsolePrintUtil.printYellowLn(StrUtil.format("重试节点: {}, 原因: {}, 第{}次重试",
                nodeName, result.getRetryReason(), retryCount));
        return true;
    }

    // 处理执行结果（重置重试计数、合并过程数据、输出摘要）
    private void onProcessingResult(String nodeName, NodeExecutionResult result, GraphContext ctx) {
        if (result.isSuccess()) {
            ctx.resetNodeRetryCount(nodeName);
        }

        if (result.getProcessData() != null) {
            result.getProcessData().forEach(ctx::putProcessData);
        }

        if (StrUtil.isNotBlank(result.getExecutionSummary())) {
            ConsolePrintUtil.printCyanLn(StrUtil.format("[{}] {}", nodeName, result.getExecutionSummary()));
        }
    }

    // 处理流转控制（终止或跳转到下一个节点）
    private boolean onFlowControlling(String nodeName, NodeExecutionResult result, GraphContext ctx) {
        if (result.isShouldTerminate()) {
            ConsolePrintUtil.printGreenLn(StrUtil.format("节点 {} 请求终止执行, 原因: {}",
                    nodeName, result.getRoutingReason()));
            ctx.setFinished(true);
            return false;
        }

        if (StrUtil.isNotBlank(result.getNextNode())) {
            ctx.setNextNode(result.getNextNode());
            if (StrUtil.isNotBlank(result.getRoutingReason())) {
                ConsolePrintUtil.printCyanLn(StrUtil.format("流转原因: {}", result.getRoutingReason()));
            }
            return true;
        } else {
            ConsolePrintUtil.printRedLn(StrUtil.format("节点 {} 未指定下一个节点", nodeName));
            return false;
        }
    }


    // 打断引擎
    public void interrupt() {
        runnerThread.interrupt();
    }

    // 检查引擎是否仍在运行
    public boolean isRunning() {
        return runnerThread != null && runnerThread.isAlive();
    }

    // 等待引擎完成（带超时）
    public void awaitCompletion(long timeoutMillis) {
        if (runnerThread != null && runnerThread.isAlive()) {
            try {
                runnerThread.join(timeoutMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    // ========================= 场景设置 =========================

    // 场景
    public static class Scene {

        // 聊天场景
        public static GraphEngine newChat(Message triggerMessage) {

            if (triggerMessage == null) throw new RuntimeException("触发消息不能为空");

            // 图-节点列表
            Map<String, GraphNode> graphMap = new HashMap<>();

            // 对话开始
            ChatBeginNode chatBeginNode = new ChatBeginNode();
            graphMap.put(GraphConstants.NODE_CHAT_BEGIN, chatBeginNode);

            // 路由节点
            graphMap.put(GraphConstants.NODE_ROUTER, new RouterNode());

            // 规划节点-画布
            graphMap.put(GraphConstants.NODE_CANVAS_PLANING, new CanvasScenePlaningNode());
            // 规划节点-通用
            graphMap.put(GraphConstants.NODE_GENERAL_PLANING, new GeneralPlaningNode());
            // 执行节点
            graphMap.put(GraphConstants.NODE_EXECUTOR, new ExecutorNode());

            // 对话结束
            graphMap.put(GraphConstants.NODE_CHAT_END, new ChatEndNode());

            return new GraphEngine(chatBeginNode, graphMap,
                    MapUtil.of(GraphConstants.PARAM_TRIGGER_MESSAGE, triggerMessage), false);

        }

        // 反思场景
        public static GraphEngine newReflection(TaskEvaluationDto taskEvaluationDto) {

            if (taskEvaluationDto == null) throw new RuntimeException("任务评价不能为空");


            // 图-节点列表
            Map<String, GraphNode> graphMap = new HashMap<>();

            // 内容过滤节点（入口）
            ContentFilterNode contentFilterNode = new ContentFilterNode();
            graphMap.put(GraphConstants.NODE_CONTENT_FILTER, contentFilterNode);

            // 反思节点
            graphMap.put(GraphConstants.NODE_REFLECTOR, new ReflectorNode());

            // 策展节点
            graphMap.put(GraphConstants.NODE_CURATOR, new CuratorNode());

            return new GraphEngine(contentFilterNode, graphMap,
                    MapUtil.of(GraphConstants.PARAM_TASK_EVALUATION, taskEvaluationDto), true);

        }


    }


    // ========================= 支撑方法 =========================

    // 断言所有参数都存在
    private void assertAllParamExisted(GroupChatEngine chatEngine, AgentInstance agentInstance) {
        if (chatEngine == null || agentInstance == null)
            throw new RuntimeException("聊天引擎以及智能体实例不得为空");
        if (this.entryNode == null) throw new RuntimeException("入口节点不能为空");
        if (this.graphNodeMap == null || graphNodeMap.isEmpty()) throw new RuntimeException("节点列表不能为空");
    }

}
