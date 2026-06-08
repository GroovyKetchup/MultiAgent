package ai.agent.dto.groupChat;

import ai.agent.dto.llmCalling.ToolDto;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("智能体信息DTO - 返回给前端的智能体信息")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-02", updateTime = "2025-09-02"
)
public class AgentInfoDto implements Serializable {

    // 智能体Id
    private String agentId;

    // 智能体头像
    private String agentAvatar;

    // 智能体名称
    private String name;


    // 商业职务
    private String agentBusinessRole;

    // 智能体介绍
    private String description;

    // 智能体状态
    private String status;

    // 是否总控智能体
    private boolean isSysScheduler;

    // 是否在线
    private boolean isOnline;  // 是否在线


    // 工具列表（能用的工具）
    private List<ToolDto> tools;

    public AgentInfoDto() {
    }

    public AgentInfoDto(String agentId, String agentAvatar, String name, String agentBusinessRole,String description, String status, boolean isSysScheduler, List<ToolDto> tools, boolean isOnline) {
        this.agentId = agentId;
        this.agentAvatar = agentAvatar;
        this.name = name;
        this.agentBusinessRole  = agentBusinessRole;
        this.description = description;
        this.status = status;
        this.isSysScheduler = isSysScheduler;
        this.tools = tools;
        this.isOnline = isOnline;
    }

    public String getAgentId() {
        return agentId;
    }

    public AgentInfoDto setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSysScheduler() {
        return isSysScheduler;
    }

    public void setSysScheduler(boolean sysScheduler) {
        isSysScheduler = sysScheduler;
    }

    public List<ToolDto> getTools() {
        return tools;
    }

    public void setTools(List<ToolDto> tools) {
        this.tools = tools;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public AgentInfoDto setOnline(boolean online) {
        isOnline = online;
        return this;
    }

    public String getAgentAvatar() {
        return agentAvatar;
    }

    public AgentInfoDto setAgentAvatar(String agentAvatar) {
        this.agentAvatar = agentAvatar;
        return this;
    }

    public String getAgentBusinessRole() {
        return agentBusinessRole;
    }

    public AgentInfoDto setAgentBusinessRole(String agentBusinessRole) {
        this.agentBusinessRole = agentBusinessRole;
        return this;
    }
}
