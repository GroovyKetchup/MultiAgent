package ai.agent.dto.llmCalling;

import java.util.Map;
import java.util.UUID;

public class ToolCall {
    private String id;  // 工具调用ID，用于Function Calling
    private final String toolName;
    private final Map<String, Object> params;

    public ToolCall(String toolName, Map<String, Object> params) {
        this.id = UUID.randomUUID().toString();  // 自动生成ID
        this.toolName = toolName;
        this.params = params;
    }

    public String getId() { 
        return id; 
    }
    
    public void setId(String id) { 
        this.id = id; 
    }
    
    public String getToolName() { 
        return toolName; 
    }
    
    public Map<String, Object> getParams() { 
        return params; 
    }
}

