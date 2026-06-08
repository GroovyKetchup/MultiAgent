package ai.agent.engine.groupChat.tool.impl.VotaForge;

import ai.agent.annotation.ToolParameter;
import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.workCache.WorkCachePanelDesignDto;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.util.ConsolePrintUtil;
import cell.ai.agent.IVotaForgeService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.Form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * VotaForge_个性交付
 */
public class VotaForge_FineDecorationTool implements Tool {

    @ToolParameter(description = "面板代码", required = true)
    public static final String PARAM_KEY_PANEL_CODES = "panelCode";
    
    @ToolParameter(description = "用户需求描述", required = false)
    public static final String PARAM_KEY_USER_NEED = "userNeed";

    @Override
    public String getName() {
        return "VotaForge_FineDecorationTool";
    }

    @Override
    public String getCnName() {
        return "VotaForge_个性交付";
    }

    @Override
    public String getDescription() {
        return "个性交付本意是对一个或多个已发布的面板使用AI优化，然后发布到应用。\n" +
                "因此当用户想要对面板使用AI优化的时候，或是调整已存在的面板，都可以使用这个工具。\n" +
                "同时，个性交付可以为已有的面板提供以下功能：1、基于AI生成更漂亮、更快速、更用户友好的界面；\n" +
                "参数描述：\n" +
                "参数1: panelCodes, 文本型，你可以传递一个或多个面板编号，如果是多个则由英文逗号分割拼接。\n" +
                "单独参数实例代表对面板编号分别为123的面板进行个性交付: 123\n" +
                "多个参数示例(代表同时对面板编号分别为123、234、678的三个面板进行个性交付): 123,234,678\n" +
                "\n补充说明：\n" +
                "用户可能只会提供给你某一个面板的名称（当然，用户大概率会携带错别字、省略字，你需要根据语义判断！）" +
                "因此，调用这个工具前，务必获取准确的面板编号，" +
                "因此，必须要在此之前调用工具GetWorkCacheTool获得工作缓存内容，从中查看之前原型智能体交付的面板设计列表，" +
                "其中原型智能体交付的面板设计列表的缓存名称为LAST_ROUGH_DELIVERY_GENERATED_PANEL_DESIGN。" +
                "参数2: userNeed, 文本型，将用户的原始需求传递到这个地方，然后告诉工具用户想要对这些面板做什么。\n"

                ;
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {

        GroupChatToolContextAdapter gcCtx = convertToGroupChatToolContext(ctx);
        GroupChatEngine chatEngine = gcCtx.getChatEngine();
        String groupChatInstId = gcCtx.getGroupInstanceId();


        Object panelCodesObj = params.get(PARAM_KEY_PANEL_CODES);
        Object userNeedObj = params.get(PARAM_KEY_USER_NEED);
        if (!(panelCodesObj instanceof String) || StrUtil.isBlank((String) panelCodesObj)) {
            return RespondDto.newStrError(
                    StrUtil.format("参数错误，或不存在文本型变量<{}>", PARAM_KEY_PANEL_CODES)
            );
        }


        String[] panelCodes = ((String) panelCodesObj).split(",");
        String userNeed = "";
        if (userNeedObj instanceof String) userNeed = (String) userNeedObj;

        try {
            List<Form> llmInitedPanelDesigns = IVotaForgeService.get().llmInitAndPublishToDefaultApplication(
                    groupChatInstId,
                    Arrays.asList(panelCodes),
                    userNeed
            );

            List<WorkCachePanelDesignDto> dtos = new ArrayList<>();
            if (CollUtil.isNotEmpty(llmInitedPanelDesigns)) {
                for (Form llmInitedPanelDesign : llmInitedPanelDesigns) {
                    WorkCachePanelDesignDto dto = WorkCachePanelDesignDto
                            .newDto(llmInitedPanelDesign);
                    if (dto != null) {
                        dtos.add(dto);
                    }
                }

                ConsolePrintUtil.printGreenLn(
                        StrUtil.format("基于大模型进行初始化出来的面板设计:{}",
                                JSONUtil.toJsonStr(dtos))
                );

            }


            return RespondDto.newStrSuccess("标准发布成功！", null);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


}

