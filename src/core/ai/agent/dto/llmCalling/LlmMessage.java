package ai.agent.dto.llmCalling;

import java.io.Serializable;
import java.util.List;

public class LlmMessage implements Serializable {

    // 系统角色
    public static final String Role_System = "system";
    // 用户角色
    public static final String Role_User = "user";
    // 智能体角色
    public static final String Role_Assistant = "assistant";
    // 工具角色
    public static final String Role_Tool = "tool";

    private String role; // "system" | "user" | "assistant" | "tool"
    private String content;
    
    // Function Calling相关字段
    private List<ToolCall> toolCalls;  // assistant角色的工具调用列表
    private String toolCallId;          // tool角色的工具调用ID
    
    // DeepSeek思考模式相关字段
    private String reasoningContent;    // 思考内容（tool call场景必须传回）
    
    // 伪装标记：用于标识伪装的画布动作消息
    private boolean isFakeCanvasActionMessage = false;

    public LlmMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LlmMessage setRole(String role) {
        this.role = role;
        return this;
    }

    public LlmMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public LlmMessage setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
        return this;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public LlmMessage setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
        return this;
    }

    public boolean isFakeCanvasActionMessage() {
        return isFakeCanvasActionMessage;
    }

    public LlmMessage setFakeCanvasActionMessage(boolean fakeCanvasActionMessage) {
        isFakeCanvasActionMessage = fakeCanvasActionMessage;
        return this;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public LlmMessage setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
        return this;
    }
}

