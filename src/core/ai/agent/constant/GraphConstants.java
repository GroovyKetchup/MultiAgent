package ai.agent.constant;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("图-常量")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-01", updateTime = "2025-12-01"
)
public class GraphConstants {

    // ========================= 节点-聊天场景 =========================
    public static final String NODE_CHAT_BEGIN = "CHAT_BEGIN";
    public static final String NODE_CHAT_END = "CHAT_END";
    public static final String NODE_ROUTER = "ROUTER";
    public static final String NODE_EXECUTOR = "EXECUTOR";
    public static final String NODE_CANVAS_PLANING = "NODE_CANVAS_PLANING";
    public static final String NODE_GENERAL_PLANING = "NODE_GENERAL_PLANING";


    // ========================= 节点-反思场景 =========================
    public static final String NODE_CONTENT_FILTER = "CONTENT_FILTER";
    public static final String NODE_REFLECTOR = "REFLECTOR";
    public static final String NODE_CURATOR = "CURATOR";


    // ========================= 初始化-参数 =========================

    // 触发消息（触发聊天场景的消息）
    public static final String PARAM_TRIGGER_MESSAGE = "TRIGGER_MESSAGE";
    // 任务评价（触发反思场景的消息）
    public static final String PARAM_TASK_EVALUATION = "TASK_EVALUATION";


}
