package ai.agent.engine.groupChat.tool;

import ai.agent.dto.llmCalling.ToolDto;
import cn.hutool.core.collection.CollUtil;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class ToolRegistry implements Serializable {
    private final Map<String, Tool> tools = new HashMap<String, Tool>();
    private ToolContext context;

    public ToolRegistry() {
    }

    public ToolRegistry(ToolContext context) {
        this.context = context;
    }

    public void setContext(ToolContext context) {
        this.context = context;
    }

    public ToolContext getContext() {
        return context;
    }

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool get(String name) {
        return tools.get(name);
    }


    // 获取所有工具
    public Map<String, Tool> getAllTools() {
        return new HashMap<>(tools);
    }

    // 检查是否包含指定工具
    public boolean containsTool(String toolName) {
        return tools.containsKey(toolName);
    }

    // 获取工具数量
    public int getToolCount() {
        return tools.size();
    }

    // 获取工具列表
    public List<Tool> getTools() {

        Collection<Tool> values = null;
        if (CollUtil.isEmpty(tools) ||
                CollUtil.isEmpty(values = tools.values())) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values);
    }

    // 获取工具列表Dto
    public List<ToolDto> getToolDtos() {
        List<Tool> list = getTools();
        if (CollUtil.isEmpty(list)) return new ArrayList<>();
        return list.stream().map(Tool::convertToDto).collect(Collectors.toList());

    }

    // 获取工具名称列表
    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }

    // 获取工具（别名方法，保持兼容性）
    public Tool getTool(String name) {
        return get(name);
    }
}

