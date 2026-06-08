package ai.agent.dto.groupChat.taskboard;

import java.io.Serializable;

/**
 * 任务日志项
 */
public class TaskLogItem implements Serializable {
    private final String action;      // 操作类型
    private final String operatorId;  // 操作者
    private final String details;     // 详情
    private final long timestamp;     // 毫秒

    public TaskLogItem(String action, String operatorId, String details, long timestamp) {
        this.action = action;
        this.operatorId = operatorId;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String getAction() { return action; }
    public String getOperatorId() { return operatorId; }
    public String getDetails() { return details; }
    public long getTimestamp() { return timestamp; }
}

