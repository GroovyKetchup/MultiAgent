package ai.agent.constant;

import ai.agent.dto.llmCalling.ToolDto;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.impl.*;
import ai.agent.engine.groupChat.tool.impl.VotaForge.*;
import ai.agent.engine.groupChat.tool.impl.canvas.*;
import ai.agent.engine.groupChat.tool.impl.task.ListTasksTool;
import ai.agent.engine.groupChat.tool.impl.task.MyTasksTool;
import ai.agent.engine.groupChat.tool.impl.task.UpdateTaskStatusTool;
import ai.agent.engine.groupChat.tool.impl.todo.AddTodoTool;
import ai.agent.engine.groupChat.tool.impl.todo.ListTodosTool;
import ai.agent.engine.groupChat.tool.impl.todo.UpdateTodoTool;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.llmCalling.PromptBuilder;
import cn.hutool.core.collection.CollUtil;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 应用常量
 */
public class AppConstants {

    // 启用鉴权机制
    public static final Boolean enableAuthLogic = false;

    // 启用全局子画布机制
    public static final boolean enableAlwaysSubCanvasMode = true;


    // 默认用户
    public static final String DEFAULT_USER_CODE = "OctoCM_MutiAgentGroupChat_User_admin";

    // 群聊实例前缀
    public static final String GROUP_INSTANCE_PREFIX = "GroupChat_Inst_";

    // 用户信息令牌私钥
    public static final String PRIVATE_KEY_USER_INFO_JWT = "kd2025";

    // 聊天开始的欢迎语
    public static final String CHAT_BEGIN_WELCOME_MESSAGE = "VotaForge 团队已准备就绪，让我们一起协作完成任务！ @全体成员";

    // 系统默认默认的智能体ID或智能体名称
    public static final String AGENT_ID_SYSTEM_DEFAULT = "VotaForge";

    // 总控智能体的智能体ID
    public static final String AGENT_ID_SYSTEM_SCHEDULER = "system_scheduler";

    // 原型智能体的智能体ID
    public static final String AGENT_ID_REQUIREMENT_STRUCTURING = "requirement_structuring";

    // 原型智能体的智能体ID
    public static final String AGENT_ID_ROUGH_DELIVERY = "rough_delivery";

    // 标准智能体的智能体ID
    public static final String AGENT_ID_BASIC_DELIVERY = "basic_delivery";

    // 个性交付智能体的智能体ID
    public static final String AGENT_ID_FINE_DECORATION = "fine_decoration";

    // 测试智能体的智能体ID
    public static final String AGENT_ID_TESTING = "tester";

    // 代码智能体的智能体ID
    public static final String AGENT_ID_CODING = "coder";

    // 模型名称
    public static final String LLM_MODEL_NAME_DEEPSEEK_CHAT = "deepseek-v4-flash";

    // 默认交互响应模板
    public static final String DEFAULT_INTERACTION_RESPONSE_TEMPLATE = "选择了${option}";


    // 基础工具集
    public static final List<Class<? extends Tool>> BASIC_TOOLKIT = Arrays.asList(
            GetChatHistoryTool.class,
            UpdateTaskStatusTool.class,
            GetWorkCacheTool.class,
            MyTasksTool.class,
            ListGroupMembersTool.class,
            WaitTool.class
//            CreateInteractionTool.class
    );

    // Canvas工具集
    public static final List<Class<? extends Tool>> CANVAS_TOOLKIT = Arrays.asList(
            ListCanvasActionTool.class,
            CallCanvasActionTool.class,
            AskCanvasOperationGuideTool.class,
            SeeCanvasOperationGuideTool.class,
            OpenCanvasTool.class
    );

    // 待办事项工具类
    public static final List<Class<? extends Tool>> TODO_TOOLKIT = Arrays.asList(
            ListTodosTool.class,
            AddTodoTool.class,
            UpdateTodoTool.class
    );

    // 子智能体工具类
    public static final List<Class<? extends Tool>> SUB_AGENT_TOOLKIT = Arrays.asList(
//            RunTaskTool.class
    );


    // 总控智能体智能体
    public static final AgentDefinition SYSTEM_SCHEDULER_AGENT = new AgentDefinition(
            AGENT_ID_SYSTEM_SCHEDULER,
            "沈宏图",
            null,
            "PMO项目总监",
            "",
            PromptBuilder.buildSystemSchedulerAgentPrompt(),
            convertToolDtos(BASIC_TOOLKIT,
                    CollUtil.newArrayList(
                            AssignTaskToAgentTool.class,
//                            CreateTaskTool.class,
                            ListTasksTool.class,
//                            CompleteTaskTool.class,
                            SetApplicationNameTool.class,
                            VotaForge_CooperativeTaskComposerTool.class
                    )),

            true,
            true
    );

    // 需求智能体
    public static final AgentDefinition REQUIREMENT_STRUCTURING_AGENT = new AgentDefinition(
            AGENT_ID_REQUIREMENT_STRUCTURING,
            "顾大全",
            null,
            "需求分析主管",
            "负责收集、整理和结构化业务需求，确保需求清晰明确。",
            PromptBuilder.buildRequirementStructureAgentPrompt(),
            convertToolDtos(BASIC_TOOLKIT,
                    CANVAS_TOOLKIT,
                    CollUtil.newArrayList(VotaForge_RequirementDocParseTool.class)),
            false,
            true
    );


    // 原型交付
    public static final AgentDefinition ROUGH_DELIVERY_AGENT = new AgentDefinition(
            AGENT_ID_ROUGH_DELIVERY,
            "梁栋材",
            null,
            "基础架构主管",
            "基于VotaForge内置的规则进行交付",
            PromptBuilder.buildRoughAgentPrompt(),
            convertToolDtos(BASIC_TOOLKIT,
                    CollUtil.newArrayList(NotifyUserTool.class,
                            VotaForge_RoughDeliveryTool.class)),

            false,
            true
    );

    // 标准智能体
    public static final AgentDefinition BASIC_DELIVERY_AGENT = new AgentDefinition(
            AGENT_ID_BASIC_DELIVERY,
            "陆行简",
            null,
            "敏捷交付主管",
            "在原型交付的基础上进行标准交付",
            PromptBuilder.buildBasicDeliveryAgentPrompt(),
            convertToolDtos(BASIC_TOOLKIT,
                    CANVAS_TOOLKIT, TODO_TOOLKIT, SUB_AGENT_TOOLKIT,
                    CollUtil.newArrayList(
                            VotaForge_BasicDeliveryTool.class
                    )),
            false,
            true
    );


    // 个性交付智能体
    public static final AgentDefinition FINE_DECORATION_DELIVERY_AGENT = new AgentDefinition(
            AGENT_ID_FINE_DECORATION,
            "宋高定",
            null,
            "产品精修顾问",
            "可以进行个性交付的智能体",
            PromptBuilder.buildFineDecorationAgentPrompt(),
            // 使用基础和JSON两种工具集
            convertToolDtos(BASIC_TOOLKIT, CANVAS_TOOLKIT, TODO_TOOLKIT),
            false,
            true
    );

    // 测试智能体
    public static final AgentDefinition TESTING_AGENT = new AgentDefinition(
            AGENT_ID_TESTING,
            "严恪",
            null,
            "风控测试主管",
            "对已发布的系统进行完整的测试",
            PromptBuilder.buildTestAgentPrompt(),
            // 使用基础和JSON两种工具集
            convertToolDtos(BASIC_TOOLKIT, TODO_TOOLKIT, CANVAS_TOOLKIT),
            false,
            true
    );

    // 代码智能体
    public static final AgentDefinition CODING_AGENT = new AgentDefinition(
            AGENT_ID_CODING,
            "罗辑",
            null,
            "开发主管",
            "可以进行代码的编写",
            PromptBuilder.buildCodingAgentPrompt(),
            // 使用基础和JSON两种工具集
            convertToolDtos(BASIC_TOOLKIT, TODO_TOOLKIT, CANVAS_TOOLKIT),
            false,
            true
    );


    // ReAct模式工作流程
    public static final String REACT_GROUP_WORKFLOW =
            "## 工作定义\n" +
                    "- 我们是VotaForge 智能体开发团队，我们最大的使命是完成用户交给我们的需求以及任务，尽自己的最大的努力！\n" +
                    "## 协作规则\n" +
                    "- 收到任务分配后，使用'我的任务列表'工具核验任务详情，如果存在，则需更新在进行中和结束更新任务状态；反之如果没有收到任务分配需求，则不必调用该工具。 \n" +
                    "- 使用模型自带的机制调用工具，系统会自动处理工具执行\n" +
                    "- 可以一次调用多个工具，可以优先批量调用工具获取完整的上下文，只要是独立的两个工具调用需求,就不要排队,直接并行。\n" +
                    "- 如需用户输入，直接在回复中说明需要什么信息，等待用户回复后继续\n" +
                    "- 如果你收到了一条用户上传的附件消息，你需要询问用户想要做什么，是否为生成应用。\n";

    // 默认群组
    public static final GroupDefinition DEFAULT_GROUP_DEFINITION = new GroupDefinition(
            "default",
            "VotaForge 智能体开发团队",
            "专业的业务系统开发团队，包含系统调度、需求分析、业务建模、基础开发和优化交付等完整流程。",
            Arrays.asList(
                    SYSTEM_SCHEDULER_AGENT,
                    REQUIREMENT_STRUCTURING_AGENT,
                    ROUGH_DELIVERY_AGENT,
                    BASIC_DELIVERY_AGENT,
                    FINE_DECORATION_DELIVERY_AGENT,
                    TESTING_AGENT,
                    CODING_AGENT
            ),
//            DEFAULT_LLM_CONFIG,  // Required group LLM config
            LLMConfigManager.getAutoLlmConfig(LLMConfigManager.AutoModelType.DEFAULT),
            REACT_GROUP_WORKFLOW  // Group workflow
    );


    // addALl多个list
    public static List<ToolDto> convertToolDtos(Collection<Class<? extends Tool>>... elements) {
        List<ToolDto> list = new ArrayList<>();
        if (CollUtil.isEmpty(Arrays.asList((elements)))) return list;

        for (Collection<Class<? extends Tool>> element : elements) {
            for (Class<? extends Tool> clazz : element) {
                try {
                    Tool tool = clazz.getDeclaredConstructor().newInstance();
                    if (tool != null) {
                        list.add(tool.convertToDto());
                    }
                } catch (Exception e) {
                    ConsolePrintUtil.printRedLn(
                            ExceptionUtils.getFullStackTrace(e)
                    );
                }
            }
        }

        return list;

    }


}
