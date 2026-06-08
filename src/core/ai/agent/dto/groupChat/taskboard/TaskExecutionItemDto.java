package ai.agent.dto.groupChat.taskboard;

import ai.agent.dto.groupChat.textStyle.ErrorItemDto;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("任务执行项Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-01-09", updateTime = "2025-01-09"
)
public class TaskExecutionItemDto implements Serializable {

    // 任务ID
    private String id;
    // 任务名称
    private String name;
    // 任务描述
    private String description;
    // 任务元信息
    private TaskExecutionMetaInfo metaInfo;

    // 状态
    private Status status;
    // 错误列表（仅当状态为ERROR时有值）
    private List<ErrorItemDto> errors;

    // ========================= getter/setter =========================

    public String getId() {
        return id;
    }

    public TaskExecutionItemDto setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public TaskExecutionItemDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TaskExecutionItemDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public TaskExecutionItemDto setStatus(Status status) {
        this.status = status;
        return this;
    }

    public List<ErrorItemDto> getErrors() {
        return errors;
    }

    public TaskExecutionItemDto setErrors(List<ErrorItemDto> errors) {
        this.errors = errors;
        return this;
    }

    public TaskExecutionMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public TaskExecutionItemDto setMetaInfo(TaskExecutionMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
        return this;
    }

    /**
     * 任务状态枚举
     */
    public enum Status {
        PENDING("PENDING", "待执行"),
        IN_PROGRESS("IN_PROGRESS", "执行中"),
        COMPLETED("COMPLETED", "已完成"),
        ERROR("ERROR", "错误");

        private final String value;
        private final String description;

        Status(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }
}
