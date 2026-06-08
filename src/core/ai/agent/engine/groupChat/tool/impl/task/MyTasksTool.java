package ai.agent.engine.groupChat.tool.impl.task;

import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.enums.TaskStatus;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

@ToolDeclare(
    name = "MyTasksTool",
    cnName = "我的任务列表",
    description = "查看当前智能体被分配的所有任务，包括待办、进行中、已完成等状态的任务",
    scope = ToolScope.GROUP_CHAT
)
public class MyTasksTool extends AbsGroupChatTool {

    @Override
    protected String executeInternal() {
        List<TaskBoardItem> allTasks = engine.getTaskBoard().getAllTasks();

        List<TaskBoardItem> myTasks = allTasks.stream()
                .filter(task -> currentAgentId.equals(task.getExecutorId()))
                .collect(Collectors.toList());

        if (myTasks.isEmpty()) {
            return "SUCCESS: 当前没有分配给您的任务";
        }

        StringBuilder result = new StringBuilder();
        result.append("SUCCESS: 您的任务列表如下：\n\n");

        long pendingCount = myTasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
        long inProgressCount = myTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long completedCount = myTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

        result.append("📊 任务统计：\n");
        result.append("- 待办任务：").append(pendingCount).append(" 个\n");
        result.append("- 进行中：").append(inProgressCount).append(" 个\n");
        result.append("- 已完成：").append(completedCount).append(" 个\n");

        result.append("📋 详细任务列表：\n");

        addTasksByStatus(result, myTasks, TaskStatus.IN_PROGRESS, "🔄 进行中的任务");
        addTasksByStatus(result, myTasks, TaskStatus.PENDING, "⏳ 待办任务");
        addTasksByStatus(result, myTasks, TaskStatus.COMPLETED, "✅ 已完成任务");

        return result.toString();
    }


    /**
     * 按状态添加任务到结果中
     */
    private void addTasksByStatus(StringBuilder result, List<TaskBoardItem> tasks,
                                  TaskStatus status, String title) {
        List<TaskBoardItem> statusTasks = tasks.stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());

        if (!statusTasks.isEmpty()) {
            result.append("\n").append(title).append("：\n");
            for (TaskBoardItem task : statusTasks) {
                result.append(StrUtil.format("任务名称:{}\n", task.getTaskName()));
                result.append(StrUtil.format("任务描述:{}\n", task.getTaskDescription()));
            }
        }
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "未知";

        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                instant, java.time.ZoneId.systemDefault());

        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return dateTime.format(formatter);
    }
}
