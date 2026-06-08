package ai.agent.dto.groupChat.message.payload;

import ai.agent.dto.groupChat.ExecutionTraceDto;
import ai.agent.dto.llmCalling.ToolCall;
import ai.agent.enums.ToolCallStatus;
import com.alibaba.fastjson2.annotation.JSONType;

import java.util.List;

/**
 * 工具调用消息载荷
 * 用于智能体调用工具的消息，支持执行前后状态更新
 */
@JSONType(typeName = "TOOL_CALL")
public class ToolCallPayload extends MessagePayload {
    // 工具调用
    private ToolCall toolCall;
    // 工具调用
    private ToolCallStatus status;
    // 工具执行结果
    private String result;
    // 错误信息（如果执行失败）
    private String error;
    // 执行耗时（毫秒）
    private long executionTimeMs;
    // 执行轨迹
    private List<ExecutionTraceDto> executionTraces;

    public ToolCallPayload() {
    }

    // 构造函数 - 执行前状态
    public ToolCallPayload(ToolCall toolCall) {
        this.toolCall = toolCall;
        this.status = ToolCallStatus.PENDING;
        this.result = null;
        this.error = null;
        this.executionTimeMs = 0;
    }

    // 构造函数 - 执行后状态
    public ToolCallPayload(ToolCall toolCall, ToolCallStatus status, String result, String error, long executionTimeMs) {
        this.toolCall = toolCall;
        this.status = status;
        this.result = result;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
    }

    public ToolCall getToolCall() {
        return toolCall;
    }

    public ToolCallStatus getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public ToolCallPayload setToolCall(ToolCall toolCall) {
        this.toolCall = toolCall;
        return this;
    }

    public ToolCallPayload setStatus(ToolCallStatus status) {
        this.status = status;
        return this;
    }

    public ToolCallPayload setResult(String result) {
        this.result = result;
        return this;
    }

    public ToolCallPayload setError(String error) {
        this.error = error;
        return this;
    }

    public ToolCallPayload setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        return this;
    }

    /**
     * 判断是否为执行前状态
     */
    public boolean isPending() {
        return status == ToolCallStatus.PENDING;
    }

    /**
     * 判断是否执行成功
     */
    public boolean isSuccess() {
        return status == ToolCallStatus.SUCCESS;
    }

    /**
     * 判断是否执行失败
     */
    public boolean isFailed() {
        return status == ToolCallStatus.FAILED;
    }


    public List<ExecutionTraceDto> getExecutionTraces() {
        return executionTraces;
    }

    public ToolCallPayload setExecutionTraces(List<ExecutionTraceDto> executionTraces) {
        this.executionTraces = executionTraces;
        return this;
    }

    @Override
    public String getPayloadType() {
        return "TOOL_CALL";
    }

    @Override
    public boolean isValid() {
        return toolCall != null && status != null;
    }

    @Override
    public String toString() {
        return "ToolCallPayload{" +
                "toolCall=" + toolCall +
                ", status=" + status +
                ", result='" + result + '\'' +
                ", error='" + error + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }


}
