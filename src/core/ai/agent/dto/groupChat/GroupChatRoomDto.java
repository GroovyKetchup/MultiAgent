package ai.agent.dto.groupChat;

import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("群聊信息DTO - 返回给前端的群聊信息")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-02", updateTime = "2025-09-02"
)
public class GroupChatRoomDto implements Serializable {

    // 是新建的
    private Boolean isNewCreate;

    // 应用编号
    private String appCode;

    // 群组名称
    private String groupName;

    // 群组实例ID
    private String groupChatInstId;

    // 任务看板-任务
    private List<TaskBoardItem> taskList;

    // 智能体列表
    private List<AgentInfoDto> agentList;

    // 最后一条消息的时间
    private Long lastMessageSendTimestamp;



    public GroupChatRoomDto() {
    }

    public GroupChatRoomDto(Boolean isNewCreate, String groupChatInstId, String groupName, List<AgentInfoDto> agentList, List<TaskBoardItem> taskList) {
        this.isNewCreate = isNewCreate;
        this.groupChatInstId = groupChatInstId;
        this.groupName = groupName;
        this.agentList = agentList;
        this.taskList = taskList;
    }

    public String getAppCode() {
        return appCode;
    }

    public GroupChatRoomDto setAppCode(String appCode) {
        this.appCode = appCode;
        return this;
    }

    public List<TaskBoardItem> getTaskList() {
        return taskList;
    }

    public GroupChatRoomDto setTaskList(List<TaskBoardItem> taskList) {
        this.taskList = taskList;
        return this;
    }

    public Boolean getNewCreate() {
        return isNewCreate;
    }

    public GroupChatRoomDto setNewCreate(Boolean newCreate) {
        isNewCreate = newCreate;
        return this;
    }

    public String getGroupChatInstId() {
        return groupChatInstId;
    }

    public void setGroupChatInstId(String groupChatInstId) {
        this.groupChatInstId = groupChatInstId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<AgentInfoDto> getAgentList() {
        return agentList;
    }

    public void setAgentList(List<AgentInfoDto> agentList) {
        this.agentList = agentList;
    }

    public Long getLastMessageSendTimestamp() {
        return lastMessageSendTimestamp;
    }

    public GroupChatRoomDto setLastMessageSendTimestamp(Long lastMessageSendTimestamp) {
        this.lastMessageSendTimestamp = lastMessageSendTimestamp;
        return this;
    }
}
