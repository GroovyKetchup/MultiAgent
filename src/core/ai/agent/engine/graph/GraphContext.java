package ai.agent.engine.graph;

import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Comment("图-上下文")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-01", updateTime = "2025-12-01"
)
public class GraphContext implements Serializable {

    // === 0、系统数据 ===
    private String playBook; // 核心操作手册（ACE）

    // === 1. 基础数据 ===
    private GroupChatEngine chatEngine; // 群聊引擎
    private AgentInstance agentInstance; // 智能体实例
    private Map<String, Object> beginningData;       // 初始的数据
    private boolean isBackTask;

    // === 2. 过程数据 ===
    private Map<String, Object> inProcessData;       // 过程中产生的数据

    // === 3. 核心流转控制 ===
    private Thread runnerThread; // 当前正在运行的线程
    private AtomicInteger loopCounter; // 循环计数器
    private String nextNode;       // 要流转的节点
    private boolean isFinished = false; // 是否结束
    private Map<String, Integer> nodeRetryCounters; // 节点级重试计数器

    public GraphContext() {
    }

    public GraphContext(GroupChatEngine chatEngine, AgentInstance agentInstance) {
        this.chatEngine = chatEngine;
        this.agentInstance = agentInstance;
    }


    // ========================= 支撑方法 =========================

    // 添加初始数据
    public void putBeginningData(String key, Object value) {
        if (beginningData == null) beginningData = new HashMap<>();
        beginningData.put(key, value);
    }

    // 获取初始数据
    public <T> T getBeginningData(String key, Class<T> classz) {
        if (beginningData == null || !beginningData.containsKey(key)) return null;
        Object dataObj = beginningData.get(key);
        if (classz.isInstance(dataObj)) return (T) dataObj;
        return null;
    }

    // 添加过程数据
    public void putProcessData(String key, Object value) {
        if (inProcessData == null) inProcessData = new HashMap<>();
        inProcessData.put(key, value);
    }

    // 获取过程数据
    public <T> T getProcessData(String key, Class<T> classz) {
        if (inProcessData == null || !inProcessData.containsKey(key)) return null;
        Object dataObj = inProcessData.get(key);
        if (classz.isInstance(dataObj)) return (T) dataObj;
        return null;
    }

    // 循环计数器增加一次
    public int increaseLoopCounter() {
        if (loopCounter == null) loopCounter = new AtomicInteger(0);
        return loopCounter.incrementAndGet();
    }

    // 获取某个节点的重试次数
    public int getNodeRetryCount(String nodeName) {
        if (nodeRetryCounters == null) return 0;
        return nodeRetryCounters.getOrDefault(nodeName, 0);
    }

    // 增加节点重试次数
    public int increaseNodeRetryCount(String nodeName) {
        if (nodeRetryCounters == null) nodeRetryCounters = new HashMap<>();
        int count = nodeRetryCounters.getOrDefault(nodeName, 0) + 1;
        nodeRetryCounters.put(nodeName, count);
        return count;
    }

    // 重置节点重试次数
    public void resetNodeRetryCount(String nodeName) {
        if (nodeRetryCounters != null) {
            nodeRetryCounters.remove(nodeName);
        }
    }

    // ========================= getter/setter =========================


    public Thread getRunnerThread() {
        return runnerThread;
    }

    public GraphContext setRunnerThread(Thread runnerThread) {
        this.runnerThread = runnerThread;
        return this;
    }

    public GroupChatEngine getChatEngine() {
        return chatEngine;
    }

    public GraphContext setChatEngine(GroupChatEngine chatEngine) {
        this.chatEngine = chatEngine;
        return this;
    }

    public AgentInstance getAgentInstance() {
        return agentInstance;
    }

    public GraphContext setAgentInstance(AgentInstance agentInstance) {
        this.agentInstance = agentInstance;
        return this;
    }

    public Map<String, Object> getBeginningData() {
        return beginningData;
    }

    public GraphContext setBeginningData(Map<String, Object> beginningData) {
        this.beginningData = beginningData;
        return this;
    }

    public Map<String, Object> getInProcessData() {
        return inProcessData;
    }

    public GraphContext setInProcessData(Map<String, Object> inProcessData) {
        this.inProcessData = inProcessData;
        return this;
    }

    public String getNextNode() {
        return nextNode;
    }

    public GraphContext setNextNode(String nextNode) {
        this.nextNode = nextNode;
        return this;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public GraphContext setFinished(boolean finished) {
        isFinished = finished;
        return this;
    }

    public AtomicInteger getLoopCounter() {
        return loopCounter;
    }

    public GraphContext setLoopCounter(AtomicInteger loopCounter) {
        this.loopCounter = loopCounter;
        return this;
    }

    public String getPlayBook() {
        return playBook;
    }

    public GraphContext setPlayBook(String playBook) {
        this.playBook = playBook;
        return this;
    }

    public boolean isBackTask() {
        return isBackTask;
    }

    public GraphContext setBackTask(boolean backTask) {
        isBackTask = backTask;
        return this;
    }
}
