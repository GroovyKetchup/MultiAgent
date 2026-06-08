package ai.agent.service.groupChat;

import ai.agent.dto.groupChat.message.Message;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.session.SessionSummary;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.enums.ThreadPoolType;
import ai.agent.service.GroupChatThreadPollManager;
import ai.agent.service.llmCalling.LLMClient;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.MessageFormatterUtil;
import ai.agent.util.llmCalling.PromptBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天记录压缩服务
 * 异步压缩子会话聊天记录为简洁总结
 */
public class ChatSummaryCompressor {

    /**
     * 异步压缩聊天记录
     *
     * @param subSession   子会话实例
     * @param llmClient    LLM客户端
     * @param llmConfig    LLM配置
     * @param startIndex   本轮消息起始索引
     * @param roundIndex   轮次序号
     * @return CompletableFuture<SessionSummary>
     */
    public static CompletableFuture<SessionSummary> compressAsync(
            SubSessionInstance subSession,
            LLMClient llmClient,
            LLMConfig llmConfig,
            int startIndex,
            int roundIndex) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                MessageHistoryManager historyManager = subSession.getMessageHistoryManager();
                if (historyManager == null) {
                    ConsolePrintUtil.printYellowLn("[ChatSummaryCompressor] 消息历史管理器为空，跳过压缩");
                    return null;
                }

                List<Message> allMessages = historyManager.getFullHistory();
                int endIndex = allMessages.size();

                if (startIndex >= endIndex) {
                    ConsolePrintUtil.printYellowLn("[ChatSummaryCompressor] 无新消息需要压缩");
                    return null;
                }

                List<Message> currentRoundMessages = allMessages.subList(
                        Math.min(startIndex, endIndex),
                        endIndex
                );

                List<Message> filteredMessages = MessageFormatterUtil.filterMessages(currentRoundMessages);
                if (filteredMessages.isEmpty()) {
                    ConsolePrintUtil.printYellowLn("[ChatSummaryCompressor] 过滤后无有效消息，跳过压缩");
                    return null;
                }

                List<String> formattedMessages = MessageFormatterUtil.formatMessages(filteredMessages);

                // 获取智能体实例，传给 PromptBuilder 构建上下文
                AgentInstance agentInstance = subSession.getAgentInstance();

                String prompt = PromptBuilder.buildChatSummaryCompressionPrompt(
                        formattedMessages, roundIndex, agentInstance);

                ConsolePrintUtil.printGreenLn("[ChatSummaryCompressor] 开始压缩第" + roundIndex + "轮会话，消息数：" + filteredMessages.size());

                String summary = llmClient.callLlm(llmConfig, prompt);

                ConsolePrintUtil.printGreenLn("[ChatSummaryCompressor] 第" + roundIndex + "轮压缩完成");

                return new SessionSummary(
                        roundIndex,
                        summary,
                        System.currentTimeMillis(),
                        filteredMessages.size()
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ConsolePrintUtil.printRedLn("[ChatSummaryCompressor] 压缩任务被中断");
                return null;
            } catch (Exception e) {
                ConsolePrintUtil.printRedLn("[ChatSummaryCompressor] 压缩失败: " + e.getMessage());
                return null;
            }
        }, GroupChatThreadPollManager.get(ThreadPoolType.REGULAR_TASK));
    }
}
