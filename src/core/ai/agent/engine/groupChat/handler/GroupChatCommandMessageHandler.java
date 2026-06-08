package ai.agent.engine.groupChat.handler;

import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.util.ConsolePrintUtil;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.util.HashSet;
import java.util.Set;

import static ai.agent.constant.GroupChatConstants.INTERNAL_PLAN_TRIGGER_PREFIX;

@Comment("群聊命令消息处理类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-12", updateTime = "2025-09-12"
)
public class GroupChatCommandMessageHandler {

    // 系统指令
    private static final Set<String> SYSTEM_COMMANDS = new HashSet<>();

    public static final String CMD_NOT_TRIGGER_ANYONE = "CMD_NOT_TRIGGER_ANYONE";

    static {
        SYSTEM_COMMANDS.add(CMD_NOT_TRIGGER_ANYONE);
    }

    public static boolean isCommandMessage(String text) {
        if (StrUtil.isBlank(text)) return false;
        if (isSystemCommand(text)) return true;
        if (isInternalPlanTrigger(text)) return true;

        return false;
    }


    // 处理类
    public static void handle(GroupChatEngine engine, AgentInstance agentInstance, String text) throws InterruptedException {
        if (isSystemCommand(text)) {
            doHandleSystemCommand(text);
        } else if (isInternalPlanTrigger(text)) {
            doHandleInternalPlanTrigger(engine, agentInstance, text);
        }
    }


    // ========================= 检验方法 =========================

    public static boolean isSystemCommand(String text) {
        return SYSTEM_COMMANDS.contains(text);
    }
    
    public static void handleSystemCommand(String text) {
        doHandleSystemCommand(text);
    }

    // 是内部计划触发消息
    private static boolean isInternalPlanTrigger(String text) {
        return StrUtil.isNotBlank(text) && text.startsWith(INTERNAL_PLAN_TRIGGER_PREFIX);
    }


    // ========================= 实际处理方法 =========================

    // 处理系统命令
    private static void doHandleSystemCommand(String text) {

        ConsolePrintUtil.printWhiteLn(StrUtil.format("用户发送了系统指令：[{}]", text));

        switch (text) {
            case CMD_NOT_TRIGGER_ANYONE:
                ConsolePrintUtil.printRedLn("执行系统指令CMD_NOT_TRIGGER_ANYONE，不触发任何智能体");

                break;
            default:
                break;
        }

    }

    // 处理内部计划
    private static void doHandleInternalPlanTrigger(GroupChatEngine engine, AgentInstance agentInstance, String text) throws InterruptedException {
        // 内部触发消息现在由GraphEngine的RouterNode自动处理（非侵入式）
        // 这里不再需要任何逻辑，消息会自动触发智能体执行
        ConsolePrintUtil.printGreenLn("[内部触发计划] 消息已由GraphEngine.RouterNode处理");
    }
}
