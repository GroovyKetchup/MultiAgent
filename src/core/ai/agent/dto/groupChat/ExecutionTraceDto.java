package ai.agent.dto.groupChat;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("执行轨迹Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-27", updateTime = "2025-11-27"
)
public class ExecutionTraceDto implements Serializable {

    // 分类
    private String category;
    // 操作
    private String operation;
    // 描述
    private String description;
    // 执行耗时（毫秒）
    private long executionTimeMs;

    public String getCategory() {
        return category;
    }

    public ExecutionTraceDto setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getOperation() {
        return operation;
    }

    public ExecutionTraceDto setOperation(String operation) {
        this.operation = operation;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ExecutionTraceDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public ExecutionTraceDto setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        return this;
    }
}
