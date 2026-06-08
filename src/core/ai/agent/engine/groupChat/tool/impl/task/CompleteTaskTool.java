package ai.agent.engine.groupChat.tool.impl.task;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.enums.TaskStatus;

import java.util.List;
import java.util.stream.Collectors;

@ToolDeclare(
    name = "CompleteTaskTool",
    cnName = "完成任务",
    description = "将指定的任务标记为已完成状态。参数taskId可选，如果不提供则完成当前智能体的进行中任务",
    scope = ToolScope.GROUP_CHAT
)
public class CompleteTaskTool extends AbsGroupChatTool {
    

    @ParamDeclare(description = "任务ID（可选，如果不提供则完成当前智能体的进行中任务）", required = false)
    private String taskId;

    @Override
    protected String executeInternal() {
        if (taskId != null && !taskId.trim().isEmpty() && !"null".equals(taskId)) {
            return completeSpecificTask();
        } else {
            return completeCurrentAgentTask();
        }
    }

    private String completeSpecificTask() {
        TaskBoardItem updatedTask = engine.updateTaskStatus(taskId, TaskStatus.COMPLETED, currentAgentId);
        
        if (updatedTask != null) {
            return String.format("✓ 任务已完成: %s", updatedTask.getTaskName());
        } else {
            return "错误: 未找到指定的任务ID: " + taskId;
        }
    }

    private String completeCurrentAgentTask() {
        List<TaskBoardItem> inProgressTasks = engine.getTaskBoard()
                .getTasksByStatus(TaskStatus.IN_PROGRESS)
                .stream()
                .filter(task -> currentAgentId.equals(task.getExecutorId()))
                .collect(Collectors.toList());
        
        if (inProgressTasks.isEmpty()) {
            return "提示: 当前没有进行中的任务需要完成";
        }
        
        TaskBoardItem taskToComplete = inProgressTasks.get(0);
        TaskBoardItem updatedTask = engine.updateTaskStatus(
                taskToComplete.getTaskId(), 
                TaskStatus.COMPLETED, 
                currentAgentId
        );
        
        if (updatedTask != null) {
            return String.format("✓ 任务已完成: %s", updatedTask.getTaskName());
        } else {
            return "错误: 更新任务状态失败";
        }
    }
}
