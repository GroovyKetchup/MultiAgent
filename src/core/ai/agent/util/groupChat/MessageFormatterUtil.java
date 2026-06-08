package ai.agent.util.groupChat;

import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.ToolCallPayload;
import ai.agent.enums.MessageType;
import ai.agent.enums.ToolCallStatus;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 消息格式化工具类
 * 提供消息过滤、格式化等功能
 */
public class MessageFormatterUtil {

    private static final int DEFAULT_MAX_TOOL_CALL_CONTENT_LENGTH = 500;
    


    /**
     * 过滤消息列表，排除不需要的消息
     * @param messages 原始消息列表
     * @return 过滤后的消息列表
     */
    public static List<Message> filterMessages(List<Message> messages) {
        List<Message> filtered = new ArrayList<>();
        for (Message message : messages) {
            if (shouldIncludeMessage(message)) {
                filtered.add(message);
            }
        }
        return filtered;
    }

    /**
     * 格式化消息列表，返回格式化后的字符串列表
     * @param messages 消息列表
     * @return 格式化后的字符串列表
     */
    public static List<String> formatMessages(List<Message> messages) {
        List<String> result = new ArrayList<>();
        for (Message message : messages) {
            result.add(formatMessageContent(message));
        }
        return result;
    }

    /**
     * 过滤并格式化消息列表
     * @param messages 原始消息列表
     * @return 过滤并格式化后的字符串列表
     */
    public static List<String> filterAndFormatMessages(List<Message> messages) {
        return formatMessages(filterMessages(messages));
    }

    /**
     * 格式化消息列表，带发送者显示名称解析
     * @param messages 消息列表
     * @param senderDisplayResolver 发送者ID到显示名称的解析函数
     * @return 格式化后的字符串列表（包含发送者信息）
     */
    public static List<String> formatMessagesWithSender(List<Message> messages, Function<String, String> senderDisplayResolver) {
        List<String> result = new ArrayList<>();
        for (Message message : messages) {
            String senderDisplay = senderDisplayResolver != null 
                    ? senderDisplayResolver.apply(message.getSenderId()) 
                    : message.getSenderId();
            String content = formatMessageContent(message);
            result.add(StrUtil.format("[{}] {}: {}", message.getMessageType(), senderDisplay, content));
        }
        return result;
    }

    /**
     * 判断消息是否应该被包含
     * @param message 消息
     * @return 是否包含
     */
    public static boolean shouldIncludeMessage(Message message) {
        if (message == null) {
            return false;
        }
        MessageType messageType = message.getMessageType();
        if (messageType == null) {
            return false;
        }

        if (messageType == MessageType.OPERATE) {
            return false;
        }

        if (messageType == MessageType.TOOL_CALL) {
            ToolCallPayload toolCallPayload = message.getToolCallPayload();
            if (toolCallPayload == null) {
                return false;
            }
            ToolCallStatus status = toolCallPayload.getStatus();
            return status == ToolCallStatus.SUCCESS || status == ToolCallStatus.FAILED;
        }

        return true;
    }

    /**
     * 格式化单条消息内容
     * @param message 消息
     * @return 格式化后的内容字符串
     */
    public static String formatMessageContent(Message message) {
        MessageType messageType = message.getMessageType();
        String content = JSONUtil.toJsonStr(message.getPayload());
        return truncateByMessageType(content, messageType, message);
    }

    /**
     * 根据消息类型截断内容
     */
    private static String truncateByMessageType(String content, MessageType messageType, Message message) {
        switch (messageType) {
            case TEXT:
                return truncateTextMessage(content);
            case ATTACHMENT:
                return truncateAttachmentMessage(content);
            case TOOL_CALL:
                return truncateToolCallMessage(content, message);
            case PLAN:
                return truncatePlanMessage(content);
            case OPERATE:
                return truncateOperateMessage(content);
            case SYSTEM:
                return truncateSystemMessage(content);
            case AGENT_ERROR:
                return truncateAgentErrorMessage(content);
            default:
                return content;
        }
    }

    private static String truncateTextMessage(String content) {
        return content;
    }

    private static String truncateAttachmentMessage(String content) {
        return content;
    }

    private static String truncateToolCallMessage(String content, Message message) {
        if (content == null) {
            return content;
        }

        if (content.length() > DEFAULT_MAX_TOOL_CALL_CONTENT_LENGTH) {
            String toolName = "未知工具";
            ToolCallPayload toolCallPayload = message.getToolCallPayload();
            if (toolCallPayload != null && toolCallPayload.getToolCall() != null) {
                toolName = toolCallPayload.getToolCall().getToolName();
            }

            return StrUtil.format(
                    "[工具调用成功] {}：返回内容较长（超过{}字符），消息省略。",
                    toolName,
                    DEFAULT_MAX_TOOL_CALL_CONTENT_LENGTH
            );
        }

        return content;
    }

    private static String truncatePlanMessage(String content) {
        return content;
    }

    private static String truncateOperateMessage(String content) {
        return content;
    }

    private static String truncateSystemMessage(String content) {
        return content;
    }

    private static String truncateAgentErrorMessage(String content) {
        return content;
    }
}
