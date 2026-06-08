package ai.agent.enums;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("通知枚举")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-10", updateTime = "2025-09-10"
)
public enum NotificationEnums {
    SUCCESS,
    INFORMATION,
    ERROR,
    WARNING
}
