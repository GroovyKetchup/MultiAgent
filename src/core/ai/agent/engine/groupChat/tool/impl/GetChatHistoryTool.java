package ai.agent.engine.groupChat.tool.impl;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.enums.MessageType;
import ai.agent.util.groupChat.MessageFormatterUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

@ToolDeclare(
        name = "GetChatHistoryTool",
        cnName = "获取聊天记录",
        description = "回溯式查询聊天记录。从最新消息往前回溯查看。支持增量查询避免重复。" +
                "参数说明：lookback=往前看多少条(默认30,最大100), skip=跳过最近多少条(默认0)。" +
                "使用示例：" +
                "首次查看最近10条: lookback=10, skip=0; " +
                "再往前看10条(第11-20条): lookback=10, skip=10; " +
                "查看最近20条: lookback=20, skip=0。" +
                "工具会自动修正超出范围的参数，并返回实际查询区间。",
        scope = ToolScope.GROUP_CHAT
)
public class GetChatHistoryTool extends AbsGroupChatTool {

    private static final int MAX_LOOK_BACK = 50;
    private static final int DEFAULT_LOOK_BACK = 30;

    @ParamDeclare(description = "往前看多少条消息，默认10，最大50", required = true, defaultValue = "操作已完成，可进行预览")
    private Integer lookBack;

    @ParamDeclare(description = "跳过最近多少条消息，默认0（从最新开始）", required = true)
    private Integer skip;

    @Override
    protected String executeInternal() {
        String selfDisplay = formatAgentDisplay(currentAgentId);
        StringBuilder sb = new StringBuilder();

        int actualLookBack = normalizeLookBack(lookBack);
        int actualSkip = normalizeSkip(skip);

        List<Message> allMessages = engine.getMessageHistoryManager().getFullHistory();
        int totalRawCount = allMessages.size();

        if (totalRawCount == 0) {
            return "当前会话暂无消息记录。";
        }

        List<Message> filteredMessages = MessageFormatterUtil.filterMessages(allMessages);

        int totalFilteredCount = filteredMessages.size();

        if (totalFilteredCount == 0) {
            return "当前会话暂无有效消息记录（所有消息已被过滤）。";
        }

        int endIndex = totalFilteredCount - actualSkip;
        int startIndex = Math.max(0, endIndex - actualLookBack);

        if (endIndex <= 0) {
            return StrUtil.format("无法获取消息：总共{}条有效消息，跳过{}条后已无剩余消息。", totalFilteredCount, actualSkip);
        }

        if (startIndex >= endIndex) {
            return StrUtil.format("无法获取消息：总共{}条有效消息，请求范围超出实际范围。", totalFilteredCount);
        }

        List<Message> targetMessages = filteredMessages.subList(startIndex, endIndex);
        int actualCount = targetMessages.size();

        int fromPosition = startIndex + 1;
        int toPosition = endIndex;

        sb.append("=== 聊天记录查询结果 ===\n");
        sb.append("⚠️ 重要提示：历史记录的内容是技术化的，不可直接模仿里面的内容信息。\n");
        sb.append(StrUtil.format("你的身份：{}\n", selfDisplay));
        sb.append(StrUtil.format("实际返回：最近第 {}-{} 条有效消息（共 {} 条）\n", fromPosition, toPosition, actualCount));
        sb.append(StrUtil.format("会话总消息数：{} 条（原始{}条，过滤后{}条有效消息）\n", totalFilteredCount, totalRawCount, totalFilteredCount));
        sb.append("消息按时间顺序排列（从旧到新，最新在底部）\n");
        sb.append("\n—— 以下为消息记录 ——\n\n");

        for (int i = 0; i < targetMessages.size(); i++) {
            Message message = targetMessages.get(i);
            MessageType messageType = message.getMessageType();

            String senderDisplay = formatAgentDisplay(message.getSenderId());
            String msgId = message.getMsgId();
            Long timestamp = message.getTimestamp();
            String timeStr = timestamp != null ? DateUtil.formatDateTime(DateUtil.date(timestamp)) : "未知时间";

            sb.append(StrUtil.format("[消息 #{}/{}]\n", startIndex + i + 1, totalFilteredCount));
            sb.append(StrUtil.format("消息ID: {}\n", msgId));
            sb.append(StrUtil.format("发送者: {}\n", senderDisplay));
            sb.append(StrUtil.format("消息类型: {}\n", messageType.toString()));
            sb.append(StrUtil.format("时间: {}\n", timeStr));
            sb.append("消息内容:\n");
            sb.append(MessageFormatterUtil.formatMessageContent(message));
            sb.append("\n---\n\n");
        }

        sb.append("=== 查询结束 ===\n");
        if (endIndex < totalFilteredCount) {
            sb.append(StrUtil.format("提示：还有 {} 条更新的有效消息未查看\n", totalFilteredCount - endIndex));
        }
        if (startIndex > 0) {
            sb.append(StrUtil.format("提示：还有 {} 条更早的有效消息未查看\n", startIndex));
        }

        return sb.toString();
    }

    private int normalizeLookBack(Integer lookBack) {
        if (lookBack == null) {
            return DEFAULT_LOOK_BACK;
        }
        int value = Math.abs(lookBack);
        return Math.min(value, MAX_LOOK_BACK);
    }

    private int normalizeSkip(Integer skip) {
        if (skip == null) {
            return 0;
        }
        return Math.max(0, Math.abs(skip));
    }

    private String formatAgentDisplay(String agentId) {
        if (agentId == null) {
            return "(unknown)";
        }

        AgentInstance agent = getGroupInstance().getAgent(agentId);
        if (agent != null && agent.getDefinition() != null) {
            String agentName = agent.getDefinition().getAgentName();
            if (StrUtil.isNotBlank(agentName)) {
                return StrUtil.format("{}({})", agentName, agentId);
            }
        }

        return agentId;
    }
}

