package ai.agent.engine.groupChat.tool.impl;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;

@ToolDeclare(
    name = "WaitTool",
    cnName = "等待",
    description = "在需要等待的场景中使用，例如等待画布加载完成、等待某个请求响应等。参数milliseconds为等待时间，单位为毫秒",
    scope = ToolScope.GROUP_CHAT
)
public class WaitTool extends AbsGroupChatTool {

    @ParamDeclare(description = "等待时间，单位为毫秒", type = "number", required = true)
    private long milliseconds;

    @Override
    protected String executeInternal() {
        if (milliseconds <= 0) {
            return "Error: milliseconds 必须大于0";
        }

        if (milliseconds > 30_000) {
            milliseconds = 30_000;
        }

        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "等待被中断";
        }

        return "OK: 已等待 " + milliseconds + " 毫秒";
    }
}
