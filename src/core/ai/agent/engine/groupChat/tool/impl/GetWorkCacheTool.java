package ai.agent.engine.groupChat.tool.impl;

import ai.agent.annotation.ToolDeclare;
import ai.agent.annotation.ToolScope;
import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.workCache.WorkCachePanelDesignDto;
import ai.agent.engine.groupChat.tool.AbsGroupChatTool;
import ai.agent.enums.GCEngineWorkCacheKey;
import ai.agent.util.groupChat.WorkCacheUtil;
import cell.ai.agent.IVotaForgeService;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.Form;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ToolDeclare(
        name = "GetWorkCacheTool",
        cnName = "获取系统中的工作缓存",
        description = "当智能体进行协作的时候，每个智能体会将自己的产出物或认为别人会需要的数据，存储到工作缓存(Work Cache)中。正例：当你需要与其他同伴协同的时候或需要查看其他智能体的产出物的时候，使用本工具。反例：当你的上下文中已经涵盖了你所需要的信息，不允许使用本工具",
        scope = ToolScope.GROUP_CHAT
)
public class GetWorkCacheTool extends AbsGroupChatTool {

    public static final boolean ALWAYS_NEWEST_PANEL_DESIGNS = true;

    @Override
    protected String executeInternal() {
        HashMap<GCEngineWorkCacheKey, Object> caches = engine.getAllEngineWorkCache();

        if (ALWAYS_NEWEST_PANEL_DESIGNS) {
            List<Form> panelDesigns = null;
            try {
                panelDesigns = IVotaForgeService.get().queryAllPanelDesign(getBusDomainCode());
                List<WorkCachePanelDesignDto> oriDtos = WorkCacheUtil.convertPdsToWorkCachePds(panelDesigns);
                List<WorkCachePanelDesignDto> newDtos = new ArrayList<>();
                for (WorkCachePanelDesignDto workCachePanelDesignDto : oriDtos) {
                    newDtos.add(new WorkCachePanelDesignDto()
                            .setPanelCode(workCachePanelDesignDto.getPanelCode())
                            .setPanelName(workCachePanelDesignDto.getPanelName())
                            .setPanelCategory(workCachePanelDesignDto.panelCategory())
                    );
                }
                caches.put(GCEngineWorkCacheKey.LAST_ROUGH_DELIVERY_GENERATED_PANEL_DESIGNS,
                        newDtos
                );
            } catch (Exception e) {
                return RespondDto.newStrError(ExceptionUtils.getFullStackTrace(e));

            }
        }

        return RespondDto.newStrSuccess("获取工作缓存成功",
                JSONUtil.toJsonStr(caches)
        );
    }
}

