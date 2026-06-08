package ai.agent.constant;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("通知类型的常量")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-16", updateTime = "2025-09-16"
)
public class NotifyConstants {


    // 任务已完成的短信模板
    public static final String SMS_CONTENT_TEMPLATE_TASK_OVER = "2521750";


}
