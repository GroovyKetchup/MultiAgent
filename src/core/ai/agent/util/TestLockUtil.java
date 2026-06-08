package ai.agent.util;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.util.concurrent.locks.ReentrantLock;

@Comment("测试锁工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-12", updateTime = "2026-01-12"
)
public class TestLockUtil {

    // 标准交付锁
    public static final ReentrantLock STANDARD_DELIVERY_LOCK = new ReentrantLock();
}
