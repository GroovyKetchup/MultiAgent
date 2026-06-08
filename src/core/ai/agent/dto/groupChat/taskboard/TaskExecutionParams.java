package ai.agent.dto.groupChat.taskboard;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("任务执行参数接口")
@ClassDeclare(
        label = "任务执行参数接口",
        what = "定义任务执行参数的基础接口", why = "用于类型安全的任务参数传递和重试", how = "各类型任务实现此接口",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-16", updateTime = "2026-01-16"
)
public abstract class TaskExecutionParams implements Serializable {

    public TaskExecutionParams() {
    }

    /**
     * 获取任务执行类型
     */
    abstract TaskExecutionType getType();

}
