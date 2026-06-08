package ai.agent.dto.groupChat.session;

import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.engine.groupChat.session.SubSessionMode;
import ai.agent.engine.groupChat.session.SubSessionStatus;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("子会话DTO")
@ClassDeclare(
        label = "子会话数据传输对象",
        what = "用于前端展示的子会话信息",
        why = "前端需要展示子会话列表和详情",
        how = "从SubSessionInstance转换而来",
        developer = "系统", version = "1.1",
        createTime = "2025-12-16", updateTime = "2025-12-17"
)
public class SubSessionDto implements Serializable {

    private String sessionId;
    private String parentSessionId;
    private String sessionName;
    private String agentId;
    private String agentName;
    private SubSessionMode sessionMode;
    private SubSessionStatus sessionStatus;
    private int messageCount;
    private Long createdAt;
    private Long startedAt;
    private Long completedAt;
    private String taskResult;
    private boolean hasCanvas;

    public static SubSessionDto fromInstance(SubSessionInstance instance) {
        SubSessionDto dto = new SubSessionDto();
        dto.sessionId = instance.getInstanceId();
        dto.parentSessionId = instance.getParentSessionId();

        AgentDefinition agentDef = instance.getAgentDefinition();
        if (agentDef != null) {
            dto.agentId = agentDef.getAgentId();
            dto.agentName = agentDef.getAgentName();
            dto.sessionName = instance.getSessionName();
        }

        dto.sessionMode = instance.getSessionMode();
        dto.sessionStatus = instance.getSubSessionStatus();

        if (instance.getMessageHistoryManager() != null) {
            dto.messageCount = instance.getMessageHistoryManager().getFullHistory().size();
        } else {
            dto.messageCount = 0;
        }

        dto.createdAt = instance.getCreatedAt();
        dto.startedAt = instance.getStartedAt();
        dto.completedAt = instance.getCompletedAt();
        dto.taskResult = instance.getTaskResult();
        dto.hasCanvas = instance.hasCanvas();

        return dto;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getParentSessionId() {
        return parentSessionId;
    }

    public void setParentSessionId(String parentSessionId) {
        this.parentSessionId = parentSessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public SubSessionMode getSessionMode() {
        return sessionMode;
    }

    public void setSessionMode(SubSessionMode sessionMode) {
        this.sessionMode = sessionMode;
    }

    public SubSessionStatus getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(SubSessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public String getTaskResult() {
        return taskResult;
    }

    public void setTaskResult(String taskResult) {
        this.taskResult = taskResult;
    }

    public boolean isHasCanvas() {
        return hasCanvas;
    }

    public void setHasCanvas(boolean hasCanvas) {
        this.hasCanvas = hasCanvas;
    }
}
