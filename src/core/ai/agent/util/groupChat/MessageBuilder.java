package ai.agent.util.groupChat;

import ai.agent.dto.groupChat.ExecutionTraceDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.ToolCallPayload;
import ai.agent.dto.llmCalling.ToolCall;
import ai.agent.enums.MessageType;
import ai.agent.enums.ToolCallStatus;

import java.util.List;

/**
 * 消息构建器
 * FIXME 后续移除或重构这个类
 */
public class MessageBuilder {

    // 发送者【用户】
    public static final String SENDER_ID_USER = "user";

    public static Message createUserTextMessage(String text, List<String> mentions) {
        return Message.createTextMessage(SENDER_ID_USER, text, mentions);
    }

    public static Message createAgentTextMessage(String agentId, String text, List<String> mentions) {
        return Message.createTextMessage(agentId, text, mentions);
    }
    public static Message createAgentTextMessageWithStyle(String agentId, String text, String style) {
        Message textMessage = Message.createTextMessage(agentId, text, null);
        textMessage.getTextPayload().setStyle(style);
        return textMessage;
    }

    public static Message createAgentErrorMessage(String senderId, String errorText, String errorDetail) {
        return Message.createAgentErrorMessage(senderId, errorText, errorDetail, null);
    }

    public static Message createAgentToolCallMessage(String agentId, ToolCall toolCall, List<String> mentions) {
        return Message.createToolCallMessage(agentId, toolCall, mentions);
    }


    public static Message createToolCallStatusUpdate(String agentId, ToolCall toolCall, ToolCallStatus status,
                                                     String result, String error, long executionTimeMs,
                                                     String rerenderMsgId, List<ExecutionTraceDto> executionTraces) {
        ToolCallPayload payload = new ToolCallPayload(toolCall, status, result, error, executionTimeMs);
        payload.setExecutionTraces(executionTraces);
        return new Message(agentId, MessageType.TOOL_CALL, payload, null, rerenderMsgId);
    }

    public static Message createOperateMessage(String senderId, String operateName, Object operateData, List<String> mentions) {
        return Message.createOperateMessage(senderId, operateName, operateData, mentions);
    }

    public static Message createTaskOperateMessage(String senderId, String operateName, Object operateData) {
        return createOperateMessage(senderId, operateName, operateData, null);
    }

    public static Message createUserAttachmentMessage(String id, String name) {
        return Message.createAttachmentMessage(SENDER_ID_USER, id, name);
    }
}
