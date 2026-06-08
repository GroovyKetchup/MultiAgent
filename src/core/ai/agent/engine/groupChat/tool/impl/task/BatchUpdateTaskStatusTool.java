package ai.agent.engine.groupChat.tool.impl.task;

import ai.agent.annotation.ToolParameter;
import ai.agent.dto.groupChat.taskboard.TaskBoard;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.enums.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BatchUpdateTaskStatusTool
 * 批量更新任务状态工具
 * 支持一次性更新多个任务的状态和执行者
 */
public class BatchUpdateTaskStatusTool implements Tool {
    
    @ToolParameter(
        description = "JSON数组，包含任务更新信息。格式：[{\"taskId\":\"task-id\",\"status\":\"IN_PROGRESS\",\"executor\":\"agent-id\"}]",
        type = "string",
        required = true
    )
    public static final String PARAM_UPDATES = "updates";
    
    @Override
    public String getName() { return "BatchUpdateTaskStatusTool"; }

    @Override
    public String getCnName() {
        return "批量更新任务状态";
    }

    @Override
    public String getDescription() {
        return "批量更新多个任务的状态和执行者。\n" +
               "参数: updates(JSON数组，包含任务更新信息)\n" +
               "格式: <ToolCalling toolName='BatchUpdateTaskStatusTool'><updates>[{\"taskId\":\"task-id-1\",\"status\":\"IN_PROGRESS\"},{\"taskId\":\"task-id-2\",\"status\":\"COMPLETED\",\"executor\":\"agent-id\"}]</updates></ToolCalling>\n" +
               "支持的状态: PENDING, IN_PROGRESS, COMPLETED";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        // 获取群组实例ID
        String groupInstanceId = getGroupInstanceId(ctx);
        if (groupInstanceId == null) {
            return "Error: Cannot get group instance ID";
        }

        Object updatesParam = params.get("updates");
        if (updatesParam == null) {
            return "Error: Updates parameter is required";
        }

        String updatesStr = String.valueOf(updatesParam);
        if ("null".equals(updatesStr) || updatesStr.trim().isEmpty()) {
            return "Error: Updates cannot be empty";
        }

        // 解析更新请求
        List<TaskBoard.TaskStatusUpdateRequest> updateRequests = parseUpdateRequests(updatesStr);
        if (updateRequests.isEmpty()) {
            return "Error: No valid update requests found";
        }

        // 执行批量更新
        if (ctx instanceof ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter) {
            ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter adapter =
                (ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter) ctx;

            List<TaskBoardItem> updatedTasks = adapter.getChatEngine().updateTaskStatuses(updateRequests);

            if (updatedTasks.isEmpty()) {
                return "Error: No tasks were updated";
            }

            // 构建结果消息
            StringBuilder result = new StringBuilder("Batch task status update successful:\n");
            for (TaskBoardItem task : updatedTasks) {
                result.append(String.format("- [%s] %s -> %s", 
                    task.getTaskId(), task.getTaskName(), task.getStatus()));
                if (task.getExecutorId() != null) {
                    result.append(String.format(" (Executor: %s)", task.getExecutorId()));
                }
                result.append("\n");
            }

            return result.toString();
        } else {
            return "Error: Invalid tool context";
        }
    }

    /**
     * 解析批量更新请求
     */
    private List<TaskBoard.TaskStatusUpdateRequest> parseUpdateRequests(String updatesStr) {
        List<TaskBoard.TaskStatusUpdateRequest> requests = new ArrayList<>();

        try {
            // 移除外层的方括号
            String content = updatesStr.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
            }

            // 分割各个更新对象
            String[] updateObjects = splitJsonObjects(content);

            for (String updateObj : updateObjects) {
                TaskBoard.TaskStatusUpdateRequest request = parseUpdateObject(updateObj.trim());
                if (request != null) {
                    requests.add(request);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to parse update requests: " + e.getMessage());
        }

        return requests;
    }

    /**
     * 分割JSON对象数组
     */
    private String[] splitJsonObjects(String content) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    // 找到一个完整的对象
                    objects.add(content.substring(start, i + 1));
                    // 跳过逗号和空格
                    while (i + 1 < content.length() && 
                           (content.charAt(i + 1) == ',' || Character.isWhitespace(content.charAt(i + 1)))) {
                        i++;
                    }
                    start = i + 1;
                }
            }
        }

        return objects.toArray(new String[0]);
    }

    /**
     * 解析单个更新对象
     */
    private TaskBoard.TaskStatusUpdateRequest parseUpdateObject(String jsonObj) {
        try {
            // 移除大括号
            String content = jsonObj.trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                content = content.substring(1, content.length() - 1);
            }

            String taskId = null;
            String statusStr = null;
            String executorId = null;

            // 简单的键值对解析
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim().replaceAll("\"", "");

                    switch (key) {
                        case "taskId":
                            taskId = value;
                            break;
                        case "status":
                            statusStr = value;
                            break;
                        case "executor":
                            executorId = value;
                            break;
                    }
                }
            }

            if (taskId != null && !taskId.isEmpty() && statusStr != null && !statusStr.isEmpty()) {
                TaskStatus status = parseTaskStatus(statusStr);
                if (status != null) {
                    return new TaskBoard.TaskStatusUpdateRequest(taskId, status, executorId);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to parse update object: " + jsonObj + " - " + e.getMessage());
        }

        return null;
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
                    System.err.println("Invalid task status: " + statusStr);
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to parse task status: " + statusStr + " - " + e.getMessage());
            return null;
        }
    }

}
