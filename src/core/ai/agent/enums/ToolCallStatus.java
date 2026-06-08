package ai.agent.enums;

/**
 * 工具调用状态枚举
 * 定义工具调用的不同执行状态
 */
public enum ToolCallStatus {
    /**
     * 准备执行 - 工具调用已创建，准备执行
     */
    PENDING,
    
    /**
     * 执行中 - 工具正在执行
     */
    EXECUTING,
    
    /**
     * 执行成功 - 工具执行完成且成功
     */
    SUCCESS,
    
    /**
     * 执行失败 - 工具执行失败
     */
    FAILED
}
