package ai.agent.service.groupChat;

import ai.agent.dto.groupChat.MessageContextDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.OperatePayload;
import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.enums.OperateMessageEnums;
import ai.agent.util.ConsolePrintUtil;
import cell.ai.agent.IGroupChatMessageService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import octocm.domain.dto.DomainDto;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 消息历史管理器
 * 支持滑动窗口的消息历史管理
 */
public class MessageHistoryManager implements Serializable {

    // 最大消息记录
    private static final int MAX_HISTORY_SIZE = 1000;
    // 消息窗口大小
    private static final int WINDOW_SIZE = 20;
    // 消息上下文最大记录数
    private static final int MAX_CONTEXT_SIZE = 20;

    // 最大记录数
    private int maxHistorySize;
    // 窗口大小
    private int windowSize;
    // 上下文时间限制（如果有，那么这个之前的消息不允许进入上下文）
    private long contextTimeLimit;

    // 消息记录
    private ConcurrentLinkedQueue<Message> messageHistory;
    // 消息上下文记录（msgId -> MessageContextDto）
    // 使用LinkedHashMap保持插入顺序，实现FIFO
    private LinkedHashMap<String, MessageContextDto> messageContextMap;

    public MessageHistoryManager() {
        this.messageHistory = new ConcurrentLinkedQueue<>();
        this.messageContextMap = new LinkedHashMap<>(16, 0.75f, false);
        this.maxHistorySize = MAX_HISTORY_SIZE;
        this.windowSize = WINDOW_SIZE;
    }

    public MessageHistoryManager(int maxHistorySize, int windowSize) {
        this.messageHistory = new ConcurrentLinkedQueue<>();
        this.messageContextMap = new LinkedHashMap<>(16, 0.75f, false);
        this.maxHistorySize = maxHistorySize;
        this.windowSize = windowSize;
    }

    // 添加消息到历史记录
    public void addMessage(DomainDto domain, Message message) {

        messageHistory.offer(message);

        // 保持历史记录大小限制
        while (messageHistory.size() > maxHistorySize) {
            messageHistory.poll();
        }

        try {
            // 添加记录
            // 后续如有必要改为异步
            IGroupChatMessageService.get()
                    .add(
                            domain,
                            message.getMsgId(),
                            message.getMessageType().getValue(),
                            "",
                            JSON.toJSONString(message)
                    );
        } catch (Exception ignored) {

        }


    }


    public void addMessageOnlyHistory(Message message) {
        messageHistory.offer(message);
    }


    // 删除消息
    public void deleteMessageByIds(List<String> msgIds) {
        if (CollUtil.isEmpty(msgIds)) return;
        if (CollUtil.isEmpty(messageHistory)) return;

        Set<String> deleteTargetIds = new HashSet<>(msgIds);

        messageHistory.removeIf(message -> deleteTargetIds.contains(message.getMsgId()));
    }


    // 获取最后一条消息
    public Message getLastMessage() {
        if (messageHistory == null || messageHistory.isEmpty()) return null;
        return new ArrayList<>(messageHistory).get(messageHistory.size() - 1);
    }

    /**
     * 获取滑动窗口消息历史
     * 返回最近的windowSize条消息
     */
    public List<Message> getWindowHistory() {
        List<Message> allMessages = getFullHistory();

        if (allMessages.size() <= windowSize) {
            return Collections.unmodifiableList(allMessages);
        }

        // 返回最后windowSize条消息
        int startIndex = allMessages.size() - windowSize;

        List<Message> messages = allMessages.subList(startIndex, allMessages.size());

        // 基于设定的过滤值进行过滤
        int filterBeforeMsgSizeNo = messages.size();
        if (this.contextTimeLimit > 0) {
            for (int i = allMessages.size() - 1; i >= 0; i--) {
                Message message = allMessages.get(i);
                if (message.getTimestamp() < contextTimeLimit) {
                    messages = allMessages.subList(i + 1, allMessages.size());
                    break;
                }
            }

        }

        ConsolePrintUtil.printGreenLn(
                StrUtil.format(
                        "过滤前消息数:{}, 过滤后消息数:{}, 界限时间戳:{}",
                        filterBeforeMsgSizeNo,
                        messages.size(),
                        this.contextTimeLimit
                )
        );

        return Collections.unmodifiableList(messages);
    }

    /**
     * 获取滑动窗口消息历史
     * 返回模型使用的标准结构
     */
    public List<LlmMessage> getWindowHistoryWithLlmMessage(boolean onlyAfterCanvasOpen) {
        List<LlmMessage> llmMessages = new ArrayList<>();
        List<Message> oriMessages = getWindowHistory();
        if (oriMessages.isEmpty()) return llmMessages;

        int startIndex = 0;
        // 如果只需要打开Canvas之后的消息
        if (onlyAfterCanvasOpen) {
            for (int i = oriMessages.size() - 1; i >= 0; i--) {
                Message message = oriMessages.get(i);
                if (message.isOperateMessage()) {
                    OperatePayload operatePayload = message.getOperatePayload();
                    if (operatePayload == null) continue;
                    String opName = operatePayload.getOperateName();
                    if (OperateMessageEnums.CANVAS_OPEN.toString().equals(opName)) {
                        startIndex = i;
                        break;
                    }
                    if (OperateMessageEnums.CANVAS_CLOSE.toString().equals(opName)) {
                        break;
                    }

                }

            }
        }


        oriMessages = oriMessages.subList(startIndex, oriMessages.size());

        for (Message msg : oriMessages) {
            if (msg.getTextPayload() == null) {
                continue; // 跳过没有文本内容的消息
            }
            if (LlmMessage.Role_User.equals(msg.getSenderId())) {
                llmMessages.add(new LlmMessage(LlmMessage.Role_User, msg.getTextPayload().getText()));
            } else if (msg.isTextMessage()) {
                llmMessages.add(new LlmMessage(LlmMessage.Role_Assistant, msg.getTextPayload().getText()));
            }
        }

        return llmMessages;
    }


    /**
     * 获取完整消息历史
     */
    public List<Message> getFullHistory() {
        return Collections.unmodifiableList(new ArrayList<>(messageHistory));
    }


    /**
     * 根据锚点时间戳，获取其前后指定数量的消息列表。
     * * * 警告：由于底层为无序队列，此方法性能为 O(N log N)，N 为队列总消息数。
     *
     * @param anchorTimestamp 锚点时间戳
     * @param beforeCount     锚点之前的消息数量（例如：10 代表取前 10 条）
     * @param afterCount      锚点之后的消息数量（例如：5 代表取后 5 条）
     * @return 包含前后消息的有序列表。
     */
    public List<Message> getContextMessagesByTimestamp(long anchorTimestamp, int beforeCount, int afterCount) {

        List<Message> allMessages = new ArrayList<>(getFullHistory());

        allMessages.sort(Comparator.comparing(Message::getTimestamp));

        int anchorIndex = -1;
        for (int i = 0; i < allMessages.size(); i++) {
            if (allMessages.get(i).getTimestamp() >= anchorTimestamp) {
                anchorIndex = i;
                break;
            }
        }

        if (anchorIndex == -1) {
            if (!allMessages.isEmpty()) {
                anchorIndex = allMessages.size();
            } else {
                return Collections.emptyList();
            }
        }


        int actualBeforeCount = beforeCount;
        int actualAfterCount = afterCount;

        // 如果锚点处的元素时间戳 == anchorTimestamp，则锚点本身算作上下文
        boolean includesAnchor = (anchorIndex < allMessages.size() && allMessages.get(anchorIndex).getTimestamp() == anchorTimestamp);

        int startIndex;
        int endIndex;

        if (includesAnchor) {
            startIndex = anchorIndex - actualBeforeCount;
            endIndex = anchorIndex + actualAfterCount;
        } else {
            startIndex = anchorIndex - actualBeforeCount;
            endIndex = anchorIndex + actualAfterCount - 1; // 因为没有锚点占位
        }

        int safeStartIndex = Math.max(0, startIndex);

        int safeEndIndex = Math.min(allMessages.size() - 1, endIndex);

        if (safeStartIndex > safeEndIndex) {
            return Collections.emptyList();
        }

        return allMessages.subList(safeStartIndex, safeEndIndex + 1);
    }

    /**
     * 根据msgId查找消息
     */
    public Message findMessageById(String msgId) {
        return messageHistory.stream()
                .filter(msg -> msg.getMsgId().equals(msgId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取指定发送者的消息历史
     */
    public List<Message> getMessagesBySender(String senderId) {
        return messageHistory.stream()
                .filter(msg -> msg.getSenderId().equals(senderId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 清空消息历史
     */
    public void clear() {
        messageHistory.clear();
    }

    /**
     * 获取消息总数
     */
    public int size() {
        return messageHistory.size();
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return messageHistory.isEmpty();
    }


    // ========================= 支撑方法 =========================

    public ConcurrentLinkedQueue<Message> getMessageHistory() {
        return messageHistory;
    }

    public MessageHistoryManager setMessageHistory(ConcurrentLinkedQueue<Message> messageHistory) {
        this.messageHistory = messageHistory;
        return this;
    }

    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    public MessageHistoryManager setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
        return this;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public MessageHistoryManager setWindowSize(int windowSize) {
        this.windowSize = windowSize;
        return this;
    }

    public long getContextTimeLimit() {
        return contextTimeLimit;
    }

    public MessageHistoryManager setContextTimeLimit(long contextTimeLimit) {
        this.contextTimeLimit = contextTimeLimit;
        return this;
    }

    public LinkedHashMap<String, MessageContextDto> getMessageContextMap() {
        return messageContextMap;
    }

    public MessageHistoryManager setMessageContextMap(LinkedHashMap<String, MessageContextDto> messageContextMap) {
        this.messageContextMap = messageContextMap;
        return this;
    }

    public void recordMessageContext(String msgId, MessageContextDto contextDto) {
        if (msgId == null || contextDto == null) return;

        synchronized (messageContextMap) {
            messageContextMap.put(msgId, contextDto);

            // 如果超过阈值，移除最旧的记录（FIFO）
            if (messageContextMap.size() > MAX_CONTEXT_SIZE) {
                Iterator<String> iterator = messageContextMap.keySet().iterator();
                if (iterator.hasNext()) {
                    String oldestKey = iterator.next();
                    iterator.remove();
                    ConsolePrintUtil.printYellowLn(StrUtil.format(
                            "[上下文管理] 超过阈值{}，移除最旧记录: {}",
                            MAX_CONTEXT_SIZE, oldestKey));
                }
            }
        }
    }

    public MessageContextDto getMessageContext(String msgId) {
        if (msgId == null) return null;
        synchronized (messageContextMap) {
            return messageContextMap.get(msgId);
        }
    }

    public void clearMessageContexts() {
        synchronized (messageContextMap) {
            messageContextMap.clear();
            ConsolePrintUtil.printGreenLn("[上下文管理] 已清空所有消息上下文记录");
        }
    }
}
