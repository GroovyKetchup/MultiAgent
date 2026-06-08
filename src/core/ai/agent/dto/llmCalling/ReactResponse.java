package ai.agent.dto.llmCalling;

import java.util.List;

/**
 * ReAct响应数据结构
 */
public class ReactResponse {
    private String textContent;
    private List<ToolCall> toolCalls;
    private LlmUsageStats usageStats;
    private String reasoningContent;

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public LlmUsageStats getUsageStats() {
        return usageStats;
    }

    public void setUsageStats(LlmUsageStats usageStats) {
        this.usageStats = usageStats;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }
}