package ai.agent.engine.groupChat.tool.impl;

import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import cn.hutool.core.util.StrUtil;

@ToolDeclare(
        name = "ListGroupMembersTool",
        cnName = "查看群组成员",
        description = "查看当前群组中的所有智能体成员，包括成员的ID、中文名称、商业职务和角色介绍",
        scope = ToolScope.GROUP_CHAT
)
public class ListGroupMembersTool extends AbsGroupChatTool {

    @Override
    protected String executeInternal() {
        GroupDefinition groupDef = engine.getGroupDefinition();

        if (groupDef == null) {
            return "Error: No group definition found";
        }

        StringBuilder result = new StringBuilder();
        result.append("群组成员列表：\n");
        result.append("群组名称：").append(groupDef.getDisplayName()).append("\n\n");

        for (AgentDefinition agent : groupDef.getAgentDefinitions()) {
            result.append(getAgentInfo(agent));
        }

        result.append("共 ").append(groupDef.getAgentDefinitions().size()).append(" 名成员");

        return result.toString();
    }

    private String getAgentInfo(AgentDefinition agent) {
        String template = "\n- {}(AgentId:{}, BusinessRole:{}) \n Introduce:{}\n";
        return StrUtil.format(template, agent.getAgentName(), agent.getAgentId(),
                agent.getAgentBusinessRole(), agent.getAgentDescription());
    }
}
