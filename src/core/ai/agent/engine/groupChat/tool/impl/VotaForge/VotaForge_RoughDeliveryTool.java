package ai.agent.engine.groupChat.tool.impl.VotaForge;

import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.ExecutionTraceDto;
import ai.agent.dto.groupChat.UserInfoDto;
import ai.agent.dto.groupChat.workCache.WorkCachePanelDesignDto;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.WorkCacheUtil;
import cell.ai.agent.IGroupChatNotifyService;
import cell.ai.agent.IGroupChatUserInfoService;
import cell.ai.agent.IVotaForgeService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.Form;

import java.util.List;
import java.util.Map;

import static ai.agent.enums.GCEngineWorkCacheKey.LAST_ROUGH_DELIVERY_GENERATED_PANEL_DESIGNS;

/**
 * VotaForge_原型交付
 */
public class VotaForge_RoughDeliveryTool implements Tool {


    @Override
    public String getName() {
        return "VotaForge_RoughDeliveryTool";
    }

    @Override
    public String getCnName() {
        return "VotaForge_原型交付";
    }

    @Override
    public String getDescription() {
        return "基于VotaForge规则进行原型交付（原毛坯交付），无需参数。";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {

        GroupChatToolContextAdapter gcCtx = convertToGroupChatToolContext(ctx);
        GroupChatEngine chatEngine = gcCtx.getChatEngine();
        String groupChatInstId = gcCtx.getGroupInstanceId();

        try {

            gcCtx.addExecutionTrace(new ExecutionTraceDto().setCategory("下发").setOperation("原型交付任务"));


            List<Form> panelDesignForms = IVotaForgeService.get()
                    .publishSceneToDefaultApplication(groupChatInstId);


            List<WorkCachePanelDesignDto> workCachePanelDesignDtos =
                    WorkCacheUtil.convertPdsToWorkCachePds(panelDesignForms);

            gcCtx.addExecutionTrace(new ExecutionTraceDto().setCategory("状态").setOperation("执行成功"));


            // 将结果缓存到工作缓存里面
            chatEngine.putEngineWorkCache(
                    LAST_ROUGH_DELIVERY_GENERATED_PANEL_DESIGNS,
                    workCachePanelDesignDtos
            );


            // 判断是否需要短信通知
            // 前面那个popDialogIfStartSmsNotify会让用户更新这个配置（如果用户点了确认）
            Boolean isStartNotifySms = chatEngine.getEngineConfig().isNotifySmsWhenLongTask();
            ConsolePrintUtil.printGreenLn(StrUtil.format("准备发送任务结束的短信通知, isStartNotifySms:{}", isStartNotifySms));

            if (isStartNotifySms) {
                // 发送短信通知
                UserInfoDto userInfoDto = IGroupChatUserInfoService.get()
                        .getCurrentUserInfo();
                if (userInfoDto != null && StrUtil.isNotBlank(userInfoDto.getPhone())) {
                    IGroupChatNotifyService.get()
                            .sendTaskOverSmsNotify(userInfoDto.getPhone());
                } else {
                    ConsolePrintUtil.printRedLn(
                            StrUtil.format("当前原型任务已结束，但发送短信失败，" +
                                    "可能是用户信息或手机号为空:\n{}", JSONUtil.toJsonStr(userInfoDto))
                    );
                }


            }


        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }


        return RespondDto.newStrSuccess("原型发布成功！", null);

    }


}

