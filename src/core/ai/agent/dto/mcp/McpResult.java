package ai.agent.dto.mcp;

import java.util.List;

/**
 * MCP 响应结果
 */
public class McpResult {
    
    private List<McpContent> content;
    private McpStructuredContent structuredContent;
    private Boolean isError;
    
    public McpResult() {
    }
    
    public List<McpContent> getContent() {
        return content;
    }
    
    public void setContent(List<McpContent> content) {
        this.content = content;
    }
    
    public McpStructuredContent getStructuredContent() {
        return structuredContent;
    }
    
    public void setStructuredContent(McpStructuredContent structuredContent) {
        this.structuredContent = structuredContent;
    }
    
    public Boolean getIsError() {
        return isError;
    }
    
    public void setIsError(Boolean isError) {
        this.isError = isError;
    }
    
    /**
     * 获取第一个文本内容
     */
    public String getFirstTextContent() {
        if (content != null && !content.isEmpty()) {
            McpContent first = content.get(0);
            if ("text".equals(first.getType())) {
                return first.getText();
            }
        }
        return null;
    }
    
    /**
     * 获取 Codex 结果
     */
    public CodexResult getCodexResult() {
        if (structuredContent != null) {
            return structuredContent.getResult();
        }
        return null;
    }
    
    /**
     * 获取 Agent 消息
     */
    public String getAgentMessages() {
        CodexResult codexResult = getCodexResult();
        if (codexResult != null) {
            return codexResult.getAgent_messages();
        }
        return null;
    }
    
    /**
     * 获取 Session ID
     */
    public String getSessionId() {
        CodexResult codexResult = getCodexResult();
        if (codexResult != null) {
            return codexResult.getSESSION_ID();
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "McpResult{" +
                "content=" + content +
                ", structuredContent=" + structuredContent +
                ", isError=" + isError +
                '}';
    }
}
