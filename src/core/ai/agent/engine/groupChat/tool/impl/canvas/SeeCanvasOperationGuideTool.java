package ai.agent.engine.groupChat.tool.impl.canvas;

import ai.agent.dto.RespondDto;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.service.frontendCalling.FrontendCanvasManager;

import java.util.Map;

/**
 * 查看画布（Canvas）操作手册
 */
public class SeeCanvasOperationGuideTool implements Tool {

    @Override
    public String getName() {
        return "SeeCanvasOperationGuideTool";
    }

    @Override
    public String getCnName() {
        return "查看画布操作手册";
    }

    @Override
    public String getDescription() {
        return "查看画布（Canvas）操作手册, 当不确定如何使用Canvas的时候进行查看。无需参数。\n";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        GroupChatToolContextAdapter gcCtx = convertToGroupChatToolContext(ctx);

        FrontendCanvasManager canvasActionManager = gcCtx.getChatEngine()
                .getFrontendActionManager();

        String operationGuidance = canvasActionManager.getOperationGuidance();

        return RespondDto.newStrSuccess("获取操作手册成功",
                operationGuidance
        );
    }


}

