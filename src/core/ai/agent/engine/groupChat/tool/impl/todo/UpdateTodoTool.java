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
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import gpf.exception.VerifyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 更新待办事项状态工具，支持批量更新
public class UpdateTodoTool implements Tool {

    @ToolParameter(description = "更新数组，格式：[{\"index\":1,\"status\":\"COMPLETED\"}]，status可选值：PENDING/IN_PROGRESS/COMPLETED/IGNORED", required = true)
    public static final String PARAM_ITEMS = "items";

    @Override
    public String getName() {
        return "UpdateTodoTool";
    }

    @Override
    public String getCnName() {
        return "更新待办事项状态";
    }

    @Override
    public String getDescription() {
        return "更新待办事项状态，传递items数组参数（如[{\"index\":1,\"status\":\"COMPLETED\"}]）\n" +
                "status可选值：PENDING(未完成)、IN_PROGRESS(进行中)、COMPLETED(已完成)、IGNORED(已忽略)";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        try {
            GroupChatToolContextAdapter adapter = convertToGroupChatToolContext(ctx);
            if (adapter == null) throw new VerifyException("无效的工具上下文");

            List<TodoItemDto> todoList = adapter.getChatEngine()
                    .getEngineWorkCache(GCEngineWorkCacheKey.TODO_LIST, List.class);

            if (CollUtil.isEmpty(todoList)) throw new VerifyException("待办事项列表为空");

            String itemsStr = String.valueOf(params.get(PARAM_ITEMS));
            if (StrUtil.isBlank(itemsStr) || "null".equals(itemsStr)) throw new VerifyException("items参数不能为空");

            JSONArray jsonArray = JSONUtil.parseArray(itemsStr);
            if (jsonArray.isEmpty()) throw new VerifyException("无法解析更新数据");

            StringBuilder sb = new StringBuilder();
            sb.append("更新结果:\n");

            List<TodoItemDto> updatedItems = new ArrayList<>();
            int successCount = 0;

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Integer index = obj.getInt("index");
                String statusStr = obj.getStr("status");

                if (index == null) continue;

                TodoItemDto item = findByIndex(todoList, index);
                if (item == null) {
                    sb.append(String.format("- [%d] 失败: 未找到该序号\n", index));
                    continue;
                }

                if (StrUtil.isBlank(statusStr)) {
                    sb.append(String.format("- [%d] 失败: 状态不能为空\n", index));
                    continue;
                }

                TodoStatus newStatus;
                try {
                    newStatus = TodoStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    sb.append(String.format("- [%d] 失败: 无效的状态值 %s\n", index, statusStr));
                    continue;
                }

                TodoStatus oldStatus = item.getStatus();
                item.setStatus(newStatus);
                updatedItems.add(item);

                sb.append(String.format("- [%d] %s: %s -> %s\n", index, item.getItemName(), oldStatus, newStatus));
                successCount++;
            }

            adapter.getChatEngine().putEngineWorkCache(GCEngineWorkCacheKey.TODO_LIST, todoList);

            // 通知前端
            if (CollUtil.isNotEmpty(updatedItems)) {
                GroupChatMessageSender.StyleText.todo(adapter.getChatEngine(), adapter.getCurrentAgentId(), updatedItems);
            }

            sb.append(String.format("成功更新 %d 项", successCount));
            return sb.toString();
        } catch (Exception e) {
            return RespondDto.newStrError(e.getMessage());
        }
    }

    // 根据序号查找待办事项
    private TodoItemDto findByIndex(List<TodoItemDto> todoList, int index) {
        for (TodoItemDto item : todoList) {
            if (item.getIndex() != null && item.getIndex() == index) {
                return item;
            }
        }
        return null;
    }
}
