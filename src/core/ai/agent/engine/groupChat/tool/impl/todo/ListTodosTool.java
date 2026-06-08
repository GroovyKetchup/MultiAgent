package ai.agent.engine.groupChat.tool.impl.todo;

import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.textStyle.TodoItemDto;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.enums.GCEngineWorkCacheKey;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import gpf.exception.VerifyException;

import java.util.List;
import java.util.Map;

// 查看待办事项列表工具
public class ListTodosTool implements Tool {

    @Override
    public String getName() {
        return "ListTodosTool";
    }

    @Override
    public String getCnName() {
        return "查看待办事项列表";
    }

    @Override
    public String getDescription() {
        return "查看当前群组的待办事项列表";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        try {
            GroupChatToolContextAdapter adapter = convertToGroupChatToolContext(ctx);
            if (adapter == null) {
                throw new VerifyException("无效的工具上下文");
            }

            List<TodoItemDto> todoList = adapter.getChatEngine()
                    .getEngineWorkCache(GCEngineWorkCacheKey.TODO_LIST, List.class);

            if (CollUtil.isEmpty(todoList)) {
                return "当前没有待办事项";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("待办事项列表:\n");
            for (TodoItemDto item : todoList) {
                sb.append(String.format("[%d] [%s] %s", item.getIndex(), item.getStatus(), item.getItemName()));
                if (StrUtil.isNotBlank(item.getItemDesc())) {
                    sb.append(String.format(" - %s", item.getItemDesc()));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return RespondDto.newStrError(e.getMessage());
        }
    }
}
