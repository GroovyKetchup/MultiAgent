package ai.agent.engine.graph.node;

import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.llmCalling.LlmMessage;
import ai.agent.engine.graph.AbstractGraphNode;
import ai.agent.engine.graph.GraphContext;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.service.workflow.WorkflowTemplateRegistry;
import ai.agent.util.ConsolePrintUtil;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

import static ai.agent.constant.GraphConstants.*;

/**
 * 意图路由节点
 */
@Comment("意图路由节点")
@ClassDeclare(
        label = "Router",
        what = "判断任务复杂度并检测工作流模板", why = "区分快慢车道+非侵入式工作流", how = "LLM分类+WorkflowTemplateRegistry",
        developer = "裴硕", version = "3.0",
        createTime = "2025-12-04", updateTime = "2025-12-14"
)
public class RouterNode extends AbstractGraphNode {

    @Override
    public String getName() {
        return NODE_ROUTER;
    }

    @Override
    public NodeExecutionResult execute(GraphContext ctx) throws Exception {



        GroupChatEngine chatEngine = ctx.getChatEngine();

        AgentInstance agentInstance = ctx.getAgentInstance();
        Message triggerMessage = getTriggerMessage(ctx);

        if (chatEngine == null || agentInstance == null || triggerMessage == null)
            throw new RuntimeException("引擎、智能体实例以及触发消息不得为空");

        boolean isCanvasOpen = chatEngine.getFrontendActionManager().isCanvasOpen(chatEngine);

        String targetPlaningNode = isCanvasOpen ? NODE_CANVAS_PLANING : NODE_GENERAL_PLANING;
        String targetDirectExecutionNode = NODE_EXECUTOR;

        if(true){
            return NodeExecutionResult.toNode(targetPlaningNode)
                    .withReason("复杂任务需要规划");
        }

        WorkflowTemplateRegistry workflowRegistry = WorkflowTemplateRegistry.getInstance();
        if (workflowRegistry.tryApplyWorkflowTemplate(ctx, triggerMessage)) {
            ConsolePrintUtil.printGreenLn(">> [工作流] 已应用工作流模板，直接进入执行节点");
            return NodeExecutionResult.toNode(NODE_EXECUTOR)
                    .withReason("工作流模板匹配")
                    .withSummary("已应用工作流模板");
        }

        try {
            ConsolePrintUtil.printGreenLn("[RouterNode] 开始执行路由判定");

            List<LlmMessage> recentHistory = getWindowMessage(ctx);
            ConsolePrintUtil.printGreenLn(StrUtil.format("[RouterNode] 历史消息数量: {}", recentHistory != null ? recentHistory.size() : 0));

            List<LlmMessage> messages = buildRouterPrompt(triggerMessage, recentHistory);
            ConsolePrintUtil.printGreenLn(StrUtil.format("[RouterNode] 构建的消息数量: {}", messages.size()));

            ConsolePrintUtil.printGreenLn("[RouterNode] 准备调用LLM...");

            LLMConfig llmConfig = LLMConfigManager.getAutoLlmConfig(LLMConfigManager.AutoModelType.FAST);
            if (llmConfig == null) llmConfig = agentInstance.getEffectiveLlmConfig();

            String llmResponse = agentInstance.getLlmClient().callChat(
                    llmConfig,
                    messages
            );
            ConsolePrintUtil.printGreenLn("[RouterNode] LLM调用完成");

            ConsolePrintUtil.printGreenLn(StrUtil.format("路由判定结果: {}", llmResponse));

            if (StrUtil.isBlank(llmResponse)) {
                if (ctx.getNodeRetryCount(getName()) < 2) {
                    return NodeExecutionResult.retry("LLM返回为空")
                            .withMaxRetry(2);
                }
                return NodeExecutionResult.toNode(targetPlaningNode)
                        .withReason("LLM返回空，默认进入规划模式");
            }

            if (StrUtil.containsIgnoreCase(llmResponse, "[PLANNING]")) {
                ConsolePrintUtil.printGreenLn(">> 进入 [复杂/规划] 模式");
                return NodeExecutionResult.toNode(targetPlaningNode)
                        .withReason("复杂任务需要规划");
            } else {
                ConsolePrintUtil.printGreenLn(">> 进入 [简单/直通] 模式");
                return NodeExecutionResult.toNode(targetDirectExecutionNode)
                        .withReason("简单任务直接执行");
            }

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("路由节点执行异常: {}", ExceptionUtils.getFullStackTrace(e)));
//            return NodeExecutionResult.error("路由节点执行异常: " + e.getMessage());

            return NodeExecutionResult.toNode(targetPlaningNode)
                    .withReason("LLM返回空，默认进入规划模式");
        }
    }

    // 构建路由专属的 Prompt
    private List<LlmMessage> buildRouterPrompt(Message triggerMessage, List<LlmMessage> history) {

        String userGoal = triggerMessage.getTextPayload().getText();

        String systemPrompt = StrUtil.format(
                "# Role\n" +
                        "你是一个任务复杂度判别器。请分析用户的最新指令，将其分类。\n\n" +
                        "# Classification Rules\n" +
                        "1. [DIRECT] - 简单/直通模式\n" +
                        "   - 修改单一属性（如“把颜色改成红色”、“字号调大”）\n" +
                        "   - 纯闲聊或问答（如“这是什么组件？”）\n" +
                        "   - 明确且简单的原子操作\n\n" +
                        "2. [PLANNING] - 复杂/规划模式\n" +
                        "   - 涉及多组件联动或布局调整\n" +
                        "   - 需要多步操作才能完成的任务\n" +
                        "   - 模糊的需求（如“帮我优化一下界面”、“设计一个订单页”）\n" +
                        "   - 涉及数据绑定、逻辑编排\n\n" +
                        "# Output Constraint\n" +
                        "仅输出标签 [DIRECT] 或 [PLANNING]，不要输出任何解释或多余字符。\n\n" +
                        "# Current Task\n" +
                        "{}"
                , userGoal
        );

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage(LlmMessage.Role_System, systemPrompt));

        if (history != null) {
            messages.addAll(history);
        }

        return messages;
    }
}