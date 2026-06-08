package ai.agent.engine.groupChat.tool.impl;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.enums.NotificationEnums;
import ai.agent.util.groupChat.GroupChatMessageSender;

@ToolDeclare(
    name = "NotifyUserTool",
    cnName = "系统_通知用户",
    description = "通知用户显示一条Toast提示。调用后向前端发送OPERATE消息 operateName=SHOW_TOAST",
    scope = ToolScope.GROUP_CHAT
)
public class NotifyUserTool extends AbsGroupChatTool {
    
    @ParamDeclare(description = "要显示的通知消息内容", required = false, defaultValue = "操作已完成，可进行预览")
    private String message;
    
    @Override
    protected String executeInternal() {
        GroupChatMessageSender.Notice.toast(
            engine,
            currentAgentId != null ? currentAgentId : "system",
            NotificationEnums.INFORMATION,
            message
        );
        
        return "OK: 已通知前端显示Toast: " + message;
    }
}

