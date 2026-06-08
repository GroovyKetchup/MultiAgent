package ai.agent.engine.graph.node.canvas;

import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.dto.groupChat.AgentExperienceDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.TextPayload;
import ai.agent.dto.groupChat.textStyle.TodoItemDto;
import ai.agent.engine.graph.AbstractGraphNode;
import ai.agent.engine.graph.GraphContext;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.enums.GCEngineWorkCacheKey;
import ai.agent.enums.MessageType;
import ai.agent.enums.TextPayloadStyle;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.llmCalling.PromptBuilder;
import ai.agent.util.llmCalling.ToolSkipStrategy;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

import static ai.agent.constant.GraphConstants.NODE_EXECUTOR;
import static ai.agent.constant.GraphConstants.NODE_GENERAL_PLANING;

@Comment("通用操作指导节点")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-01", updateTime = "2025-12-01"
)
public class GeneralPlaningNode extends AbstractGraphNode {
    @Override
    public String getName() {
        return NODE_GENERAL_PLANING;
    }

    @Override
    public NodeExecutionResult execute(GraphContext ctx) {

        GroupChatEngine chatEngine = ctx.getChatEngine();
        AgentInstance agentInstance = ctx.getAgentInstance();
        Message triggerMessage = getTriggerMessage(ctx);
        if (chatEngine == null || agentInstance == null || triggerMessage == null) {
            return NodeExecutionResult.toNode(NODE_EXECUTOR)
                    .withReason("基础数据缺失，跳过规划");
        }


        try {

            // 曾经的经验
            AgentExperienceDto agentExperience = getAgentExperience(ctx);


            List<Tool> availableTools = agentInstance.getAvailableTools(ToolSkipStrategy.onNonCanvasMode);


            LLMConfig llmConfig = LLMConfigManager.getAutoLlmConfig(LLMConfigManager.AutoModelType.HIGH_PERFORMANCE);
            if (llmConfig == null) llmConfig = agentInstance.getEffectiveLlmConfig();

            String llmResponse = agentInstance.getLlmClient().callLlm(
                    llmConfig,
                    buildPlanPrompt(ctx,chatEngine, agentInstance, availableTools, triggerMessage, agentExperience)
            );

            ConsolePrintUtil.printGreenLn(StrUtil.format("编制的计划: {}", llmResponse));

            if (StrUtil.isNotBlank(llmResponse)) {
                setPlan(ctx, llmResponse);
                String agentId = agentInstance.getDefinition().getAgentId();
                chatEngine.submitAgentMessage(
                        new Message(agentId, MessageType.TEXT,
                                new TextPayload(llmResponse, TextPayloadStyle.THINKING.getValue()), null)
                );

            }

            return NodeExecutionResult.toNode(NODE_EXECUTOR)
                    .withReason("通用规划完成")
                    .withSummary("已生成执行计划");

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return NodeExecutionResult.error("通用规划节点执行异常: " + e.getMessage());
        }
    }

    private String buildPlanPrompt(GraphContext ctx, GroupChatEngine chatEngine, AgentInstance agentInstance, List<Tool> availableTools, Message triggerMessage, AgentExperienceDto agentExperience) {
        String todoListInfo = buildTodoListInfo(chatEngine);

        return StrUtil.format(
                "# 任务\n" +
                        "根据角色定义和可用工具，编制执行计划（30~200字）。\n\n" +
                        "# 上下文\n" +
                        "- 画布状态：未打开\n" +
                        "- 角色定义：{}\n" +
                        "- 可用工具：{}\n" +
                        "- 用户需求：{}\n" +
                        "- 当前已存在的待办事项:\n{}\n" +
                        "- 曾经的经验:{}\n" +
                        "- 你的角色提示词:{}\n" +
                        "- 你可以使用的工具:{}\n" +
                        "- 曾经的历史上下文:{}\n" +
                        "# 计划要求\n" +
                        "1. 如果察觉到用户的需求可能确实了信息，比如【继续】【再看一下】，可能是因为我们没有加载历史上下文的缘故，你需要在计划中显性的提醒要调用工具【GetChatHistoryTool】\n" +
                        "2. 尝试基于已知事实进行编制计划，确保计划可靠，不可胡编乱造。\n" +
                        "# 输出要求\n" +
                        "1. 明确列出要创建的待办事项\n" +
                        "2. 判断是否清空旧待办（与当前任务无关或已完成时清空）\n" +
                        "3. 后续节点优先使用Todo工具管理任务\n" +
                        "4. 若用户意图非操作，用10-30字简单总结用户意图\n" +
                        "5. 所有内容严格使用纯文本的方式，禁止回复JSON、Markdown、Emoji。\n"

                ,
                PromptBuilder.buildAgentDefinitionPrompt(agentInstance.getDefinition()),
                JSONUtil.toJsonStr(availableTools),
                triggerMessage.getTextPayload().getText(),
                todoListInfo,
                PromptBuilder.buildExperiencePrompt(agentExperience),
                PromptBuilder.buildAgentDefinitionPrompt(agentInstance.getDefinition()),
                agentInstance.getToolRegistry().getToolNames(),
                PromptBuilder.buildHistorySummaryContextPrompt(getHistorySummary(ctx))

        );
    }

    // 构建当前待办事项信息
    private String buildTodoListInfo(GroupChatEngine chatEngine) {
        List<TodoItemDto> todoList = chatEngine.getEngineWorkCache(GCEngineWorkCacheKey.TODO_LIST, List.class);
        if (CollUtil.isEmpty(todoList)) {
            return "当前待办事项：无";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("当前待办事项：\n");
        for (TodoItemDto item : todoList) {
            sb.append(String.format("  - [%d] [%s] %s\n", item.getIndex(), item.getStatus(), item.getItemName()));
        }
        return sb.toString();
    }
}
