package ai.agent.enums;

public enum OperateMessageEnums {

    // 系统消息指令
    SHOW_FULL_SCREEN_LOADING, // 打开全屏加载
    CLOSE_FULL_SCREEN_LOADING,// 关闭全屏加载
    SHOW_TOAST, // 显示Toast
    SHOW_CONFIRM, // 显示确认框
    POP_PROGRESS,
    POP_RESULT_VIEW,
    POP_DIALOG_IF_START_SMS_NOTIFY,
    CLOSE_PROGRESS,
    ADD_PROGRESS_MESSAGE,
    AGENT_STATUS_CHANGE,

    // 对话状态相关指令
    CHAT_BEGIN, // 对话开始
    CHAT_END, // 对话结束
    AGGREGATION_BEGIN, // 聚合开始
    AGGREGATION_END, // 聚合结束

    // 工作区相关指令
    WORKSPACE_OPEN,
    WORKSPACE_CLOSE,

    // 画布相关指令
    CANVAS_OPEN,
    CANVAS_CLOSE,
    CANVAS_CALL_ACTION,
    CANVAS_GET_STATUS,

    // 子画布相关指令
    SUB_CANVAS_MOVE_TO_PRIMARY,
    SUB_CANVAS_MOVE_OUT_PRIMARY,
    SUB_CANVAS_PREVIEW,

    // 子会话相关指令
    SUB_SESSION_PENDING,
    SUB_SESSION_STARTED,
    SUB_SESSION_COMPLETED,
    SUB_SESSION_FAILED,
    SUB_SESSION_WAIT_REPLY,


}
