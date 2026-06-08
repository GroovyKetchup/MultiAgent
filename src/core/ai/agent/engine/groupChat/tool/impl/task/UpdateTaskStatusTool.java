package ai.agent.engine.groupChat.tool.impl.task;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.enums.TaskStatus;

import java.util.List;

@ToolDeclare(
    name = "UpdateTaskStatusTool",
    cnName = "更新任务状态",
    description = "更新任务状态和执行者。参数：id(任务ID)或taskName(任务名称)、status(状态)、executor(可选，执行者ID)",
    scope = ToolScope.GROUP_CHAT
)
public class UpdateTaskStatusTool extends AbsGroupChatTool {
    
    @ParamDeclare(description = "任务ID", required = false)
    private String id;
    
    @ParamDeclare(description = "任务名称（如果没有提供ID）", required = false)
    private String taskName;
    
    @ParamDeclare(
        description = "任务状态",
        enumValues = {"PENDING", "IN_PROGRESS", "COMPLETED"},
        required = true
    )
    private String status;
    
    @ParamDeclare(description = "执行者ID（可选）", required = false)
    private String executor;

    @Override
    protected String executeInternal() {
        String taskId = id;
        
        if (taskId == null || "null".equals(taskId) || taskId.trim().isEmpty()) {
            if (taskName != null && !"null".equals(taskName) && !taskName.trim().isEmpty()) {
                taskId = findTaskIdByName();
                if (taskId == null) {
                    return "Error: Task not found with name: " + taskName;
                }
            } else {
                return "Error: Task ID or task name must be provided";
            }
        }

        TaskStatus taskStatus = parseTaskStatus(status);
        if (taskStatus == null) {
            return "Error: Invalid task status '" + status + "', supported statuses: PENDING, IN_PROGRESS, COMPLETED";
        }

        String executorId = (executor != null && !"null".equals(executor)) ? executor : null;

        TaskBoardItem updatedTask = engine.updateTaskStatus(taskId, taskStatus, executorId);

        if (updatedTask == null) {
            return "Task not found: " + taskId;
        }

        String result = String.format("Task status updated successfully: [%s] %s -> %s",
                updatedTask.getTaskId(), updatedTask.getTaskName(), updatedTask.getStatus());

        if (updatedTask.getExecutorId() != null) {
            result += String.format(" (Executor: %s)", updatedTask.getExecutorId());
        }

        return result;
    }

    private String findTaskIdByName() {
        List<TaskBoardItem> allTasks = engine.getTaskBoard().getAllTasks();
        for (TaskBoardItem task : allTasks) {
            if (task.getTaskName().contains(taskName) || taskName.contains(task.getTaskName())) {
                return task.getTaskId();
            }
        }
        return null;
    }

    private TaskStatus parseTaskStatus(String statusStr) {
        if (statusStr == null) return null;
        switch (statusStr.toUpperCase()) {
            case "PENDING":
            case "TODO":
                return TaskStatus.PENDING;
            case "IN_PROGRESS":
            case "DOING":
            case "PROGRESS":
                return TaskStatus.IN_PROGRESS;
            case "COMPLETED":
            case "DONE":
            case "COMPLETE":
                return TaskStatus.COMPLETED;
            default:
                return null;
        }
    }
}

