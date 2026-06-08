package ai.agent.engine.groupChat.tool.impl.canvas;

import ai.agent.dto.RespondDto;
import ai.agent.dto.frontendCalling.FrontendActionDto;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.service.frontendCalling.FrontendCanvasManager;
import cn.hutool.json.JSONUtil;
import octo.cm.util.EasyOperation;

import java.util.List;
import java.util.Map;

/**
 * 系统_设置应用名称
 */
public class ListCanvasActionTool implements Tool {

    public static final EasyOperation Op = EasyOperation.get();

    @Override
    public String getName() {
        return "ListCanvasActionTool";
    }

    @Override
    public String getCnName() {
        return "查看画布中可以调用的动作";
    }

    @Override
    public String getDescription() {
        return "如果用户激活了画布（大概率是需要你帮他在画布中做某种事情），你可以查看这个画布注册了那些动作(Action)可以用来调用。";
    }

    // 结果模板
    public static final String RESULT_TEMPLATE = "## 动作列表\n{}\n\n";

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        GroupChatToolContextAdapter gcCtx = convertToGroupChatToolContext(ctx);

        FrontendCanvasManager canvasActionManager = gcCtx.getChatEngine()
                .getFrontendActionManager();

        String operationGuidance = canvasActionManager.getOperationGuidance();
        List<FrontendActionDto> actions = canvasActionManager.listAction();

        if (Op.isEmpty(actions)) return RespondDto.newStrError("获取动作成功，但似乎目前没有注册任何动作");

        return RespondDto.newStrSuccess("获取动作成功",
                JSONUtil.toJsonStr(actions)
        );


    }

}

