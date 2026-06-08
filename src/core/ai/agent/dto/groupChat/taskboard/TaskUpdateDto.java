package ai.agent.dto.groupChat.taskboard;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

/**
 * 任务执行增量更新DTO
 * 用于支持单个任务项的更新，而不需要传递完整列表
 */
@Comment("任务执行增量更新DTO")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-01-09", updateTime = "2025-01-09"
)
public class TaskUpdateDto implements Serializable {

    // 操作类型
    private Operation operation;

    // 单个任务项
    private TaskExecutionItemDto task;

    // 可选：任务索引（用于有序列表中的位置更新）
    private Integer index;

    public TaskUpdateDto() {
    }

    public TaskUpdateDto(Operation operation, TaskExecutionItemDto task) {
        this.operation = operation;
        this.task = task;
    }

    // ========================= getter/setter =========================

    public Operation getOperation() {
        return operation;
    }

    public TaskUpdateDto setOperation(Operation operation) {
        this.operation = operation;
        return this;
    }

    public TaskExecutionItemDto getTask() {
        return task;
    }

    public TaskUpdateDto setTask(TaskExecutionItemDto task) {
        this.task = task;
        return this;
    }

    public Integer getIndex() {
        return index;
    }

    public TaskUpdateDto setIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * 操作类型枚举
     */
    public enum Operation {
        @JSONField(name = "update")
        UPDATE("update", "更新单个任务"),
        @JSONField(name = "add")
        ADD("add", "添加任务"),
        @JSONField(name = "remove")
        REMOVE("remove", "删除任务");

        private final String value;
        private final String description;

        Operation(String value, String description) {
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