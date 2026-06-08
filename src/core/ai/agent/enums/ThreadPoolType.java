package ai.agent.enums;

public enum ThreadPoolType {
    // 常规任务 (CPU/IO混杂，耗时长，允许排队)
    REGULAR_TASK,

    // 消息归档 (IO密集，高并发，绝对不可丢数据)
    MSG_ARCHIVE,

    // 运维日志 (低优先级，允许丢弃)
    OPS_LOG,

    // 调度任务 (定时清理过期数据)
    SCHEDULER



}