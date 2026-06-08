package ai.agent.dto.graph;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Comment("节点执行结果")
@ClassDeclare(
        label = "NodeExecutionResult",
        what = "节点执行后的流转指令与附加数据",
        why = "替代ctx.setNextNode()，实现返回值驱动的流转控制，支持重试机制",
        how = "封装下一节点、中断标识、重试控制、过程数据等",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-14", updateTime = "2025-12-14"
)
public class NodeExecutionResult implements Serializable {

    private String nextNode;
    private boolean shouldTerminate;
    private boolean shouldRetry;
    private int retryAfterSeconds;
    private int maxRetryCount;
    private String retryReason;
    private Map<String, Object> processData;
    private boolean success;
    private String errorMessage;
    private String executionSummary;
    private Long executionTimeMs;
    private String routingReason;

    public NodeExecutionResult() {
    }

    public static NodeExecutionResult toNode(String nodeName) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.nextNode = nodeName;
        result.success = true;
        return result;
    }

    public static NodeExecutionResult terminate() {
        NodeExecutionResult result = new NodeExecutionResult();
        result.shouldTerminate = true;
        result.success = true;
        return result;
    }

    public static NodeExecutionResult retry(String reason) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.shouldRetry = true;
        result.retryReason = reason;
        result.retryAfterSeconds = 0;
        result.success = false;
        return result;
    }

    public static NodeExecutionResult retryAfter(int seconds, String reason) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.shouldRetry = true;
        result.retryAfterSeconds = seconds;
        result.retryReason = reason;
        result.success = false;
        return result;
    }

    public static NodeExecutionResult error(String errorMsg) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.success = false;
        result.errorMessage = errorMsg;
        result.shouldTerminate = true;
        return result;
    }

    public NodeExecutionResult withProcessData(String key, Object value) {
        if (this.processData == null) this.processData = new HashMap<>();
        this.processData.put(key, value);
        return this;
    }

    public NodeExecutionResult withReason(String reason) {
        this.routingReason = reason;
        return this;
    }

    public NodeExecutionResult withSummary(String summary) {
        this.executionSummary = summary;
        return this;
    }

    public NodeExecutionResult withMaxRetry(int maxRetry) {
        this.maxRetryCount = maxRetry;
        return this;
    }

    public NodeExecutionResult withExecutionTime(Long timeMs) {
        this.executionTimeMs = timeMs;
        return this;
    }

    public String getNextNode() {
        return nextNode;
    }

    public NodeExecutionResult setNextNode(String nextNode) {
        this.nextNode = nextNode;
        return this;
    }

    public boolean isShouldTerminate() {
        return shouldTerminate;
    }

    public NodeExecutionResult setShouldTerminate(boolean shouldTerminate) {
        this.shouldTerminate = shouldTerminate;
        return this;
    }

    public boolean isShouldRetry() {
        return shouldRetry;
    }

    public NodeExecutionResult setShouldRetry(boolean shouldRetry) {
        this.shouldRetry = shouldRetry;
        return this;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public NodeExecutionResult setRetryAfterSeconds(int retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
        return this;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public NodeExecutionResult setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    public String getRetryReason() {
        return retryReason;
    }

    public NodeExecutionResult setRetryReason(String retryReason) {
        this.retryReason = retryReason;
        return this;
    }

    public Map<String, Object> getProcessData() {
        return processData;
    }

    public NodeExecutionResult setProcessData(Map<String, Object> processData) {
        this.processData = processData;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public NodeExecutionResult setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public NodeExecutionResult setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public String getExecutionSummary() {
        return executionSummary;
    }

    public NodeExecutionResult setExecutionSummary(String executionSummary) {
        this.executionSummary = executionSummary;
        return this;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public NodeExecutionResult setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        return this;
    }

    public String getRoutingReason() {
        return routingReason;
    }

    public NodeExecutionResult setRoutingReason(String routingReason) {
        this.routingReason = routingReason;
        return this;
    }
}
