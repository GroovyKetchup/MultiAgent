package ai.agent.engine.groupChat.tool.impl.task;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.groupChat.taskboard.TaskBoard;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;

import java.util.ArrayList;
import java.util.List;

@ToolDeclare(
    name = "CreateTaskTool",
    cnName = "创建任务",
    description = "在任务看板上创建任务，天然支持批量创建。单个任务: 提供title、description、assignee参数。批量任务: 提供tasks参数（JSON数组）。assignee支持agentId或中文名称",
    scope = ToolScope.GROUP_CHAT
)
public class CreateTaskTool extends AbsGroupChatTool {
    
    @ParamDeclare(description = "任务标题", required = false)
    private String title;
    
    @ParamDeclare(description = "任务描述", required = false)
    private String description;
    
    @ParamDeclare(description = "负责人的agentId或中文名称（如'rough_delivery'或'原型智能体'）", required = false)
    private String assignee;
    
    @ParamDeclare(description = "JSON格式的任务数组，用于批量创建任务", required = false)
    private String tasks;

    @Override
    protected String executeInternal() {
        if (tasks != null && !"null".equals(tasks)) {
            return createMultipleTasks();
        } else {
            return createSingleTask();
        }
    }

    private String createSingleTask() {
        if (title == null || "null".equals(title) || title.trim().isEmpty()) {
            return "Error: Task title is required and cannot be empty";
        }

        if (description == null || "null".equals(description) || description.trim().isEmpty()) {
            return "Error: Task description is required and cannot be empty";
        }

        if (assignee == null || "null".equals(assignee) || assignee.trim().isEmpty()) {
            return "Error: Task assignee is required and cannot be empty";
        }

        GroupDefinition groupDef = engine.getGroupDefinition();
        String resolvedAssignee = resolveAgentId(assignee, groupDef);
        if (resolvedAssignee == null) {
            return "Error: Assignee '" + assignee + "' is not a valid team member";
        }

        TaskBoardItem task = engine.createTask(title, description, currentAgentId, resolvedAssignee);

        String result = String.format("Task created successfully: [%s] %s - Status: %s",
                task.getTaskId(), task.getTaskName(), task.getStatus());

        if (task.getExecutorId() != null) {
            String assigneeName = getAgentDisplayName(task.getExecutorId(), groupDef);
            result += String.format(" - Assignee: %s (%s)", assigneeName, task.getExecutorId());
        }

        return result;
    }

    private String createMultipleTasks() {
        try {
            List<TaskBoard.TaskCreationRequest> requests = parseTasksFromJson(tasks, currentAgentId);

            if (requests.isEmpty()) {
                return "Error: Invalid batch task format";
            }

            List<TaskBoardItem> createdTasks = engine.createTasks(requests);

            StringBuilder result = new StringBuilder("Batch task creation successful:\n");
            GroupDefinition groupDef = engine.getGroupDefinition();
            for (TaskBoardItem task : createdTasks) {
                String assigneeInfo = "";
                if (task.getExecutorId() != null) {
                    String assigneeName = getAgentDisplayName(task.getExecutorId(), groupDef);
                    assigneeInfo = String.format(" - Assignee: %s (%s)", assigneeName, task.getExecutorId());
                }
                result.append(String.format("- [%s] %s%s\n", task.getTaskId(), task.getTaskName(), assigneeInfo));
            }

            return result.toString();

        } catch (Exception e) {
            return "Error: Batch task creation failed - " + e.getMessage();
        }
    }

    private List<TaskBoard.TaskCreationRequest> parseTasksFromJson(String tasksStr, String creatorId) {
        List<TaskBoard.TaskCreationRequest> requests = new ArrayList<>();

        try {
            if (tasksStr.contains("title") && tasksStr.contains("description")) {
                requests = parseJsonTaskArray(tasksStr, creatorId);
            }

        } catch (Exception e) {
            System.err.println("Failed to parse tasks JSON: " + e.getMessage());
        }

        return requests;
    }

    private List<TaskBoard.TaskCreationRequest> parseJsonTaskArray(String jsonStr, String creatorId) {
        List<TaskBoard.TaskCreationRequest> requests = new ArrayList<>();

        try {
            String content = jsonStr.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
            }

            String[] taskObjects = splitJsonObjects(content);

            for (String taskObj : taskObjects) {
                TaskBoard.TaskCreationRequest request = parseJsonTaskObject(taskObj.trim(), creatorId);
                if (request != null) {
                    requests.add(request);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to parse JSON task array: " + e.getMessage());
        }

        return requests;
    }

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

    private TaskBoard.TaskCreationRequest parseJsonTaskObject(String jsonObj, String creatorId) {
        try {
            String content = jsonObj.trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                content = content.substring(1, content.length() - 1);
            }

            String title = null;
            String description = null;
            String assignee = null;

            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim().replaceAll("\"", "");

                    if ("title".equals(key)) {
                        title = value;
                    } else if ("description".equals(key)) {
                        description = value;
                    } else if ("assignee".equals(key)) {
                        assignee = value;
                    }
                }
            }

            if (title == null || title.trim().isEmpty()) {
                System.err.println("Task title is required: " + jsonObj);
                return null;
            }

            if (description == null || description.trim().isEmpty()) {
                System.err.println("Task description is required: " + jsonObj);
                return null;
            }

            if (assignee == null || assignee.trim().isEmpty()) {
                System.err.println("Task assignee is required: " + jsonObj);
                return null;
            }

            GroupDefinition groupDef = engine.getGroupDefinition();
            String resolvedAssignee = resolveAgentId(assignee, groupDef);
            if (resolvedAssignee == null) {
                System.err.println("Invalid assignee '" + assignee + "' in task: " + jsonObj);
                return null;
            }

            return new TaskBoard.TaskCreationRequest(title, description, creatorId, resolvedAssignee);

        } catch (Exception e) {
            System.err.println("Failed to parse JSON task object: " + jsonObj + " - " + e.getMessage());
        }

        return null;
    }


    /**
     * 解析智能体ID，支持agentId或中文名称
     * @param input agentId或中文名称
     * @param groupDef 群组定义
     * @return 解析后的agentId，如果找不到则返回null
     */
    private String resolveAgentId(String input, GroupDefinition groupDef) {
        if (groupDef == null || input == null || input.trim().isEmpty()) {
            return null;
        }
        
        input = input.trim();
        
        // 1. 先尝试直接作为agentId查找
        if (groupDef.getAgentDefinition(input) != null) {
            return input;
        }
        
        // 2. 尝试作为中文名称查找
        for (ai.agent.engine.groupChat.model.definition.AgentDefinition agentDef : groupDef.getAgentDefinitions()) {
            if (input.equals(agentDef.getAgentName())) {
                return agentDef.getAgentId();
            }
        }
        
        return null;
    }

    /**
     * 获取智能体显示名称
     */
    private String getAgentDisplayName(String agentId, GroupDefinition groupDef) {
        if (groupDef == null || agentId == null) {
            return agentId;
        }

        ai.agent.engine.groupChat.model.definition.AgentDefinition agentDef = groupDef.getAgentDefinition(agentId);
        if (agentDef != null) {
            return agentDef.getAgentName();
        }

        return agentId;
    }
}

