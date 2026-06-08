package ai.agent.dto.groupChat.message;

import ai.agent.dto.groupChat.message.payload.*;
import ai.agent.dto.groupChat.plan.Plan;
import ai.agent.dto.llmCalling.ToolCall;
import ai.agent.enums.MessageType;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;
import java.util.UUID;

/**
 * 消息类
 * 支持多种消息类型：文本、计划、工具调用等
 * 前端可以根据MessageId进行替换或新增
 */
public class Message {
    private String msgId;
    private String senderId;
    private Integer msgSeqNo; // 序号
    private Long timestamp;
    private MessageType messageType;

    @JSONField(deserialize = false)
    private MessagePayload payload;
    private List<String> mentions; // @提及的智能体列表
    private String rerenderMsgId; // 指向需要重新渲染的消息ID

    public Message() {
    }

    // 构造函数 - 自动生成msgId和timestamp
    public Message(String senderId, MessageType messageType, MessagePayload payload, List<String> mentions) {
        this.msgId = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.messageType = messageType;
        this.payload = payload;
        this.mentions = mentions;
        this.rerenderMsgId = null; // 普通消息不需要重新渲染其他消息
    }

    // 构造函数 - 指定msgId（用于更新现有消息）
    public Message(String msgId, String senderId, MessageType messageType, MessagePayload payload, List<String> mentions) {
        this.msgId = msgId;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.messageType = messageType;
        this.payload = payload;
        this.mentions = mentions;
        this.rerenderMsgId = null; // 默认不重新渲染其他消息
    }

    // 构造函数 - 带rerenderMsgId（用于状态更新通知）
    public Message(String senderId, MessageType messageType, MessagePayload payload, List<String> mentions, String rerenderMsgId) {
        this.msgId = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.messageType = messageType;
        this.payload = payload;
        this.mentions = mentions;
        this.rerenderMsgId = rerenderMsgId;
    }

    // 静态工厂方法 - 创建文本消息
    public static Message createTextMessage(String senderId, String text, List<String> mentions) {
        return new Message(senderId, MessageType.TEXT, new TextPayload(text), mentions);
    }

    // 静态工厂方法 - 创建智能体错误消息
    public static Message createAgentErrorMessage(String senderId, String errorText, String errorDetail, List<String> mentions) {
        return new Message(senderId, MessageType.AGENT_ERROR,
                new AgentErrorPayload(errorText, errorDetail), mentions);
    }

    // 静态工厂方法 - 创建计划消息
    public static Message createPlanMessage(String senderId, Plan plan, String description, List<String> mentions) {
        return new Message(senderId, MessageType.PLAN, new PlanPayload(plan, description), mentions);
    }

    // 静态工厂方法 - 创建工具调用消息（执行前）
    public static Message createToolCallMessage(String senderId, ToolCall toolCall, List<String> mentions) {
        return new Message(senderId, MessageType.TOOL_CALL, new ToolCallPayload(toolCall), mentions);
    }

    // 静态工厂方法 - 创建操作消息
    public static Message createOperateMessage(String senderId, String operateName, Object operateData, List<String> mentions) {
        return new Message(senderId, MessageType.OPERATE, new OperatePayload(operateName, operateData), mentions);
    }

    // 静态工厂方法 - 创建附件消息
    public static Message createAttachmentMessage(String senderId, String id, String name) {
        return new Message(senderId, MessageType.ATTACHMENT, new AttachmentPayload(id, name), null);
    }
    // Getters
    public String getMsgId() {
        return msgId;
    }

    public String getSenderId() {
        return senderId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public MessagePayload getPayload() {
        return payload;
    }

    public List<String> getMentions() {
        return mentions;
    }

    public String getRerenderMsgId() {
        return rerenderMsgId;
    }

    // 类型检查方法
    public boolean isTextMessage() {
        return messageType == MessageType.TEXT;
    }

    public boolean isPlanMessage() {
        return messageType == MessageType.PLAN;
    }

    public boolean isToolCallMessage() {
        return messageType == MessageType.TOOL_CALL;
    }

    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }

    public boolean isOperateMessage() {
        return messageType == MessageType.OPERATE;
    }

    public boolean isAttachmentMessage() {
        return messageType == MessageType.ATTACHMENT;
    }

    // 类型安全的载荷获取方法
    public TextPayload getTextPayload() {
        if (isTextMessage()) {
            return (TextPayload) payload;
        }
        return null;
    }

    public PlanPayload getPlanPayload() {
        if (isPlanMessage()) {
            return (PlanPayload) payload;
        }
        return null;

    }

    public ToolCallPayload getToolCallPayload() {
        if (isToolCallMessage()) {
            return (ToolCallPayload) payload;
        }
        return null;

    }

    public OperatePayload getOperatePayload() {
        if (isOperateMessage()) {
            return (OperatePayload) payload;
        }
        return null;

    }

    public AttachmentPayload getAttachmentPayload() {
        if (isAttachmentMessage()) {
            return (AttachmentPayload) payload;
        }
        return null;

    }


    // ========================= getter and setter =========================


    public Integer getMsgSeqNo() {
        return msgSeqNo;
    }

    public Message setMsgSeqNo(Integer msgSeqNo) {
        this.msgSeqNo = msgSeqNo;
        return this;
    }

    public Message setMsgId(String msgId) {
        this.msgId = msgId;
        return this;
    }

    public Message setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public Message setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Message setMessageType(MessageType messageType) {
        this.messageType = messageType;
        return this;
    }

    public Message setPayload(MessagePayload payload) {
        this.payload = payload;
        return this;
    }

    public Message setMentions(List<String> mentions) {
        this.mentions = mentions;
        return this;
    }

    public Message setRerenderMsgId(String rerenderMsgId) {
        this.rerenderMsgId = rerenderMsgId;
        return this;
    }

    /**
     * 验证消息的有效性
     */
    public boolean isValid() {
        return msgId != null && !msgId.trim().isEmpty() &&
                senderId != null && !senderId.trim().isEmpty() &&
                timestamp != null &&
                messageType != null &&
                payload != null && payload.isValid();
    }

    @Override
    public String toString() {
        return "ExtendedMessage{" +
                "msgId='" + msgId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", timestamp=" + timestamp +
                ", messageType=" + messageType +
                ", payload=" + payload +
                ", mentions=" + mentions +
                '}';
    }
}
