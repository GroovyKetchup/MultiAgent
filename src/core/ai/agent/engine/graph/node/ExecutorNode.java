package ai.agent.engine.graph.node;

import ai.agent.dto.graph.NodeExecutionResult;
import ai.agent.dto.groupChat.ExecutionTraceDto;
import ai.agent.dto.groupChat.MessageContextDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.llmCalling.*;
import ai.agent.engine.graph.AbstractGraphNode;
import ai.agent.engine.graph.GraphContext;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolContext;
import ai.agent.engine.groupChat.tool.ToolRegistry;
import ai.agent.enums.AgentStatus;
import ai.agent.enums.ToolCallStatus;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import ai.agent.util.groupChat.MessageBuilder;
import ai.agent.util.llmCalling.NativeFunctionCallingUtil;
import ai.agent.util.llmCalling.PromptBuilder;
import ai.agent.util.llmCalling.ToolSkipStrategy;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static ai.agent.constant.GraphConstants.NODE_CHAT_END;
import static ai.agent.constant.GraphConstants.NODE_EXECUTOR;

@Comment("图-执行节点")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-01", updateTime = "2025-12-01"
)
// TODO 系统提示词要稳定下来，不稳定的部分后续改为追加到最近的一次工具调用里
public class ExecutorNode extends AbstractGraphNode {

    // 默认最大循环次数
    private static final int DEFAULT_MAX_LOOPS = 50;

    // 图-上下文
    private GraphContext ctx;

    // 群聊引擎
    private GroupChatEngine chatEngine;
    // 智能体实例
    private AgentInstance agentInstance;
    // 工具注册器
    private ToolRegistry toolRegistry;
    // 是否打开画布
    private boolean isOpenCanvas = false;
    // 画布动作版本号（记录开始时的版本号）
    private int initialCanvasActionVersion = 0;
    // 计数器
    private final AtomicInteger loopCounter = new AtomicInteger(0);
    // 当前循环的消息历史
    private final List<LlmMessage> currentLoopMessages = new ArrayList<>();
    // 每轮LLM调用的使用统计列表
    private final List<LlmUsageStats> usageStatsList = new ArrayList<>();


    @Override
    public String getName() {
        return NODE_EXECUTOR;
    }

    @Override
    public NodeExecutionResult execute(GraphContext ctx) throws Exception {

        initExecutor(ctx);

        processMessage();

        return NodeExecutionResult.toNode(NODE_CHAT_END)
                .withSummary("执行完成，进入结束节点");
    }


    // ========================= 核心方法 =========================


    // 处理消息
    public void processMessage() throws InterruptedException {
        String agentId = agentInstance.getDefinition().getAgentId();

        try {
            // 重置计数器
            loopCounter.set(0);

            // 记录当前执行版本号（防止被之前的打断影响）
            agentInstance.startExecution();

            // 开始循环
            executeReActLoop();

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw e;
            }
            handleError(agentId, e);
        } finally {
            // 恢复智能体状态
            agentInstance.updateAgentStatus(chatEngine, AgentStatus.IDLE);

            // 如果是子会话，执行完成后标记为WAITING_REPLY并启动异步压缩
            // 压缩统一在这里触发，无论是正常完成还是被打断
            GroupChatInstance groupChatInstance = chatEngine.getGroupChatInstance();
            if (groupChatInstance instanceof SubSessionInstance) {
                SubSessionInstance subSession = (SubSessionInstance) groupChatInstance;

                boolean wasInterrupted = agentInstance.isCurrentExecutionInterrupted();

                synchronized (subSession) {
                    // 只有非打断情况下才在这里标记状态（打断时已经标记过了）
                    if (!wasInterrupted) {
                        subSession.markAsWaitingReply();
                    }
                }

                // 统一触发压缩（包含打断信息，如果有的话）
                if (wasInterrupted) {
                    ConsolePrintUtil.printYellowLn("[ExecutorNode] 子会话被打断，触发压缩");
                } else {
                    ConsolePrintUtil.printGreenLn("[ExecutorNode] 子会话执行完成，触发压缩");
                }
                subSession.startSummaryCompressionAsync();
            }
        }
    }

    // 执行ExecutorNode
    private void executeReActLoop() throws InterruptedException {
        String agentId = agentInstance.getDefinition().getAgentId();

        // 查看画布是否有打开
        CompletableFuture<Boolean> isOpenCanvasFuture = CompletableFuture.supplyAsync(() ->
                chatEngine.getFrontendActionManager().isCanvasOpen(chatEngine));

        this.isOpenCanvas = isOpenCanvasFuture.join();
        // 记录初始画布动作版本号
        this.initialCanvasActionVersion = chatEngine.getFrontendActionManager().getCanvasActionVersion();
        ConsolePrintUtil.printGreenLn(StrUtil.format(
                "isOpenCanvas: {}, initialCanvasActionVersion: {}",
                isOpenCanvas, initialCanvasActionVersion
        ));

        // 只有打开画布以及生成了计划才进行聚合
        boolean enableAggregationMode = StrUtil.isNotBlank(getPlan(ctx));


        try {
            if (enableAggregationMode) GroupChatMessageSender.Chat.aggregationBegin(chatEngine, agentId);

            // 执行ReAct循环
            doReActLoop();

            // 输出累计使用统计
            outputTotalUsageStats();

        } finally {
            // 版本号机制已在processMessage中通过startExecution()处理，此处无需额外清除isInterrupted
            if (enableAggregationMode) GroupChatMessageSender.Chat.aggregationEnd(chatEngine, agentId);
        }


    }

    // 执行ReAct循环
    private void doReActLoop() throws InterruptedException {

        String agentId = agentInstance.getDefinition().getAgentId();
        int maxLoops = getMaxLoops();

        while (loopCounter.get() < maxLoops) {
            // 检查线程中断状态
            boolean isThreadInterrupted = Thread.currentThread().isInterrupted();
            // 使用版本号机制检查是否被中断
            boolean isVersionInterrupted = agentInstance.isCurrentExecutionInterrupted();
            if (isThreadInterrupted || isVersionInterrupted) {
                ConsolePrintUtil.printYellowLn(StrUtil.format("[ExecutorNode] 智能体 {} 检测到线程中断，退出循环", agentId));
                break;
            }

            int currentLoop = loopCounter.incrementAndGet();


            if (currentLoop > getMaxLoops()) {
                GroupChatMessageSender.Notice.hint(chatEngine, agentId, "已超出最大循环次数，请回复[继续]");
                throw new RuntimeException("智能体超出最大循环次数");
            } else {
                ConsolePrintUtil.printGreenLn(StrUtil.format("[ExecutorNode] 智能体 {} 开始第 {} 轮循环",
                        agentId, currentLoop));
            }


            try {
                // 修改智能体状态
                agentInstance.updateAgentStatus(chatEngine, AgentStatus.THINKING);

                // 1、模型调用
                ReactResponse response = callLlm();

                // 收集使用统计
                if (response.getUsageStats() != null) {
                    usageStatsList.add(response.getUsageStats());
                }

                // 2. 将响应添加到消息历史
                String textContent = StrUtil.blankToDefault(response.getTextContent(), "");
                LlmMessage assistantMessage = new LlmMessage(LlmMessage.Role_Assistant, textContent);

                if (CollUtil.isNotEmpty(response.getToolCalls())) {
                    assistantMessage.setToolCalls(response.getToolCalls());
                }
                // DeepSeek思考模式：保存reasoning_content
                if (StrUtil.isNotBlank(response.getReasoningContent())) {
                    assistantMessage.setReasoningContent(response.getReasoningContent());
                }
                currentLoopMessages.add(assistantMessage);


                // 3. 处理LLM响应
                boolean hasContent = StrUtil.isNotBlank(textContent)
                        && !"null".equalsIgnoreCase(textContent.trim());
                if (hasContent) {
                    ConsolePrintUtil.printCyanLn(StrUtil.format("[{}] {}",
                            agentInstance.getDefinition().getAgentName(),
                            textContent));

                    Message textMessage = MessageBuilder.createAgentTextMessage(
                            agentId, textContent, null);
                    chatEngine.submitAgentMessage(textMessage);

                    // 记录消息上下文（包含响应和usage统计）
                    recordMessageContextWithResponse(textMessage.getMsgId(), assistantMessage, currentLoop);
                }

                // 4. 检查是否有工具调用
                if (CollUtil.isEmpty(response.getToolCalls())) {
                    ConsolePrintUtil.printGreenLn(StrUtil.format("[ExecutorNode] 智能体 {} 任务完成，共执行 {} 轮",
                            agentId, currentLoop));
                    break;
                }

                // 5、并行调用工具
                for (ToolCall toolCall : response.getToolCalls()) {

                    // 初始化群聊工具上下文
                    GroupChatToolContextAdapter toolContext = new GroupChatToolContextAdapter(chatEngine,
                            agentInstance.getDefinition().getAgentId());

                    // 发送工具调用消息到前端
                    Message toolCallMessage = MessageBuilder.createAgentToolCallMessage(agentId, toolCall, null);
                    chatEngine.submitAgentMessage(toolCallMessage);

                    long startTime = System.currentTimeMillis();

                    // 执行工具
                    String toolResult = executeToolCall(toolContext, toolCall);
                    long executionTime = System.currentTimeMillis() - startTime;

                    // 执行轨迹
                    List<ExecutionTraceDto> executionTraces = toolContext.getExecutionTraces();

                    ToolCallStatus callStatus = ToolCallStatus.SUCCESS;
                    Message toolResultMessage = MessageBuilder.createToolCallStatusUpdate(
                            agentId, toolCall, callStatus,
                            toolResult, null, executionTime, toolCallMessage.getMsgId(), executionTraces);

                    chatEngine.submitAgentMessage(toolResultMessage);

                    // 将工具执行结果作为tool角色的消息添加到历史
                    LlmMessage toolMessage = new LlmMessage(LlmMessage.Role_Tool, toolResult);
                    toolMessage.setToolCallId(toolCall.getId());
                    currentLoopMessages.add(toolMessage);


                }


            } catch (Exception e) {
                ConsolePrintUtil.printRedLn(StrUtil.format("[ExecutorNode] 智能体 {} 第 {} 轮执行失败: {}",
                        agentId, currentLoop, ExceptionUtils.getFullStackTrace(e)));
                throw e;
            }

        }
    }


    // ========================= 支撑方法 =========================


    // 调用LLM
    private ReactResponse callLlm() throws InterruptedException {

        // 构建消息历史
        List<LlmMessage> messages = buildReActMessages();

//        appendLeftLoopNumberToSystemPrompt(messages);

        // 获取可用工具列表并转换标准格式
        List<Tool> availableTools = getAvailableToolsWithFilter();
        List<Map<String, Object>> toolSchemas = NativeFunctionCallingUtil.convertToolsToFunctions(availableTools);

        // 调用LLM with Function Calling
        String llmResponse = agentInstance.getLlmClient().callChatWithFunctions(
                agentInstance.getEffectiveLlmConfig(),
                messages,
                toolSchemas
        );

        // 解析响应
        return parseReActResponse(llmResponse);
    }


    // 构建消息列表
    // 初始化: 从历史记录中读取当前消息窗口的Text消息 + SystemPrompt
    // 循环中: 从历史记录中读取当前消息窗口的Text消息 + SystemPrompt + 循环中产生的消息
    private List<LlmMessage> buildReActMessages() {

        // 检查画布版本变化（每轮循环都执行）
        checkCanvasVersionChange();

        // 如果是第一轮循环，初始化消息历史
        if (currentLoopMessages.isEmpty()) {

            String systemPrompt = buildReActSystemPrompt();

            ConsolePrintUtil.printCyanLn(StrUtil.format("系统提示词: {}", systemPrompt));

            // 1. 添加系统提示
            currentLoopMessages.add(new LlmMessage(LlmMessage.Role_System, systemPrompt));

            // 2. 添加历史消息
            currentLoopMessages.addAll(
                    getWindowMessage(ctx)
            );

            // 触发消息
            Message triggerMessage = getTriggerMessage(ctx);
            if (triggerMessage == null || !triggerMessage.isTextMessage()) {
                throw new RuntimeException("被触发的消息为空或并非文字消息！");
            }

            // 3、添加用户消息
            currentLoopMessages.add(
                    new LlmMessage(LlmMessage.Role_User, triggerMessage.getTextPayload().getText())
            );
        }


        return new ArrayList<>(currentLoopMessages);
    }

    // 检查画布版本变化并更新状态
    private void checkCanvasVersionChange() {
        int currentVersion = chatEngine.getFrontendActionManager().getCanvasActionVersion();
        boolean versionChanged = currentVersion != initialCanvasActionVersion;

        // 如果版本号变化，说明画布动作已更新，需要重新检查画布状态
        if (versionChanged) {
            ConsolePrintUtil.printYellowLn(StrUtil.format(
                    "[画布动作版本变化] 初始版本: {}, 当前版本: {}, 重新检查画布状态",
                    initialCanvasActionVersion, currentVersion
            ));
            this.isOpenCanvas = chatEngine.getFrontendActionManager().isCanvasOpen(chatEngine);
            this.initialCanvasActionVersion = currentVersion;
        }
    }

    // 构建ReAct模式的系统提示词
    private String buildReActSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        GroupChatInstance groupChatInstance = chatEngine.getGroupChatInstance();
        GroupDefinition groupDefinition = groupChatInstance.getDefinition();
        AgentDefinition agentDefinition = agentInstance.getDefinition();

        // 1. 角色定义
        if (agentDefinition != null) {
            prompt.append(PromptBuilder.buildAgentDefinitionPrompt(agentDefinition));
        }

        // 2. 群组工作流程
        if (groupDefinition != null) {
            prompt.append(PromptBuilder.buildGroupDefinitionPrompt(groupDefinition));
        }

        // 3. 工作计划
        appendPlanPrompt(prompt);

        // 6. 工作原则
        prompt.append(PromptBuilder.buildPrinciplePrompt());

        // 7. 相关名词解释
        prompt.append(PromptBuilder.buildNounsExplanationPrompt());

        // 8、历史经验
        prompt.append(PromptBuilder.buildExperiencePrompt(getAgentExperience(this.ctx)));

        // 9、画布上下文（动态追加）
        prompt.append(PromptBuilder.buildCanvasContextPrompt(chatEngine, isOpenCanvas));

        // 10、子会话历史总结上下文
        String summaries = getHistorySummary(ctx);
        if (StrUtil.isNotBlank(summaries)) {
            prompt.append(PromptBuilder.buildHistorySummaryContextPrompt(summaries));
            ConsolePrintUtil.printGreenLn("[ExecutorNode] 已将历史总结加入系统提示词");
        }

        return prompt.toString();
    }

    /**
     * 输出累计使用统计
     */
    private void outputTotalUsageStats() {
        if (usageStatsList.isEmpty()) return;

        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;
        int totalTokens = 0;
        int totalCacheHit = 0;
        int totalCacheMiss = 0;

        for (LlmUsageStats stats : usageStatsList) {
            totalPromptTokens += stats.getPromptTokens();
            totalCompletionTokens += stats.getCompletionTokens();
            totalTokens += stats.getTotalTokens();
            totalCacheHit += stats.getCacheHitTokens();
            totalCacheMiss += stats.getCacheMissTokens();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Tokens[输入:%d, 输出:%d, 总计:%d]",
                totalPromptTokens, totalCompletionTokens, totalTokens));

        sb.append("\n");

        if (totalCacheHit > 0 || totalCacheMiss > 0) {
            int totalInput = totalCacheHit + totalCacheMiss;
            double hitRate = totalInput > 0 ? (double) totalCacheHit / totalInput : 0;
            sb.append(String.format(" 缓存[命中:%d, 未命中:%d, 命中率:%.1f%%]",
                    totalCacheHit, totalCacheMiss, hitRate * 100));
        }

        ConsolePrintUtil.printGreenLn(sb.toString());
    }

    /**
     * 解析ReAct响应
     * 使用原生Function Calling适配器解析模型响应
     */
    private ReactResponse parseReActResponse(String llmResponse) {

        ReactResponse response = new ReactResponse();

        // 使用原生Function Calling适配器解析
        NativeFunctionCallingUtil.FunctionCallingResult result =
                NativeFunctionCallingUtil.parseFunctionCallingResponse(llmResponse);

        response.setTextContent(result.getTextContent());
        response.setToolCalls(result.getToolCalls());
        response.setUsageStats(result.getUsageStats());
        response.setReasoningContent(result.getReasoningContent());

        ConsolePrintUtil.printGreenLn(StrUtil.format("[ReAct解析] 文本内容: {}, 工具调用数量: {}",
                result.hasTextContent() ? "有" : "无", result.getToolCalls().size()));

        return response;
    }

    /**
     * 执行工具调用
     *
     * @return 工具执行结果
     */
    private String executeToolCall(ToolContext toolContext, ToolCall toolCall) {
        String agentId = agentInstance.getDefinition().getAgentId();

        try {
            // 获取工具
            Tool tool = toolRegistry.get(toolCall.getToolName());
            if (tool == null) {
                String error = "工具不存在: " + toolCall.getToolName();
                ConsolePrintUtil.printRedLn(StrUtil.format("[ReAct工具] {}", error));
                return error;
            }

            ConsolePrintUtil.printYellowLn(StrUtil.format("[ReAct工具] 智能体 {} 开始执行工具 {}",
                    agentId, toolCall.getToolName()));


            return tool.execute(toolContext, toolCall.getParams());


        } catch (Exception e) {
            String error = "工具执行失败: " + e.getMessage();
            ConsolePrintUtil.printRedLn(StrUtil.format("[ReAct工具] 智能体 {} 执行工具 {} 失败: {}",
                    agentId, toolCall.getToolName(), e.getMessage()));

            // 发送错误消息
            Message errorMessage = MessageBuilder.createAgentErrorMessage(agentId,
                    error, ExceptionUtils.getFullStackTrace(e));
            chatEngine.submitAgentMessage(errorMessage);

            return error;
        }
    }

    /**
     * 处理错误
     */
    private void handleError(String agentId, Exception e) {
        ConsolePrintUtil.printRedLn(StrUtil.format("[ReAct错误] 智能体 {} 执行失败: {}",
                agentId, e.getMessage()));
        e.printStackTrace();

        // 发送错误消息
        Message errorMessage = MessageBuilder.createAgentErrorMessage(agentId,
                "消息引擎执行失败: " + e.getMessage(), ExceptionUtils.getFullStackTrace(e));
        chatEngine.submitAgentMessage(errorMessage);
    }


    // 填充计划Prompt
    private void appendPlanPrompt(StringBuilder prompt) {
        String plan = getPlan(ctx);
        if (StrUtil.isNotBlank(plan) && plan.length() > 10) {
            prompt.append("\n<work_plan>\n## 计划指导\n")
                    .append("（注意：如果计划指导中有提及待办事项，请务必调用待办事项相关工具进行创建/变更。）\n")
                    .append(plan)
                    .append("\n</work_plan>\n");

        }


    }

    // ========================= 基础方法 =========================

    private void initExecutor(GraphContext ctx) {
        this.ctx = ctx;
        this.chatEngine = ctx.getChatEngine();
        this.agentInstance = ctx.getAgentInstance();
        this.toolRegistry = agentInstance.getToolRegistry();

    }

    private List<Tool> getAvailableToolsWithFilter() {
        Function<Tool, Boolean> filterToolStrategy;

        if (chatEngine.getToolSkipStrategy() != null) {
            filterToolStrategy = chatEngine.getToolSkipStrategy();
        } else {
            filterToolStrategy = this.isOpenCanvas ? ToolSkipStrategy.onCanvasMode :
                    ToolSkipStrategy.onNonCanvasMode;
        }

        return agentInstance.getAvailableTools(filterToolStrategy);
    }

    // 记录消息上下文
    private void recordMessageContextWithResponse(String msgId, LlmMessage assistantResponse, int loopIndex) {
        if (msgId == null) return;

        try {
            // 提取系统提示词（仌currentLoopMessages中获取）
            String systemPrompt = "";
            for (LlmMessage msg : currentLoopMessages) {
                if (LlmMessage.Role_System.equals(msg.getRole())) {
                    systemPrompt = msg.getContent();
                    break;
                }
            }

            // 获取可用工具列表
            List<Tool> availableTools = getAvailableToolsWithFilter();
            List<ToolDto> toolDtoList = new ArrayList<>();
            for (Tool tool : availableTools) {
                toolDtoList.add(tool.convertToDto());
            }

            // 深拷贝消息列表（包含assistant响应）
            List<LlmMessage> msgList = new ArrayList<>();
            for (LlmMessage msg : currentLoopMessages) {
                LlmMessage copy = new LlmMessage(msg.getRole(), msg.getContent());
                if (msg.getToolCalls() != null) {
                    copy.setToolCalls(msg.getToolCalls());
                }
                if (msg.getToolCallId() != null) {
                    copy.setToolCallId(msg.getToolCallId());
                }
                msgList.add(copy);
            }

            MessageContextDto contextDto = new MessageContextDto()
                    .setSystemPrompt(systemPrompt)
                    .setToolList(toolDtoList)
                    .setMsgList(msgList)
                    .setLoopIndex(loopIndex)
                    .setTimestamp(System.currentTimeMillis())
                    .setUsageStatsList(new ArrayList<>(usageStatsList));

            chatEngine.getMessageHistoryManager().recordMessageContext(msgId, contextDto);

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil.format("[上下文记录] 记录消息上下文失败: {}", e.getMessage()));
        }
    }

    /**
     * 获取最大循环次数
     */
    private int getMaxLoops() {
        return DEFAULT_MAX_LOOPS;
    }
}
