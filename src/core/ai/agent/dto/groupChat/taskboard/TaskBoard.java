package ai.agent.dto.groupChat.taskboard;

import ai.agent.enums.TaskStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 任务看板
 * 管理一个群组实例的所有任务
 */
public class TaskBoard implements Serializable {
    private String groupInstanceId; // 关联的群组实例ID
    private Map<String, TaskBoardItem> tasks; // 任务ID -> 任务项映射
    private Instant createTime;
    private Instant lastUpdateTime;

    // 任务依赖关系（taskId -> predecessors）
    private final Map<String, Set<String>> dependencies = new ConcurrentHashMap<>();

    public TaskBoard(String groupInstanceId) {
        this.groupInstanceId = groupInstanceId;
        this.tasks = new ConcurrentSkipListMap<>();
        this.createTime = Instant.now();
        this.lastUpdateTime = Instant.now();
    }

    /**
     * 添加任务
     */
    public TaskBoardItem addTask(String taskName, String taskDescription, String creatorId) {
        TaskBoardItem task = new TaskBoardItem(taskName, taskDescription, creatorId);
        tasks.put(task.getTaskId(), task);
        this.lastUpdateTime = Instant.now();
        return task;
    }

    /**
     * 添加任务（带负责人）
     */
    public TaskBoardItem addTask(String taskName, String taskDescription, String creatorId, String assigneeId) {
        TaskBoardItem task = new TaskBoardItem(taskName, taskDescription, creatorId);
        if (assigneeId != null && !assigneeId.trim().isEmpty()) {
            task.setExecutorId(assigneeId);
        }
        tasks.put(task.getTaskId(), task);
        this.lastUpdateTime = Instant.now();
        return task;
    }

    /**
     * 批量添加任务
     */
    public List<TaskBoardItem> addTasks(List<TaskCreationRequest> requests) {
        List<TaskBoardItem> createdTasks = new ArrayList<>();
        for (TaskCreationRequest request : requests) {
            TaskBoardItem task = addTask(request.getTaskName(), request.getTaskDescription(), request.getCreatorId());
            if (request.getAssigneeId() != null && !request.getAssigneeId().trim().isEmpty()) {
                task.setExecutorId(request.getAssigneeId());
            }
            createdTasks.add(task);
        }
        return createdTasks;
    }

    /**
     * 获取任务
     */
    public TaskBoardItem getTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 设置前序任务（单链）
     */
    public boolean setPredecessor(String taskId, String predecessorTaskId) {
        TaskBoardItem task = tasks.get(taskId);
        if (task == null) return false;
        if (predecessorTaskId != null && !predecessorTaskId.trim().isEmpty()) {
            if (!tasks.containsKey(predecessorTaskId)) return false;
            task.setPredecessorTaskId(predecessorTaskId);
            dependencies.computeIfAbsent(taskId, k -> new java.util.HashSet<>()).add(predecessorTaskId);
        } else {
            task.setPredecessorTaskId(null);
            dependencies.remove(taskId);
        }
        this.lastUpdateTime = Instant.now();
        return true;
    }

    /**
     * 检查任务是否可开始（前序任务需完成）
     */
    public boolean canStart(String taskId) {
        TaskBoardItem task = tasks.get(taskId);
        if (task == null) return false;
        String predecessor = task.getPredecessorTaskId();
        if (predecessor == null) return true;
        TaskBoardItem pre = tasks.get(predecessor);
        return pre != null && pre.getStatus() == TaskStatus.COMPLETED;
    }

    /**
     * 更新任务状态（含前置校验）
     */
    public TaskBoardItem updateTaskStatus(String taskId, TaskStatus status, String executorId) {
        TaskBoardItem task = tasks.get(taskId);
        if (task != null) {
            if (status == TaskStatus.IN_PROGRESS && !canStart(taskId)) {
                task.setStatus(TaskStatus.PENDING);
                return task;
            }
            task.setStatus(status);
            if (executorId != null) {
                task.setExecutorId(executorId);
            }
            this.lastUpdateTime = Instant.now();
        }
        return task;
    }

    /**
     * 获取所有任务
     */
    public List<TaskBoardItem> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * 按状态获取任务
     */
    public List<TaskBoardItem> getTasksByStatus(TaskStatus status) {
        List<TaskBoardItem> result = new ArrayList<TaskBoardItem>();
        for (TaskBoardItem task : tasks.values()) {
            if (task.getStatus() == status) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * 按创建者获取任务
     */
    public List<TaskBoardItem> getTasksByCreator(String creatorId) {
        List<TaskBoardItem> result = new ArrayList<TaskBoardItem>();
        for (TaskBoardItem task : tasks.values()) {
            if (task.getCreatorId().equals(creatorId)) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * 按执行者获取任务
     */
    public List<TaskBoardItem> getTasksByExecutor(String executorId) {
        List<TaskBoardItem> result = new ArrayList<TaskBoardItem>();
        for (TaskBoardItem task : tasks.values()) {
            if (executorId != null && executorId.equals(task.getExecutorId())) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * 获取任务统计信息
     */
    public TaskBoardStats getStats() {
        int pendingCount = 0;
        int inProgressCount = 0;
        int completedCount = 0;

        for (TaskBoardItem task : tasks.values()) {
            switch (task.getStatus()) {
                case PENDING:
                    pendingCount++;
                    break;
                case IN_PROGRESS:
                    inProgressCount++;
                    break;
                case COMPLETED:
                    completedCount++;
                    break;
            }
        }

        return new TaskBoardStats(tasks.size(), pendingCount, inProgressCount, completedCount);
    }

    /**
     * 删除任务
     */
    public boolean removeTask(String taskId) {
        TaskBoardItem removed = tasks.remove(taskId);
        if (removed != null) {
            this.lastUpdateTime = Instant.now();
            return true;
        }
        return false;
    }

    /**
     * 清空所有任务
     */
    public void clearAllTasks() {
        tasks.clear();
        this.lastUpdateTime = Instant.now();
    }

    // Getters
    public String getGroupInstanceId() {
        return groupInstanceId;
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * 任务创建请求
     */
    public static class TaskCreationRequest {
        private final String taskName;
        private final String taskDescription;
        private final String creatorId;
        private final String assigneeId; // 负责人ID

        public TaskCreationRequest(String taskName, String taskDescription, String creatorId) {
            this(taskName, taskDescription, creatorId, null);
        }

        public TaskCreationRequest(String taskName, String taskDescription, String creatorId, String assigneeId) {
            this.taskName = taskName;
            this.taskDescription = taskDescription;
            this.creatorId = creatorId;
            this.assigneeId = assigneeId;
        }

        public String getTaskName() {
            return taskName;
        }

        public String getTaskDescription() {
            return taskDescription;
        }

        public String getCreatorId() {
            return creatorId;
        }

        public String getAssigneeId() {
            return assigneeId;
        }
    }

    /**
     * 任务状态更新请求
     */
    public static class TaskStatusUpdateRequest {
        private final String taskId;
        private final TaskStatus status;
        private final String executorId;

        public TaskStatusUpdateRequest(String taskId, TaskStatus status, String executorId) {
            this.taskId = taskId;
            this.status = status;
            this.executorId = executorId;
        }

        public String getTaskId() {
            return taskId;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public String getExecutorId() {
            return executorId;
        }
    }

    /**
     * 任务看板统计信息
     */
    public static class TaskBoardStats {
        private final int totalTasks;
        private final int pendingTasks;
        private final int inProgressTasks;
        private final int completedTasks;

        public TaskBoardStats(int totalTasks, int pendingTasks, int inProgressTasks, int completedTasks) {
            this.totalTasks = totalTasks;
            this.pendingTasks = pendingTasks;
            this.inProgressTasks = inProgressTasks;
            this.completedTasks = completedTasks;
        }

        public int getTotalTasks() {
            return totalTasks;
        }

        public int getPendingTasks() {
            return pendingTasks;
        }

        public int getInProgressTasks() {
            return inProgressTasks;
        }

        public int getCompletedTasks() {
            return completedTasks;
        }

        @Override
        public String toString() {
            return String.format("TaskBoardStats{total=%d, pending=%d, inProgress=%d, completed=%d}",
                    totalTasks, pendingTasks, inProgressTasks, completedTasks);
        }
    }


    public TaskBoard setGroupInstanceId(String groupInstanceId) {
        this.groupInstanceId = groupInstanceId;
        return this;
    }

    public Map<String, TaskBoardItem> getTasks() {
        return tasks;
    }

    public TaskBoard setTasks(Map<String, TaskBoardItem> tasks) {
        this.tasks = tasks;
        return this;
    }

    public TaskBoard setCreateTime(Instant createTime) {
        this.createTime = createTime;
        return this;
    }

    public TaskBoard setLastUpdateTime(Instant lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
        return this;
    }

    public Map<String, Set<String>> getDependencies() {
        return dependencies;
    }
}
