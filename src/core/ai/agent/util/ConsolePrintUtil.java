package ai.agent.util;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

@Comment("控制台输出工具")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-04", updateTime = "2025-09-04"
)
public class ConsolePrintUtil {

    public static final String SYSTEM_LOG_PREFIX = "GROUP_CHAT";

    public static void printRedLn(String text) {
        println(StrUtil.format("\u001b[31m{}\u001b[0m", text));

    }

    public static void printGreenLn(String text) {
        println(StrUtil.format("\u001b[32m{}\u001b[0m", text));
    }

    public static void printWhiteLn(String text) {
        println(StrUtil.format("\u001b[37m{}\u001b[0m", text));
    }

    public static void printYellowLn(String text) {
        println(StrUtil.format("\u001b[33m{}\u001b[0m", text));
    }

    public static void printBlueLn(String text) {
        println(StrUtil.format("\u001b[34m{}\u001b[0m", text));
    }

    public static void printCyanLn(String text) {
        println(StrUtil.format("\u001b[36m{}\u001b[0m", text));
    }


    public static void println(String text) {
        System.out.println(
                StrUtil.format("[{}]\t{}", SYSTEM_LOG_PREFIX, text)
        );
    }


}
