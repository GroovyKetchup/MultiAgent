package ai.agent.dto.groupChat.taskboard;

import cmn.anotation.ClassDeclare;
import com.leavay.common.util.GsonUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("任务执行元信息")
@ClassDeclare(
        label = "任务执行元信息",
        what = "封装任务执行的类型和参数", why = "用于任务重试时获取类型化参数", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-16", updateTime = "2026-01-16"
)
public class TaskExecutionMetaInfo implements Serializable {

    private TaskExecutionType type;
    // 使用 Object 类型存储，避免 Gson 反序列化抽象类的问题
    private Object params;

    public TaskExecutionType getType() {
        return type;
    }

    public TaskExecutionMetaInfo setType(TaskExecutionType type) {
        this.type = type;
        return this;
    }

    public Object getParams() {
        return params;
    }

    public TaskExecutionMetaInfo setParams(Object params) {
        this.params = params;
        return this;
    }

    /**
     * 获取类型化的参数对象
     * @param clazz 参数类型
     * @return 类型化的参数对象
     */
    @SuppressWarnings("unchecked")
    public <T extends TaskExecutionParams> T getTypedParams(Class<T> clazz) {
        if (params == null) return null;
        // 如果已经是目标类型，直接返回
        if (clazz.isInstance(params)) {
            return (T) params;
        }
        // 否则通过 Gson 进行类型转换（处理反序列化后为 LinkedHashMap 的情况）
        String json = GsonUtil.toJson(params);
        return GsonUtil.fromJson(json, clazz);
    }
}
