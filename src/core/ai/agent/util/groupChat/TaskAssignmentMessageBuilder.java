package ai.agent.util.groupChat;

import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.dto.groupChat.taskboard.TaskLogItem;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.enums.TaskStatus;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务分配消息构建器
 * - 仅依据 TaskBoardItem 本身的字段生成消息正文
 * - 不硬编码执行步骤/要求
 */
public class TaskAssignmentMessageBuilder {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.CHINA)
            .withZone(ZoneId.systemDefault());

    private static final Pattern BRACKET_TOKEN = Pattern.compile("\\[([^\\]]+)\\]");

    private TaskAssignmentMessageBuilder() {}

    public static String build(TaskBoardItem task, AgentInstance assignee) {
        if (task == null) {
            return "## 任务分配通知\n\n任务信息缺失";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务分配通知\n\n");
        sb.append("**任务名称：** ").append(nvl(task.getTaskName())).append("\n\n");
        sb.append("**任务负责人：** ").append(nvl(assignee.getDefinition().getAgentName())).append("\n\n");

        // 基本元信息
        sb.append("**状态：** ").append(statusName(task.getStatus())).append("\n\n");

        if (task.getCreateTime() != null) {
            sb.append("**创建时间：** ").append(TS_FMT.format(task.getCreateTime())).append("\n\n");
        }
        if (task.getUpdateTime() != null) {
            sb.append("**最近更新时间：** ").append(TS_FMT.format(task.getUpdateTime())).append("\n\n");
        }

        // 最后给出通用引导，但不规定具体步骤
        sb.append("请基于任务名称与描述，自主拟定执行计划并按序推进；如需使用工具，请参考上方提示。");
        return sb.toString();
    }

    private static String nvl(String s) { return s == null ? "" : s; }
    private static String safe(String s) { return s == null ? "" : s; }

    private static String statusName(TaskStatus status) {
        if (status == null) return "未知";
        switch (status) {
            case PENDING: return "待开始";
            case IN_PROGRESS: return "进行中";
            case COMPLETED: return "已完成";
            default: return status.name();
        }
    }

    private static List<String> extractBracketTokens(String text) {
        List<String> list = new ArrayList<>();
        if (text == null || text.isEmpty()) return list;
        Matcher m = BRACKET_TOKEN.matcher(text);
        Set<String> uniq = new LinkedHashSet<>();
        while (m.find()) {
            String token = m.group(1).trim();
            if (!token.isEmpty()) {
                uniq.add(token);
            }
        }
        list.addAll(uniq);
        return list;
    }

    private static List<TaskLogItem> lastNLogs(TaskBoardItem task, int n) {
        List<TaskLogItem> logs = task.getLogs();
        List<TaskLogItem> result = new ArrayList<>();
        if (logs == null || logs.isEmpty()) return result;
        int start = Math.max(0, logs.size() - n);
        for (int i = start; i < logs.size(); i++) {
            result.add(logs.get(i));
        }
        return result;
    }
}

