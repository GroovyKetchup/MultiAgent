package ai.agent.dto.mcp;

/**
 * MCP 内容项
 */
public class McpContent {
    
    private String type;
    private String text;
    
    public McpContent() {
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return "McpContent{" +
                "type='" + type + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
