package ai.agent.dto.groupChat;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("任务评价dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-11", updateTime = "2025-12-11"
)
public class TaskEvaluationDto implements Serializable {

    // 关联的智能体ID列表
    List<String> relatedAgentIds;
    // 消息相关的上下文
    String messageContext;
    // 任务评价
    String category;
    // 任务评价说明
    String taskComment;

    public TaskEvaluationDto(List<String> relatedAgentIds, String messageContext, String category, String taskComment) {
        this.relatedAgentIds = relatedAgentIds;
        this.messageContext = messageContext;
        this.category = category;
        this.taskComment = taskComment;
    }

    public List<String> getRelatedAgentIds() {
        return relatedAgentIds;
    }

    public TaskEvaluationDto setRelatedAgentIds(List<String> relatedAgentIds) {
        this.relatedAgentIds = relatedAgentIds;
        return this;
    }

    public String getMessageContext() {
        return messageContext;
    }

    public TaskEvaluationDto setMessageContext(String messageContext) {
        this.messageContext = messageContext;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public TaskEvaluationDto setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getTaskComment() {
        return taskComment;
    }

    public TaskEvaluationDto setTaskComment(String taskComment) {
        this.taskComment = taskComment;
        return this;
    }
}
