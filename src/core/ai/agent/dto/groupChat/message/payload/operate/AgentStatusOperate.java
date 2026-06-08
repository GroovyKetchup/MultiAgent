package ai.agent.dto.groupChat.message.payload.operate;

import ai.agent.enums.AgentStatus;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

/**
 * 操作消息数据：智能体状态更新
 * 作为 OperatePayload 的 operateData 数据传输对象
 */
@Comment("智能体状态更新操作数据")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-05", updateTime = "2025-09-05"
)
public class AgentStatusOperate {

    private  String agentId;     // 智能体ID
    private  String agentName;   // 智能体名称
    private  AgentStatus status; // 智能体状态（枚举）
    private  long timestamp;     // 事件时间戳（毫秒）

    public AgentStatusOperate() {
    }

    public AgentStatusOperate(String agentId, String agentName, AgentStatus status, long timestamp) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public AgentStatusOperate setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public AgentStatusOperate setAgentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    public AgentStatusOperate setStatus(AgentStatus status) {
        this.status = status;
        return this;
    }

    public AgentStatusOperate setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}

