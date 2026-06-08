package ai.agent.dto.groupChat.taskboard;

import ai.agent.enums.TaskStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 任务看板项
 * 表示任务看板中的单个任务
 */
public class TaskBoardItem implements Serializable {
    private String taskId;
    private String taskName;
    private String taskDescription;
    private TaskStatus status;
    private String creatorId; // 创建者（智能体ID或用户ID）
    private String executorId; // 执行者（可选，智能体ID）
    private Instant createTime;
    private Instant updateTime;

    // 前序任务（必须完成后才能进行本任务）
    private String predecessorTaskId;

    // 任务日志
    private final List<TaskLogItem> logs = new ArrayList<>();

    public TaskBoardItem() {
    }

    // 构造函数 - 创建新任务
    public TaskBoardItem(String taskName, String taskDescription, String creatorId) {
        this.taskId = UUID.randomUUID().toString();
        this.taskName = taskName;
        this.taskDescription = taskDescription;
        this.status = TaskStatus.PENDING;
        this.creatorId = creatorId;
        this.executorId = null;
        this.createTime = Instant.now();
        this.updateTime = Instant.now();
        addLog("CREATE", creatorId, "任务创建: " + taskName);
    }

    // Getters
    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public String getExecutorId() {
        return executorId;
    }

    public String getPredecessorTaskId() {
        return predecessorTaskId;
    }

    public void setPredecessorTaskId(String predecessorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
        addLog("SET_PREDECESSOR", creatorId, "设置前序任务: " + predecessorTaskId);
    }

    public List<TaskLogItem> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public void setStatus(TaskStatus status) {
        TaskStatus old = this.status;
        this.status = status;
        this.updateTime = Instant.now();
        addLog("STATUS_CHANGE", executorId != null ? executorId : creatorId, "状态: " + old + " -> " + status);
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
        addLog("SET_EXECUTOR", creatorId, "负责人: " + executorId);
    }

    private void addLog(String action, String operatorId, String details) {
        logs.add(new TaskLogItem(action, operatorId, details, Instant.now().toEpochMilli()));
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    // Setters
    public void setTaskName(String taskName) {
        this.taskName = taskName;
        this.updateTime = Instant.now();
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
        this.updateTime = Instant.now();
    }

    public TaskBoardItem setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public TaskBoardItem setTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public TaskBoardItem setCreatorId(String creatorId) {
        this.creatorId = creatorId;
        return this;
    }

    public TaskBoardItem setCreateTime(Instant createTime) {
        this.createTime = createTime;
        return this;
    }

    /**
     * 验证任务项的有效性
     */
    public boolean isValid() {
        return taskId != null && !taskId.trim().isEmpty() &&
                taskName != null && !taskName.trim().isEmpty() &&
                status != null &&
                creatorId != null && !creatorId.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "TaskBoardItem{" +
                "taskId='" + taskId + '\'' +
                ", taskName='" + taskName + '\'' +
                ", taskDescription='" + taskDescription + '\'' +
                ", status=" + status +
                ", creatorId='" + creatorId + '\'' +
                ", executorId='" + executorId + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
