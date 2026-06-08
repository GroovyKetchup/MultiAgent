package ai.agent.util.ucpParse;

import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AtParser {
    private static final Pattern AT_PATTERN = Pattern.compile("@\\S+");

    private AtParser() {}

    public static List<String> parseMentions(String content) {
        List<String> result = new ArrayList<>();
        if (content == null) return result;
        Matcher m = AT_PATTERN.matcher(content);
        while (m.find()) {
            String token = m.group();
            // strip leading '@'
            String name = token.substring(1);
            result.add(name);
        }
        return result;
    }

    /**
     * 解析@提及，如果没有@提及则默认@总控智能体
     */
    public static List<String> parseMentionsWithDefault(String content, GroupDefinition groupDefinition) {
        List<String> mentions = parseMentions(content);

        // 如果没有@提及且有群组定义，默认@总控智能体
        if (mentions.isEmpty() && groupDefinition != null) {
            AgentDefinition sysScheduler = groupDefinition.getSystemSchedulerAgent();
            if (sysScheduler != null) {
                mentions.add(sysScheduler.getAgentId());
            }
        }

        return mentions;
    }

    /**
     * 验证@权限：总控智能体可以@任何人，普通智能体可以@任何人（包括其他普通智能体和总控）
     */
    public static boolean validateMentionPermission(String fromAgentId, List<String> mentions, GroupDefinition groupDefinition) {
        if (mentions == null || mentions.isEmpty()) return true;
        if (groupDefinition == null) return true;

        AgentDefinition fromAgent = groupDefinition.getAgentDefinition(fromAgentId);
        if (fromAgent == null) return true;

        if (fromAgent.isSysScheduler()) return true;

        return false;
    }
}

