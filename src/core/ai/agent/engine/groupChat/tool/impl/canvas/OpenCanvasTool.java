package ai.agent.engine.groupChat.tool.impl.canvas;

import ai.agent.annotation.ToolParameter;
import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.canvas.CanvasStatus;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.engine.groupChat.tool.impl.canvas.descriptor.CanvasTypeRegistry;
import ai.agent.enums.GCEngineWorkCacheKey;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.Map;

/**
 * 调用画布(Canvas)注册的动作
 */
public class OpenCanvasTool implements Tool {

    @ToolParameter(description = "要打开的画布类型", required = true)
    private static final String PARAM_CANVAS_TYPE = "canvasType";
    @ToolParameter(description = "画布所需的参数，JSON对象格式，具体参数结构取决于目标画布的定义", required = false)
    private static final String PARAM_CANVAS_TYPE_PARAMS = "canvasParams";

    @Override
    public String getName() {
        return "OpenCanvasTool";
    }

    @Override
    public String getCnName() {
        return "打开画布";
    }

    @Override
    public String getDescription() {
        return CanvasTypeRegistry.generateToolDescription();
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        GroupChatToolContextAdapter gcCtx = convertToGroupChatToolContext(ctx);

        if (CollUtil.isEmpty(params))
            return RespondDto.newStrError("请传递下列参数:" + JSONUtil.toJsonStr(getParameterSchema()));
        try {
            Object canvasTypeObj = params.get(PARAM_CANVAS_TYPE);
            if (!(canvasTypeObj instanceof String) || StrUtil.isBlank((String) canvasTypeObj)) {
                return RespondDto.newStrError("参数错误: 请提供有效的画布类型(canvasType)");
            }

            String canvasType = (String) canvasTypeObj;

            Object canvasParamsObj = params.get(PARAM_CANVAS_TYPE_PARAMS);

            ConsolePrintUtil.printYellowLn(
                    StrUtil.format(
                            "正在打开画布[{}], 参数类型为[{}], 内容[{}]",
                            canvasType,
                            canvasParamsObj == null ? "无" : canvasParamsObj.getClass().getName(),
                            JSONUtil.toJsonStr(canvasParamsObj)
                    )
            );


            Map<String, Object> canvasParams = null;
            if (canvasParamsObj instanceof Map) {
                canvasParams = (Map<String, Object>) canvasParamsObj;
            } else if (canvasParamsObj instanceof String) {
                try {
                    canvasParams = JSONUtil.toBean(canvasParamsObj.toString(), Map.class);
                } catch (Exception ignored) {
                }
            }

            GroupChatEngine chatEngine = gcCtx.getChatEngine();

            // 检测是否在子会话环境中
            boolean isSubSession = chatEngine.getGroupChatInstance() instanceof SubSessionInstance;
            String subSessionId = null;

            if (isSubSession) {
                SubSessionInstance subSession = (SubSessionInstance) chatEngine.getGroupChatInstance();
                subSessionId = subSession.getInstanceId();
                ConsolePrintUtil.printGreenLn(
                        StrUtil.format("[子会话画布] 检测到子会话环境，SubSessionId: {}", subSessionId)
                );
            }

            GroupChatMessageSender.Canvas.open(
                    chatEngine,
                    gcCtx.getCurrentAgentId(),
                    canvasType,
                    canvasParams,
                    subSessionId
            );

            // 保存到缓存
            CanvasStatus canvasStatus = new CanvasStatus()
                    .setCanvasType(canvasType)
                    .setCanvasParams(canvasParams)
                    .setCanvasOpener(gcCtx.getCurrentAgentId())
                    .setSubSessionId(subSessionId);

            chatEngine.putEngineWorkCache(
                    GCEngineWorkCacheKey.CURRENT_CANVAS_STATUS,
                    canvasStatus
            );

            // 如果是子会话，同时保存到SubSessionInstance
            if (isSubSession) {
                SubSessionInstance subSession = (SubSessionInstance) chatEngine.getGroupChatInstance();
                subSession.setCanvasStatus(canvasStatus);
            }

            return RespondDto.newStrSuccess("打开画布成功，画布的加载需要一些时间" +
                    "（使用等待动作, 建议等待5秒钟，然后检查状态，如果未能满足，再继续等待，超过60秒仍不正常，直接结束，告知用户异常），" +
                    "请勿重复调用本工具。", null);
        } catch (Exception e) {
            return RespondDto.newStrError("打开画布失败:" + ExceptionUtils.getFullStackTrace(e));
        }


    }


}

