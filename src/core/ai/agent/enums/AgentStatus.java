package ai.agent.enums;

/**
 * 智能体状态枚举
 * 用于统一描述智能体的运行状态
 */
public enum AgentStatus {
    /** 空闲 */
    IDLE,
    /** 忙碌（执行中） */
    BUSY,
    /** 思考中（准备/推理） */
    THINKING,
    /** 出错 */
    ERROR,
}

