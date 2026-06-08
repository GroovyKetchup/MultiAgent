package ai.agent.engine.groupChat.tool.impl.VotaForge;

import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.enums.NotificationEnums;
import ai.agent.service.workflow.DefaultWorkflowBootstrap;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cell.ai.agent.IVotaForgeService;
import cn.hutool.core.collection.CollUtil;
import gpf.adur.data.Form;
import octo.cm.util.PanelCategoryUtil;

import java.util.List;

@ToolDeclare(
        name = "VotaForge_CooperativeTaskComposerTool",
        cnName = "VotaForge_协作任务编制",
        description = "",
        scope = ToolScope.GROUP_CHAT
)
public class VotaForge_CooperativeTaskComposerTool extends AbsGroupChatTool {

    @Override
    public String getDescription() {
        return "编制业务系统初始化的任务列表，任务涵盖了预制的任务列表及人员分配。";
    }

    @Override
    protected String executeInternal() {

        try {

            // 如果当前已经有面板设计了，就不允许执行这个工具了
            List<Form> panelDesigns = IVotaForgeService.get()
                    .queryAllPanelDesign(getBusDomainCode());

            if (CollUtil.isNotEmpty(panelDesigns)) {

                boolean existNonSystemModule = false;
                for (Form panelDesignForm : panelDesigns) {
                    if (!PanelCategoryUtil.isSystemModuleCategory(panelDesignForm)) {
                        existNonSystemModule = true;
                        break;
                    }
                }

                if(existNonSystemModule){
                    return "系统早已初始化，请勿重复编制任务，如果想要更新业务文档请到意图确认进行更新。";
                }


            }

        } catch (Exception ignored) {
        }


        List<TaskBoardItem> created = DefaultWorkflowBootstrap.generateInitialTasks(engine);
        GroupChatMessageSender.Notice.toast(engine, currentAgentId, NotificationEnums.INFORMATION, "计划编制成功，请查看待办任务");
        return "OK: 任务已编制成功, 共" + created.size() + "个";
    }
}

