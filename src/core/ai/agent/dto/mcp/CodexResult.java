package ai.agent.dto.mcp;

/**
 * Codex 工具执行结果
 * 对应 structuredContent.result 中的内容
 */
public class CodexResult {
    
    private Boolean success;
    private String SESSION_ID;
    private String agent_messages;
    
    public CodexResult() {
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getSESSION_ID() {
        return SESSION_ID;
    }
    
    public void setSESSION_ID(String SESSION_ID) {
        this.SESSION_ID = SESSION_ID;
    }
    
    public String getAgent_messages() {
        return agent_messages;
    }
    
    public void setAgent_messages(String agent_messages) {
        this.agent_messages = agent_messages;
    }
    
    /**
     * 判断是否执行成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
    
    @Override
    public String toString() {
        return "CodexResult{" +
                "success=" + success +
                ", SESSION_ID='" + SESSION_ID + '\'' +
                ", agent_messages='" + agent_messages + '\'' +
                '}';
    }
}
