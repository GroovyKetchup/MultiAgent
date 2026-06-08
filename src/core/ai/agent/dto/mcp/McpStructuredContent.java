package ai.agent.dto.mcp;

/**
 * MCP 结构化内容包装
 */
public class McpStructuredContent {
    
    private CodexResult result;
    
    public McpStructuredContent() {
    }
    
    public CodexResult getResult() {
        return result;
    }
    
    public void setResult(CodexResult result) {
        this.result = result;
    }
    
    @Override
    public String toString() {
        return "McpStructuredContent{" +
                "result=" + result +
                '}';
    }
}
