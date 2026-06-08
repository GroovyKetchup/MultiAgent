package ai.agent.engine.groupChat.tool.impl.todo;

import ai.agent.annotation.ToolParameter;
import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.textStyle.TodoItemDto;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.enums.GCEngineWorkCacheKey;
import ai.agent.enums.TodoStatus;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import gpf.exception.VerifyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 添加待办事项工具，支持批量添加
public class AddTodoTool implements Tool {

    @ToolParameter(description = "待办事项数组，格式：[{\"itemName\":\"事项1\",\"itemDesc\":\"描述1\"}]", required = true)
    public static final String PARAM_ITEMS = "items";

    @ToolParameter(description = "是否清空已存在的待办事项，默认false", required = false)
    public static final String PARAM_CLEAR_EXISTING = "clearExisting";

    @Override
    public String getName() {
        return "AddTodoTool";
    }

    @Override
    public String getCnName() {
        return "添加待办事项";
    }

    @Override
    public String getDescription() {
        return "添加待办事项，传递items数组参数（如[{\"itemName\":\"事项1\",\"itemDesc\":\"描述1\"}]），可选clearExisting参数清空已有待办";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        try {
            GroupChatToolContextAdapter adapter = convertToGroupChatToolContext(ctx);
            if (adapter == null) throw new VerifyException("无效的工具上下文");

            String itemsStr = String.valueOf(params.get(PARAM_ITEMS));
            if (StrUtil.isBlank(itemsStr) || "null".equals(itemsStr)) throw new VerifyException("items参数不能为空");

            // 是否清空已有待办
            String clearExistingStr = String.valueOf(params.get(PARAM_CLEAR_EXISTING));
            boolean clearExisting = "true".equalsIgnoreCase(clearExistingStr);

            // 获取现有的待办列表
            List<TodoItemDto> todoList;
            if (clearExisting) {
                todoList = new ArrayList<>();
            } else {
                todoList = adapter.getChatEngine().getEngineWorkCache(GCEngineWorkCacheKey.TODO_LIST, List.class);
                if (todoList == null) todoList = new ArrayList<>();
            }

            List<TodoItemDto> newItems = parseItemsFromJson(itemsStr, todoList);
            if (CollUtil.isEmpty(newItems)) throw new VerifyException("无法解析待办事项数据");

            todoList.addAll(newItems);
            adapter.getChatEngine().putEngineWorkCache(GCEngineWorkCacheKey.TODO_LIST, todoList);

            // 通知前端
            GroupChatMessageSender.StyleText.todo(adapter.getChatEngine(), adapter.getCurrentAgentId(), newItems);

            StringBuilder sb = new StringBuilder();
            sb.append("添加成功:\n");
            for (TodoItemDto item : newItems) {
                sb.append(String.format("- [%d] %s\n", item.getIndex(), item.getItemName()));
            }

            return sb.toString();
        } catch (Exception e) {
            return RespondDto.newStrError(e.getMessage());
        }
    }

    // 解析JSON格式的待办事项数组
    private List<TodoItemDto> parseItemsFromJson(String jsonStr, List<TodoItemDto> existingList) {
        List<TodoItemDto> items = new ArrayList<>();
        int nextIndex = CollUtil.isEmpty(existingList) ? 1 : existingList.stream().mapToInt(TodoItemDto::getIndex).max().orElse(0) + 1;

        JSONArray jsonArray = JSONUtil.parseArray(jsonStr);
        for (int i = 0; i < jsonArray.size(); i++) {
            TodoItemDto dto = jsonArray.get(i, TodoItemDto.class);
            if (dto != null && StrUtil.isNotBlank(dto.getItemName())) {
                dto.setIndex(nextIndex++);
                if (dto.getItemDesc() == null) dto.setItemDesc("");
                if (dto.getStatus() == null) dto.setStatus(TodoStatus.PENDING);
                items.add(dto);
            }
        }

        return items;
    }
}
