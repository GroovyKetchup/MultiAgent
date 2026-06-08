package ai.agent.dto.mcp;

/**
 * MCP (Model Context Protocol) 响应封装
 * 对应 JSON-RPC 2.0 响应格式
 */
public class McpResponse {
    
    private String jsonrpc;
    private Integer id;
    private McpResult result;
    private McpError error;
    
    public McpResponse() {
    }
    
    public String getJsonrpc() {
        return jsonrpc;
    }
    
    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public McpResult getResult() {
        return result;
    }
    
    public void setResult(McpResult result) {
        this.result = result;
    }
    
    public McpError getError() {
        return error;
    }
    
    public void setError(McpError error) {
        this.error = error;
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return error == null && result != null;
    }
    
    @Override
    public String toString() {
        return "McpResponse{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", id=" + id +
                ", result=" + result +
                ", error=" + error +
                '}';
    }
}
