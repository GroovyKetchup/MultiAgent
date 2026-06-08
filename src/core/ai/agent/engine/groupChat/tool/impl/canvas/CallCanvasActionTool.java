package ai.agent.engine.groupChat.tool.impl.canvas;

import ai.agent.annotation.ToolParameter;
import ai.agent.dto.RespondDto;
import ai.agent.dto.frontendCalling.FrontendActionDto;
import ai.agent.dto.groupChat.ExecutionTraceDto;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.service.frontendCalling.FrontendAsyncCallService;
import ai.agent.util.ConsolePrintUtil;
import cell.ai.agent.IVotaForgeService;
import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.dc.basic.form.define.ApplicationDefine;
import octo.cm.util.EasyOperation;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;

import java.util.Map;

/**
 * 调用画布(Canvas)注册的动作
 */
public class CallCanvasActionTool implements Tool {

    public static final EasyOperation Op = EasyOperation.get();

    @ToolParameter(description = "要调用的动作名称", required = true)
    public static final String PARAM_ACTION_NAME = "actionName";
    @ToolParameter(description = "动作所需的参数，JSON对象格式，具体参数结构取决于目标动作的定义", required = false)
    public static final String PARAM_ACTION_PARAMS = "actionParams";

    @Override
    public String getName() {
        return "CallCanvasActionTool";
    }

    @Override
    public String getCnName() {
        return "调用画布注册的动作";
    }

    @Override
    public String getDescription() {
        return "调用画布(Canvas)中注册的动作(Action)来辅助用户完成任务。\n" +
                "当用户激活了画布并需要在画布中执行特定操作时，可以使用此工具调用相应的动作。\n" +
                "参数描述：\n" +
                "参数1：actionName，文本型，必填，要调用的动作名称\n" +
                "参数2：actionParams，JSON对象，可选，动作所需的参数，具体结构取决于目标动作的定义\n" +
                "调用示例(仅为演示参数使用方法）：\n" +
                "{\"actionName\": \"addNode\", \"actionParams\": {\"key1\": \"value1\", \"key2\": \"value2\"}}";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {
        GroupChatToolContextAdapter gcCtx = convertToGroupChatToolContext(ctx);

        if (CollUtil.isEmpty(params))
            return RespondDto.newStrError("请传递下列参数:" + JSONUtil.toJsonStr(getParameterSchema()));
        try {
            Object actionNameObj = params.get(PARAM_ACTION_NAME);
            if (!(actionNameObj instanceof String) || StrUtil.isBlank((String) actionNameObj)) {
                return RespondDto.newStrError("参数错误: 请提供有效的动作名称(actionName)");
            }

            String actionName = (String) actionNameObj;

            Object actionParamsObj = params.get(PARAM_ACTION_PARAMS);

            ConsolePrintUtil.printYellowLn(
                    StrUtil.format(
                            "正在调用画布动作[{}], 参数类型为[{}], 内容[{}]",
                            actionName,
                            actionParamsObj == null ? "无" : actionParamsObj.getClass().getName(),
                            JSONUtil.toJsonStr(actionParamsObj)
                    )
            );

            Map<String, Object> actionParams = null;
            if (actionParamsObj instanceof Map) {
                actionParams = (Map<String, Object>) actionParamsObj;
            } else if (actionParamsObj instanceof String) {
                try {
                    actionParams = JSONUtil.toBean(actionParamsObj.toString(), Map.class);
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
                    StrUtil.format("[子会话画布] 检测到子会话环境，调用画布动作 - SubSessionId: {}, Action: {}", 
                        subSessionId, actionName)
                );
            }

            FrontendActionDto frontendActionDto = chatEngine
                    .getFrontendActionManager().getAction(actionName);
            if (frontendActionDto == null) {
                return RespondDto.newStrError("未找到名称为[" + actionName + "]的动作");
            }

            String actionAlias = frontendActionDto.getActionAlias();
            if (StrUtil.isBlank(actionAlias)) actionAlias = actionName;

            // 添加执行轨迹
            gcCtx.addExecutionTrace(
                    new ExecutionTraceDto()
                            .setCategory("调用")
                            .setOperation(actionAlias)
                            .setDescription(frontendActionDto.getActionDescription())
            );

            // 如果是子会话，使用消息路由方式调用
            if (isSubSession) {
//                String requestId = IdUtil.fastSimpleUUID();
//                GroupChatMessageSender.Canvas.callAction(
//                    chatEngine,
//                    requestId,
//                    actionName,
//                    actionParams,
//                    subSessionId
//                );
                Object result = FrontendAsyncCallService
                        .callAction(chatEngine, actionName, actionParams, -1, subSessionId);
                return RespondDto.newStrSuccess("调用动作成功（子会话画布）", result);
            } else {
                // 主会话使用原有的同步调用方式
                Object result = FrontendAsyncCallService
                        .callAction(chatEngine, actionName, actionParams, -1);
                return RespondDto.newStrSuccess("调用动作成功", result);
            }
        } catch (Exception e) {
            return RespondDto.newStrError("调用动作失败:" + ExceptionUtils.getFullStackTrace(e));
        }


    }


    private void doSetAppName(IDao dao, DomainDto busDomain, String groupInstanceId, String appName) throws Exception {

        IVotaForgeService VotaForgeService = IVotaForgeService.get();

        String formModelId = ApplicationDefine.FormModelId;
        Cnd queryCnd = Op.getBusDomainFilterCondition(new OctoDomainOpObserver(busDomain.getDomainCode(),
                busDomain.getDomainUuid()), formModelId);

        queryCnd.where().andEquals(
                Form.Code,
                VotaForgeService.buildApplicationName(groupInstanceId)
        );

        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, queryCnd, 1, 1, true, true);
        if (queryRs.isEmpty()) throw new RuntimeException("无法找到当前实例对应的应用");

        Form appForm = queryRs.getDataList().get(0);
        appForm.setAttrValue(ApplicationDefine.sLabel, appName);

        IFormMgr.get().updateForm(dao, appForm);


    }
}

