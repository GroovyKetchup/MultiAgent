package ai.agent.engine.groupChat.tool.impl.canvas;

import ai.agent.annotation.ToolParameter;
import ai.agent.dto.RespondDto;
import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.service.frontendCalling.FrontendCanvasManager;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.service.llmCalling.LLMClient;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 询问画布（Canvas）操作手册
 */
public class AskCanvasOperationGuideTool implements Tool {

    @ToolParameter(description = "要询问的问题，尽可能详细地描述需要了解的内容", required = true)
    public static final String PARAM_QUESTION = "question";

    @Override
    public String getName() {
        return "AskCanvasOperationGuideTool";
    }

    @Override
    public String getCnName() {
        return "询问画布操作手册";
    }

    @Override
    public String getDescription() {
        return "询问画布（Canvas）操作手册，通过AI回答关于Canvas操作的问题。\n" +
                "当需要了解如何使用Canvas的某个功能时，优先使用此工具提问，如果多次询问仍未解决问题则调用SeeCanvasOperationGuideTool工具查看。\n" +
                "此工具会基于操作手册内容，使用AI智能回答你的问题。\n" +
                "参数描述：\n" +
                "参数1：question，文本型，必填，要询问的问题，尽可能详细地描述需要了解的内容\n" +
                "调用示例：\n" +
                "{\"question\": \"如何在Canvas中添加一个新节点？\"}";
    }

    @Override
    public String execute(ToolContext ctx, Map<String, Object> params) {

        GroupChatToolContextAdapter gcCtx = convertToGroupChatToolContext(ctx);

        if (CollUtil.isEmpty(params)) {
            return RespondDto.newStrError("请传递下列参数:" + JSONUtil.toJsonStr(getParameterSchema()));
        }

        try {
            Object questionObj = params.get(PARAM_QUESTION);
            if (!(questionObj instanceof String) || StrUtil.isBlank((String) questionObj)) {
                return RespondDto.newStrError("参数错误: 请提供有效的问题(question)");
            }

            String question = (String) questionObj;

            GroupChatEngine chatEngine = gcCtx.getChatEngine();

            String currentAgentId = gcCtx.getCurrentAgentId();
            GroupChatInstance groupChatInstance = chatEngine.getGroupChatInstance();

            AgentInstance currentAgent = groupChatInstance.getAgent(currentAgentId);

            FrontendCanvasManager canvasManager = chatEngine.getFrontendActionManager();
            String operationGuidance = canvasManager.getOperationGuidance();

            if (StrUtil.isBlank(operationGuidance) ||
                    operationGuidance.equals(FrontendCanvasManager.DEFAULT_OPERATION_GUIDANCE)) {
                return RespondDto.newStrError("当前Canvas没有提供操作手册");
            }


            if (currentAgent == null) {
                return RespondDto.newStrError("无法获取当前智能体实例");
            }




            LLMClient llmClient = currentAgent.getLlmClient();
//            LLMConfig llmConfig = currentAgent.getEffectiveLlmConfig();
            LLMConfig llmConfig = LLMConfigManager
                    .getAutoLlmConfig(LLMConfigManager.AutoModelType.FAST);

            if (llmClient == null || llmConfig == null) {
                return RespondDto.newStrError("无法获取大模型配置");
            }

            List<LlmMessage> messages = new ArrayList<>();

            String systemPrompt = "你是一个Canvas操作助手，专门帮助用户理解和使用Canvas功能。\n" +
                    "请基于以下操作手册内容回答用户的问题，给出清晰、准确、实用的答案，必须言简意赅，保证内容在200字以内。\n" +
                    "如果操作手册中没有相关信息，请明确告知用户。\n\n" +
                    "操作手册内容：\n" +
                    operationGuidance;

            messages.add(new LlmMessage(LlmMessage.Role_System, systemPrompt));
            messages.add(new LlmMessage(LlmMessage.Role_User, question));

            String answer = llmClient.callChat(llmConfig, messages);

            return RespondDto.newStrSuccess("查询成功", answer);

        } catch (Exception e) {
            return RespondDto.newStrError("询问操作手册失败:" + ExceptionUtils.getFullStackTrace(e));
        }
    }
}
