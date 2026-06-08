package ai.agent.engine.groupChat.tool.impl.task;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.groupChat.taskboard.TaskBoard;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.enums.TaskStatus;

import java.util.List;

@ToolDeclare(
    name = "ListTasksTool",
    cnName = "查看任务列表",
    description = "列出当前群组任务看板的任务列表，支持按状态分组显示",
    scope = ToolScope.GROUP_CHAT
)
public class ListTasksTool extends AbsGroupChatTool {

    @ParamDeclare(description = "分组方式，值为'status'时按状态分组", required = false)
    private String groupBy;

    @ParamDeclare(
            description = "筛选特定状态的任务",
            enumValues = {"PENDING", "IN_PROGRESS", "COMPLETED"},
            required = false
    )
    private String status;

    @Override
    protected String executeInternal() {
        List<TaskBoardItem> tasks;
        if (status != null && !"null".equals(status)) {
            TaskStatus taskStatus = parseTaskStatus(status);
            if (taskStatus == null) {
                return "Error: Invalid task status '" + status + "'";
            }
            tasks = engine.getTaskBoard().getTasksByStatus(taskStatus);
        } else {
            tasks = engine.getTaskBoard().getAllTasks();
        }

        if (tasks == null || tasks.isEmpty()) {
            return "Current group has no tasks";
        }

        if ("status".equals(groupBy)) {
            return formatTasksByStatus(tasks);
        } else {
            return formatTasksList(tasks);
        }
    }

    /**
     * 格式化任务列表（普通格式）
     */
    private String formatTasksList(List<TaskBoardItem> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 任务列表:\n");

        for (TaskBoardItem task : tasks) {
            String statusIcon = getStatusIcon(task.getStatus());
            sb.append(String.format("%s [%s] %s", statusIcon, task.getTaskId(), task.getTaskName()));

            if (task.getExecutorId() != null) {
                sb.append(String.format(" (执行者: %s)", task.getExecutorId()));
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatTasksByStatus(List<TaskBoardItem> tasks) {
        StringBuilder sb = new StringBuilder();

        TaskBoard.TaskBoardStats stats = engine.getTaskBoard().getStats();
        sb.append("Task Board Statistics:\n");
        sb.append(String.format("Total: %d | Pending: %d | In Progress: %d | Completed: %d\n\n",
                stats.getTotalTasks(), stats.getPendingTasks(),
                stats.getInProgressTasks(), stats.getCompletedTasks()));

        for (TaskStatus taskStatus : TaskStatus.values()) {
            List<TaskBoardItem> statusTasks = engine.getTaskBoard().getTasksByStatus(taskStatus);
            if (!statusTasks.isEmpty()) {
                String statusIcon = getStatusIcon(taskStatus);
                sb.append(String.format("%s %s (%d tasks):\n", statusIcon, getStatusName(taskStatus), statusTasks.size()));

                for (TaskBoardItem task : statusTasks) {
                    sb.append(String.format("  • [%s] %s", task.getTaskId(), task.getTaskName()));
                    if (task.getExecutorId() != null) {
                        sb.append(String.format(" (Executor: %s)", task.getExecutorId()));
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 解析任务状态
     */
    private TaskStatus parseTaskStatus(String statusStr) {
        try {
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
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取状态图标
     */
    private String getStatusIcon(TaskStatus status) {
        switch (status) {
            case PENDING:
                return "⏳";
            case IN_PROGRESS:
                return "🔄";
            case COMPLETED:
                return "✅";
            default:
                return "❓";
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(TaskStatus status) {
        switch (status) {
            case PENDING:
                return "待开始";
            case IN_PROGRESS:
                return "进行中";
            case COMPLETED:
                return "已完成";
            default:
                return "未知";
        }
    }


}

