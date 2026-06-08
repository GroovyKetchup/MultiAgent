package ai.agent.enums;

/**
 * 消息类型枚举
 * 定义群组聊天中支持的不同消息类型
 */
public enum MessageType {
    /**
     * 普通文本消息 - 用户或智能体发送的普通文本内容
     */
    TEXT("TEXT"),
    /**
     * 普通文本消息 - 用户或智能体发送的普通文本内容
     */
    AGENT_ERROR("AGENT_ERROR"),

    /**
     * 计划制定消息 - 智能体制定的结构化计划
     */
    PLAN("PLAN"),

    /**
     * 工具调用消息 - 智能体调用工具的消息（包含执行前后状态）
     */
    TOOL_CALL("TOOL_CALL"),

    /**
     * 系统消息 - 系统级别的通知消息
     */
    SYSTEM("SYSTEM"),

    // 对话操作
    OPERATE("OPERATE"),

    /**
     * 附件消息 - 代表用户上传的附件（编号、名称）
     */
    ATTACHMENT("ATTACHMENT");

    public final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
