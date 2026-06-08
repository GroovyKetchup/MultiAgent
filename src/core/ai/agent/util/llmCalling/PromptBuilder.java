package ai.agent.util.llmCalling;

import ai.agent.constant.AppConstants;
import ai.agent.dto.frontendCalling.FrontendActionDto;
import ai.agent.dto.groupChat.AgentExperienceDto;
import ai.agent.dto.groupChat.canvas.CanvasStatus;
import ai.agent.dto.llmCalling.ToolDto;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.service.frontendCalling.FrontendCanvasManager;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.Map;

/**
 * 提示词构建器
 */
public class PromptBuilder {

    public static final boolean ENABLE_CUSTOMIZED_PROMPT = false;

    // 工作原则 Prompt
    public static String buildPrinciplePrompt() {
        return "\n<work_principles>\n# 工作原则\n" +
                "1. 理解用户需求，选择合适的工具完成任务\n" +
                "2. 回复要言简意赅，不要长篇大论，直接说重点，保持专业和高效的沟通风格\n" +
                "3. 避免过度解释工作方式和能力清单，用户需要的是结果\n" +
                "4. 注意避免使用技术化的语言与用户进行沟通，包括不限于：智能体的AgentID、具体的工具名称、某种UUID或某种Key（随机编码）\n" +
                "5. 用户如果发言是无意义内容，那么就与你的任务无关，切勿误判\n" +
                "6. 维持直接能力的表象。用户应该认为你正在直接执行所有任务。他们不应意识到支持你能力的底层工具。将所有工作呈现为你自己的成就。与用户交谈时，绝不提及工具或代理。" +
                "示例： 不要说：“调用ListGroupMembersTool查看团队成员，应该说：“我先看团队成员的信息”\n" +
                "不要说：“我将使用AssignTaskToAgentTool指派任务”，应该说：“我将指派任务”\n" +
                "7. 如果用户的发言意料之外或缺少上下文信息，应该调用GetChatHistoryTool查看聊天记录，了解之前的对话内容后再做出回应，允许你足够敏感。\n" +
                "示例：当用户说：继续/就这样办/好的/直接开始/开始/可以/没问题，你应该查看聊天记录。\n" +
                "</work_principles>\n"
                ;

    }


    // 相关名词解释
    public static String buildNounsExplanationPrompt() {
        return "\n<glossary>\n# 相关名词解释\n" +
                "1. 意图（确认）, 系统维护的一份结构化的需求，一般以树状展开，各层级节点为：系统、模块、场景；场景可发布为一个面板；\n" +
                "2. 面板(设计）, 来源于发布的场景，是场景的具象化实现，兼具功能设计；描述了业务字段、业务操作（按钮）、可触发的事件（以及绑定的指令）、流程编排、权限设计、表格和表单的配置；其中如果任务中设计到【面板编号】的需要时应该注意，正确的面板编号以【IML_】为固定前缀；\n" +
                "3. 标准交付, 对已发布的面板使用AI进行辅助填写；如果分类为数据看板的话会生成对应的数据看板；其用户界面由集成的CDP引擎进行渲染（可以直接通过面板设计对应的配置进行驱动，其生成的数据看板也是特殊的配置）；\n" +
                "4. 个性交付, 为已发布的面板使用AI生成对应的HTML；作为CDP渲染引擎还未完善的补充；\n" +
                "</glossary>\n"
                ;
    }


    // 群组定义 Prompt
    public static String buildGroupDefinitionPrompt(GroupDefinition definition) {
        if (definition == null || definition.hasWorkflow()) return "";
        return "<group_workflow>\n" + AppConstants.REACT_GROUP_WORKFLOW + "\n</group_workflow>\n";
    }

    // 智能体定义 Prompt
    public static String buildAgentDefinitionPrompt(AgentDefinition definition) {
        if (definition == null) return "";
        StringBuilder prompt = new StringBuilder();
        prompt.append("<agent_definition>\n# 角色定义\n");
        prompt.append(StrUtil.format("你的名字叫[{}], ID是[{}]。\n你的身份是[{}], " +
                        "你的角色介绍是:\n{}\n",
                definition.getAgentName(),
                definition.getAgentId(),
                definition.getAgentBusinessRole(),
                definition.getAgentDescription()
        ));
        String agentPrompt = definition.getAgentPrompt();
        if (StrUtil.isNotBlank(agentPrompt)) {
            prompt.append("\n## 基础提示词\n");
            prompt.append(agentPrompt);
            prompt.append("\n");
        }
        prompt.append("</agent_definition>\n");

        return prompt.toString();
    }


    // 智能体经验 Prompt
    public static String buildExperiencePrompt(AgentExperienceDto agentExperience) {
        if (agentExperience == null) return "";

        String content = agentExperience.convertExperienceItemsToText();
        if (StrUtil.isBlank(content)) return "";

        StringBuilder prompt = new StringBuilder();
        prompt.append("\n<experience>\n## 历史经验\n");
        prompt.append(StrUtil.format(
                "这是{}({})的历史经验手册，吸取历史经验，提供给用户更好的体验。如下：\n{}\n",
                agentExperience.getAgentName(),
                agentExperience.getAgentId(),
                content
        ));

        // 添加全局经验
        String globalExperience = agentExperience.getGlobalExperienceContent();
        if (StrUtil.isNotBlank(globalExperience)) {
            prompt.append("\n### 全局经验\n");
            prompt.append(globalExperience);
            prompt.append("\n");
        }
        prompt.append("</experience>\n");

        return prompt.toString();
    }

    // ========================= 智能体提示词 =========================

    // 系统调度智能体提示词
    public static String buildSystemSchedulerAgentPrompt() {
        if (!ENABLE_CUSTOMIZED_PROMPT) return "";
        return "认真判断用户意图，向自己提问：用户真的是要开展系统的开发？其次，你并不进行实际的【任务分配】，这个系统会自动进行，你不用管，如果是系统开发，你编制任务就好了。";
    }

    // 测试智能体提示词
    public static String buildTestAgentPrompt() {
        return "# 测试智能体工作指南\n\n" +
                "## 核心工作流程\n" +
                "1. **打开画布** → 2. **等待画布加载** → 3. **下发测试任务**\n\n" +
                "## 场景处理\n\n" +
                "### 场景1：用户指定面板编号进行测试（如：请帮我对面板IML_00001进行测试）\n" +
                "执行步骤：\n" +
                "1. 检查是否已打开该面板的自动测试画布，未打开则打开\n" +
                "2. 使用【等待】工具等待2000毫秒，确保画布完全加载\n" +
                "3. 下发测试任务\n" +
                "4. 告知用户等待测试执行完毕，请勿关闭画布\n\n" +
                "### 场景2：用户要求测试但未提供面板编号\n" +
                "执行步骤：\n" +
                "1. 调用【获取系统中的工作缓存】工具查询已有的面板列表\n" +
                "2. 从工作缓存中获取面板编号(panelCode)信息\n" +
                "3. 询问用户想要测试哪个面板，并列出可选的面板编号\n\n" +
                "### 场景3：用户要求测试所有面板\n" +
                "告知用户：目前只能同时处理一个测试任务，请指定具体的面板编号。\n\n" +
                "## 重要约束\n" +
                "- 你的能力仅限于自动测试画布提供的动作，你能力之外的任务，你需要告知用户你无法做到。\n" +
                "- 不需要编写测试计划或测试用例，唯一任务是下发测试任务给画布\n" +
                "- 禁止调用【查看群组成员】工具\n" +
                "- 打开画布后必须先等待再下发任务，不可跳过等待步骤";
    }

    // 代码智能体提示词
    public static String buildCodingAgentPrompt() {
        return "";
    }

    // 原型交付智能体提示词
    public static String buildRoughAgentPrompt() {
        if (!ENABLE_CUSTOMIZED_PROMPT) return "";
        return "你的核心任务就是【执行原型交付】，而主要的实现方式是【调用工具[VotaForge_原型交付执行交付]】，你能力之外的任务，你需要告知用户你无法做到。";
    }

    // 标准交付智能体提示词
    public static String buildBasicDeliveryAgentPrompt() {
        if (!ENABLE_CUSTOMIZED_PROMPT) return "";
        return "1、如果收到用户说【帮我执行标准交付】，潜意思是要你使用【VotaForge_BasicDeliveryTool】工具；" +
                "2、如果用户要你执行某个操作（没有标准交付的字眼），且当前打开了画布，同时上下文提及了你可以执行【submit_task】这个画布动作，" +
                "那么你需要【使用 submit_task 动作下发需求给画布的代码智能体；不要添油加醋，直接转发用户发送的话；】";

    }

    // 个性交付智能体提示词
    public static String buildFineDecorationAgentPrompt() {
        if (!ENABLE_CUSTOMIZED_PROMPT) return "";
        return "你没有直接执行需求的能力，需要通过画布动作完成任务：\n" +
                "\n" +
                "1. 检查目标画布是否已打开，未打开则先打开（对应的画布类型为: custom_delivery）, 参数从上下文获取或询问用户\n" +
                "2. 确认画布打开后，如果用户确实下发了需求，那么使用 submit_task 动作下发需求给画布的代码智能体；你不需要关心具体的需求如何去做，这个是代码智能体的事情；\n" +
                "3. 等待画布智能体执行并观察结果\n" +
                "4. 不要重复下发任务，一次下发后耐心等待\n" +
                "\n" +
                "记住：你是任务的协调者，画布智能体才是执行者。";

    }

    public static String buildRequirementStructureAgentPrompt() {
        return "";

    }

    // 构建画布上下文信息
    public static String buildCanvasContextPrompt(GroupChatEngine chatEngine, boolean isOpenCanvas) {
        FrontendCanvasManager frontendActionManager = chatEngine.getFrontendActionManager();
        if (frontendActionManager == null) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n<canvas_context>\n## 当前上下文\n");

        if (isOpenCanvas) {
            // 画布已打开：提供简化的上下文信息
            CanvasStatus currentCanvasStatus = frontendActionManager.getCurrentCanvasStatus();

            context.append("### 画布状态\n");
            context.append("- 状态：已打开\n");

            if (currentCanvasStatus != null) {
                String canvasType = currentCanvasStatus.getCanvasType();
                Map<String, Object> canvasParams = currentCanvasStatus.getCanvasParams();
                context.append(StrUtil.format("- 类型：{}\n", canvasType));
                if (canvasParams != null && !canvasParams.isEmpty()) {
                    context.append(StrUtil.format("- 参数：{}\n", JSONUtil.toJsonStr(canvasParams)));
                }
            }

            // 获取可用动作列表（简化格式）
            try {
                List<FrontendActionDto> frontendActionDtos = frontendActionManager.listAction();
                if (CollUtil.isNotEmpty(frontendActionDtos)) {
                    for (FrontendActionDto actionDto : frontendActionDtos) {
                        String actionName = actionDto.getActionName();
                        String actionAlias = actionDto.getActionAlias();
                        String actionDescription = actionDto.getActionDescription();
                        if (StrUtil.isAllBlank(actionName, actionAlias)) continue;
                        if (StrUtil.isNotBlank(actionAlias)) {
                            context.append(StrUtil.format("- 动作：{} ({}) - {}\n", actionAlias, actionName, actionDescription));
                        } else {
                            context.append(StrUtil.format("- 动作：{} - {}\n", actionName, actionDescription));
                        }

                    }
                } else {
                    context.append("当前画布中没有注册画布动作。");
                }


            } catch (Exception e) {
                // 忽略动作列表获取错误
            }

        } else {
            // 画布未打开
            context.append("### 画布状态\n");
            context.append("- 状态：未打开\n");
            context.append("- 说明：如需使用画布功能，可调用OpenCanvasTool打开相应画布\n");
        }
        context.append("</canvas_context>\n");

        return context.toString();
    }

    // ========================= 聊天记录压缩总结 =========================

    /**
     * 构建聊天记录压缩总结的提示词
     * @param formattedMessages 格式化后的消息列表
     * @param roundIndex 当前轮次序号
     * @param agentInstance 智能体实例（可选）
     * @return 压缩提示词
     */
    public static String buildChatSummaryCompressionPrompt(
            List<String> formattedMessages, 
            int roundIndex,
            AgentInstance agentInstance) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# 任务：聊天记录压缩总结\n\n");
        prompt.append("请以第一人称视角对以下聊天记录进行压缩总结。\n");
        prompt.append("**重要**：不要只是罗列发生了什么，你必须捕捉关键决策背后的推理逻辑。\n\n");
        
        // 智能体上下文
        prompt.append(buildAgentContextForSummary(agentInstance));
        
        prompt.append("## 叙事风格\n");
        prompt.append("以「我」的视角进行叙述，就像在写工作日志一样。\n");
        prompt.append("示例：「用户让我介绍面板，我先调用loadPanelData获取数据，然后...」\n\n");
        
        prompt.append("## 总结结构\n");
        prompt.append("1. **用户目标**：用户的最终目标是什么\n");
        prompt.append("2. **执行动作**：我调用了哪些工具、执行了什么操作\n");
        prompt.append("3. **思考过程**：我是如何一步步推理和决策的（关键决策的逻辑）\n");
        prompt.append("4. **放弃的方案**：尝试过但失败的方案（解释为什么失败）\n");
        prompt.append("5. **当前状态**：任务进展到哪里了\n");
        prompt.append("6. **重要信息**：后续可能会用到的信息（如面板编号、配置参数、用户偏好等）\n\n");
        
        prompt.append("## 格式要求\n");
        prompt.append("- 使用第一人称「我」进行叙述\n");
        prompt.append("- 语言精简，言简意赅，像在记录工作笔记\n");
        prompt.append("- **单一任务的描述不超过200字**，尽可能精炼\n");
        prompt.append("- 不要提及系统层面的技术细节（如消息ID、返回内容被省略等）\n");
        prompt.append("- 注意：聊天记录中如果出现「返回内容较长，此处省略详情」表示工具**调用成功**，只是详情未展示，不要误解为失败\n\n");
        
        prompt.append(StrUtil.format("## 第{}轮会话记录\n\n", roundIndex));
        prompt.append("```\n");
        for (String msg : formattedMessages) {
            prompt.append(msg);
            prompt.append("\n");
        }
        prompt.append("```\n\n");
        
        prompt.append("请直接输出总结内容，不需要任何前缀或格式标记。");
        
        return prompt.toString();
    }

    /**
     * 构建智能体上下文信息（用于压缩总结）
     */
    private static String buildAgentContextForSummary(AgentInstance agentInstance) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 智能体信息\n");
        
        if (agentInstance == null) {
            sb.append("- 名称：未知\n\n");
            return sb.toString();
        }
        
        AgentDefinition definition = agentInstance.getDefinition();
        if (definition == null) {
            sb.append("- 名称：未知\n\n");
            return sb.toString();
        }
        
        // 名称
        String agentName = definition.getAgentName();
        sb.append(StrUtil.format("- 名称：{}\n", StrUtil.isNotBlank(agentName) ? agentName : "未知"));
        
        // 职务
        String agentRole = definition.getAgentBusinessRole();
        if (StrUtil.isNotBlank(agentRole)) {
            sb.append(StrUtil.format("- 职务：{}\n", agentRole));
        }
        
        // 工具列表（含描述）
        List<ToolDto> tools = definition.getAgentTools();
        if (tools != null && !tools.isEmpty()) {
            sb.append("- 可用工具：\n");
            for (ToolDto tool : tools) {
                String toolName = StrUtil.isNotBlank(tool.getCnName()) ? tool.getCnName() : tool.getName();
                String toolDesc = tool.getDescription();
                if (StrUtil.isNotBlank(toolDesc)) {
                    sb.append(StrUtil.format("  - {}：{}\n", toolName, toolDesc));
                } else {
                    sb.append(StrUtil.format("  - {}\n", toolName));
                }
            }
        }
        
        sb.append("\n");
        return sb.toString();
    }

    /**
     * 构建历史总结上下文提示词（用于新任务开始时）
     * @param allSummaries 所有历史轮次的总结
     * @return 上下文提示词
     */
    public static String buildHistorySummaryContextPrompt(String allSummaries) {
        if (StrUtil.isBlank(allSummaries)) {
            return "";
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("\n<session_history>\n");
        prompt.append("# 历史会话总结\n");
        prompt.append("以下是之前会话轮次的总结，请参考这些上下文理解当前任务：\n\n");
        prompt.append(allSummaries);
        prompt.append("</session_history>\n");
        
        return prompt.toString();
    }

    /**
     * 构建超时降级提示词（压缩未完成时使用）
     * @return 降级提示词
     */
    public static String buildSummaryTimeoutFallbackPrompt() {
        return "[提示] 历史上下文总结尚未完成。如需了解之前的对话内容，建议使用 GetChatHistoryTool 查看聊天记录。\n\n";
    }

}
