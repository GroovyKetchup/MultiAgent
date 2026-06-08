package ai.agent.engine.groupChat;

import ai.agent.constant.AppConstants;
import ai.agent.dto.groupChat.GroupChatEngineConfig;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.taskboard.TaskBoard;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.engine.graph.GraphEngine;
import ai.agent.engine.groupChat.handler.GroupChatCommandMessageHandler;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.engine.groupChat.session.SubSessionManager;
import ai.agent.enums.*;
import ai.agent.service.GroupChatThreadPollManager;
import ai.agent.service.frontendCalling.FrontendCanvasManager;
import ai.agent.service.groupChat.MessageHistoryManager;
import ai.agent.service.groupChat.manager.GroupChatEngineManager;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.service.workflow.DefaultWorkflowBootstrap;
import ai.agent.service.workflow.WorkflowTemplateRegistry;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatEngineStoreUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import ai.agent.util.groupChat.MessageBuilder;
import ai.agent.util.groupChat.TaskAssignmentMessageBuilder;
import ai.agent.util.ucpParse.AtParser;
import cell.ai.agent.IGroupChatUserInfoService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.annotation.JSONField;
import octocm.domain.dto.DomainDto;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static ai.agent.constant.AppConstants.AGENT_ID_SYSTEM_SCHEDULER;
import static ai.agent.constant.GroupChatConstants.INTERNAL_PLAN_TRIGGER_PREFIX;

/**
 * 群组聊天交互引擎
 * TODO 1: 任务相关机制统一抽离到 TaskBoard
 * TODO 2: 智能体相关机制统一抽离到 GroupChatInst
 * TODO 3: 会话相关机制统一抽离到 ChatSession
 */
public class GroupChatEngine implements Runnable, Serializable {

    // 是否运行
    private volatile boolean running = false;

    // 对话次数
    private final AtomicLong chatNumber = new AtomicLong(0L);

    // 所运行的线程
    @JSONField(serialize = false)
    private Thread currentThread;

    // VotaForge 业务域信息
    // FIXME 后续如果改的话，统一管理
    private DomainDto BusDomain;

    // 引擎配置
    // FIXME 由于目前引擎本身只保存一次，所有发布插件导致的重加载并不会恢复最新已经调整的配置
    private GroupChatEngineConfig engineConfig;

    // 工作中使用的缓存
    // @SEE GCWorkCacheKey
    // FIXME 目前还没有时间查看如何让FastJSON支持Enum值的转换
    @JSONField(serialize = false, deserialize = false)
    private final Map<GCEngineWorkCacheKey, Object> engineWorkCache = new ConcurrentHashMap<>();

    // 正在运行的图引擎
    @JSONField(serialize = false, deserialize = false)
    private final Map<String /* AgentId */, GraphEngine> runningGraphEngines = new ConcurrentHashMap<>();

    // 群组实例
    private GroupChatInstance groupChatInstance;

    // 前端动作的管理器
    @JSONField(serialize = false, deserialize = false)
    private FrontendCanvasManager frontendActionManager;

    // 历史消息管理器
    @JSONField(serialize = false, deserialize = false)
    private MessageHistoryManager messageHistoryManager;

    // 子会话管理器
    @JSONField(serialize = false, deserialize = false)
    private SubSessionManager subSessionManager;

    // 工具跳过策略
    @JSONField(serialize = false, deserialize = false)
    private java.util.function.Function<ai.agent.engine.groupChat.tool.Tool, Boolean> toolSkipStrategy;

    // 等待处理的消息
    @JSONField(serialize = false)
    private final BlockingQueue<Message> messageQueue;


    // 任务看板
    @JSONField(serialize = false)
    private final TaskBoard taskBoard;

    // 最近调度的任务集合，防止重复调度
    @JSONField(serialize = false)
    private final Set<String> recentlyScheduledTaskIds = ConcurrentHashMap.newKeySet();

    // 跳过调度的任务集合，防止重复调度
    @JSONField(serialize = false)
    private final Set<String> skippedScheduledTaskIds = ConcurrentHashMap.newKeySet();

    // 调度互斥锁：避免在智能体发言与自动调度之间发生并发干扰
    @JSONField(serialize = false)
    private final ReentrantLock schedulingMutex = new ReentrantLock(true);

    // 配置参数

    // 最大聊天次数
    private static final int MAX_CHAT_NUMBER = 300;

    // 消息队列超时时间（秒）
    private static final int MESSAGE_QUEUE_TIMEOUT_SECONDS = 5;


    public GroupChatEngine(GroupChatInstance groupChatInstance) {


        this.groupChatInstance = groupChatInstance;
        this.messageHistoryManager = new MessageHistoryManager();
        this.frontendActionManager = new FrontendCanvasManager();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.taskBoard = new TaskBoard(groupChatInstance.getInstanceId());
        this.subSessionManager = new SubSessionManager();

        // 启动默认工作流模板注册
        DefaultWorkflowBootstrap.bootstrap();
    }

    /**
     * 提交用户消息
     */
    public void submitUserMessage(String content) {
        List<String> mentions;

        // 子会话中直接@执行智能体，不需要@机制
        if (isRunInSubSession()) {
            SubSessionInstance subSession = (SubSessionInstance) groupChatInstance;
            AgentDefinition agentDef = subSession.getAgentDefinition();
            if (agentDef != null) {
                mentions = Collections.singletonList(agentDef.getAgentId());
            } else {
                mentions = new ArrayList<>();
            }
        } else {
            // 普通会话使用@解析，如果没有@提及则默认@总控智能体
            mentions = AtParser.parseMentionsWithDefault(content, groupChatInstance.getDefinition());
        }

        Message userMessage = MessageBuilder.createUserTextMessage(content, mentions);

        ConsolePrintUtil.printGreenLn(StrUtil.format("当前引擎状态: isRunning[{}], thread existed[{}], thread Name[{}]",
                        this.running,
                        currentThread != null,
                        currentThread != null ? currentThread.getName() : "null"
                )
        );

        // 添加到历史记录
        messageHistoryManager.addMessage(getBusDomain(), userMessage);

        // 添加到处理队列
        messageQueue.offer(userMessage);

        System.out.println("[用户] " + content);
        if (groupChatInstance instanceof SubSessionInstance) {
            System.out.println("[子会话] 消息直接发送给执行智能体: " + mentions);
        } else if (!AtParser.parseMentions(content).isEmpty()) {
            System.out.println("[系统] 检测到@提及: " + mentions);
        } else {
            System.out.println("[系统] 未检测到@提及，默认@总控智能体: " + mentions);
        }

        // 检查现在的运行状态
        ConsolePrintUtil.printRedLn(StrUtil.format("[运行状态] 运行状态: {}, 线程:{}",
                isRunning(), JSONUtil.toJsonStr(currentThread)));


        if(!isRunInSubSession()){
            // 将引擎备份任务丢到线程池里
            CompletableFuture.runAsync(() -> {
                // 当前OpsLog的拒绝策略是移除最老的任务
                GroupChatEngineStoreUtil.saveTotalData(this);

            }, GroupChatThreadPollManager.get(ThreadPoolType.OPS_LOG));

        }



    }


    /**
     * 提交智能体消息
     */
    public void submitAgentMessage(Message agentMessage) {
        // 进入互斥区：发言期间阻止自动调度抢占
        try {

            String senderId = agentMessage.getSenderId();


            // 如果智能体当前执行被打断了，就不要发了（使用版本号机制判断）
            // 不再依赖状态检查，只依赖版本号机制
            AgentInstance agentInstance = getGroupChatInstance().getAgent(senderId);
            if (agentInstance != null && agentInstance.isCurrentExecutionInterrupted()) {
                ConsolePrintUtil.printRedLn("[运行状态] 智能体 " + senderId + " 当前执行已被中断，消息被拒绝");
                return;
            }

            List<String> mentions = agentMessage.getMentions();
            if (mentions != null && !mentions.isEmpty()) {
                boolean hasPermission = AtParser.validateMentionPermission(
                        senderId,
                        mentions,
                        groupChatInstance.getDefinition()
                );

                if (!hasPermission) {
                    ConsolePrintUtil.printRedLn("[权限检查] 智能体 " + senderId + " 无权限@其他成员，消息被拒绝");
                    return;
                }
            }

            // 添加到历史记录
            messageHistoryManager.addMessage(getBusDomain(), agentMessage);


            // 如果是子会话，转发画布操作消息到父会话
            if (groupChatInstance instanceof SubSessionInstance) {
                SubSessionInstance subSession = (SubSessionInstance) groupChatInstance;
                if (subSession.shouldForwardToParent(agentMessage)) {

                    GroupChatEngine parentEngine = subSession.getParentEngine();
                    if (parentEngine != null) {
                        parentEngine.submitAgentMessage(agentMessage);
                    }
                }
            }

            // 如果是工具调用消息且有@提及，继续处理
            if (agentMessage.isToolCallMessage()
                    && mentions != null && !mentions.isEmpty()) {
                messageQueue.offer(agentMessage);
            }

            // 如果是文本消息且有@提及，也需要处理
            if (agentMessage.isTextMessage()
                    && mentions != null && !mentions.isEmpty()) {
                messageQueue.offer(agentMessage);
            }

            if (agentMessage.isAttachmentMessage()) {
                // 默认@系统调度器
//                agentMessage.setMentions(CollUtil.newArrayList(AGENT_ID_SYSTEM_SCHEDULER));
                messageQueue.offer(agentMessage);
                return;
            }
        } finally {
        }
    }


    /**
     * 启动引擎
     */
    public void start() {


        String threadId = null;
        if (this.groupChatInstance != null) {
            threadId = this.groupChatInstance.getInstanceId();
        } else {
            threadId = IdUtil.fastSimpleUUID();
        }
        Thread engineThread = new Thread(this, StrUtil.format("GroupChatEngine_{}", threadId));
        ConsolePrintUtil.printRedLn("[群组聊天引擎] 启动线程: " + engineThread.getName());
        this.running = true;
        this.currentThread = engineThread;

        engineThread.start();
    }

    /**
     * 停止引擎
     */
    public void stop() {

        this.running = false;

        if (subSessionManager != null) {
            subSessionManager.stopAllSubSessions();
        }

        GroupChatEngineManager.removeChatEngine(groupChatInstance.getInstanceId());
        if (this.currentThread != null) {
            // 打断线程运行
            this.currentThread.interrupt();
        }
    }


    /**
     * 重启引擎
     */
    public void reStart() {

        try {
            this.currentThread.interrupt();
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
        }
        try {
            this.start();
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));

        }

    }


    /**
     * 主处理循环
     */
    @Override
    public void run() {

        ConsolePrintUtil.printRedLn("[群组聊天引擎] 已启动");

        while (running) {
            try {

                // 尝试从消息队列获取消息，最多等待5秒
                Message message = messageQueue.poll(MESSAGE_QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (message != null) {

                    // 有消息，处理消息
                    processMessage(message);

                } else {

                    Boolean isAutoSchedule = getEngineConfig().isStartTaskAutoSchedule();

                    // 默认为开启自动任务调度
                    if (isAutoSchedule == null || isAutoSchedule) {
                        // 超时无消息，检查是否需要自动调度任务
                        processAutoProgressTasks();
                    }


                }

            } catch (InterruptedException e) {
                // 线程被中断（通常是打断操作导致），清除中断状态并继续运行
                Thread.interrupted(); // 清除中断标志
                ConsolePrintUtil.printYellowLn("[群组聊天引擎] 线程被中断，已清除中断状态，继续运行");
            } catch (Exception e) {
                ConsolePrintUtil.printRedLn("[群组聊天引擎] 处理消息时发生错误: \n" +
                        ExceptionUtils.getFullStackTrace(e));
                // 非中断异常记录但不崩溃，继续处理下一个消息
            }
        }

        ConsolePrintUtil.printRedLn("[群组聊天引擎] 已停止");
    }

    /**
     * 自动任务调度器
     * 检查是否满足自动调度条件：1)无消息队列 2)无执行中任务 3)有待办任务
     * 如果满足条件，则自动调度下一个可执行的任务
     */
    private void processAutoProgressTasks() {
        // 1. 检查消息队列是否为空
        if (!messageQueue.isEmpty()) return; // 有待处理消息，不进行自动调度
        if (!isRunning()) return;
        try {

            // 2. 获取所有任务
            List<TaskBoardItem> tasks = taskBoard.getAllTasks();

            if (tasks == null || tasks.isEmpty()) return; // 没有任务，无需调度

            // 3. 检查特殊情况：附件上传后自动完成相关任务
            if (checkAndCompleteAttachmentRelatedTasks(tasks)) return; // 已处理附件相关任务，等待下次调度

            // 4. 检查是否有真正执行中的任务（排除等待用户输入的情况）
            List<TaskBoardItem> inProgressTasks = tasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS)
                    .collect(Collectors.toList());

            if (!inProgressTasks.isEmpty()) {
                // 不为空直接返回
                return;
            }

            // 5. 查找第一个可以开始的待办任务
            TaskBoardItem nextTask = findNextExecutableTask(tasks);

            // 如果有任一智能体处于思考或忙碌中，暂缓自动调度
            if (isAnyAgentActive()) {
                ConsolePrintUtil.printYellowLn("[自动调度器] 检测到有智能体处于THINKING/BUSY，暂缓调度");
                return;
            }

            if (nextTask == null) return; // 没有可执行的任务

            // 6. 自动调度任务
            scheduleTask(nextTask);

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("[自动调度器] 处理失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
//            schedulingMutex.unlock();
        }
    }

    /**
     * 查找下一个可执行的任务
     */
    private TaskBoardItem findNextExecutableTask(List<TaskBoardItem> tasks) {
        // 1、根据任务创建时间排序
        tasks.sort(Comparator.comparing(TaskBoardItem::getCreateTime));

        for (TaskBoardItem task : tasks) {
            if (task.getStatus() == TaskStatus.PENDING && taskBoard.canStart(task.getTaskId())) {
                return task;
            }
        }
        return null;
    }

    /**
     * 检查并完成附件相关任务
     */
    private boolean checkAndCompleteAttachmentRelatedTasks(List<TaskBoardItem> tasks) {
        try {
            // 检查是否有附件消息
            boolean hasAttachment = messageHistoryManager.getFullHistory().stream()
                    .anyMatch(Message::isAttachmentMessage);

            if (!hasAttachment) {
                return false; // 没有附件，无需处理
            }

            // 查找"引导上传需求文档"任务
            TaskBoardItem uploadTask = tasks.stream()
                    .filter(task -> "引导上传需求文档".equals(task.getTaskName()))
                    .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS)
                    .findFirst()
                    .orElse(null);

            if (uploadTask != null) {
                // 检测到附件上传，交由任务执行人（例如业务建模智能体）继续执行，不在此处自动完成
                ConsolePrintUtil.printGreenLn("[系统] 检测到附件上传，等待执行人继续完成任务: " + uploadTask.getTaskName());
                return false;
            }

            return false;
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("[自动调度器] 检查附件任务失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 调度任务执行
     * FIXME 后续抽离到 TaskBoard
     */
    private void scheduleTask(TaskBoardItem task) {
        try {
            String taskId = task.getTaskId();
            String assigneeId = task.getExecutorId();
            if (!isRunning()) return;

            if (skippedScheduledTaskIds.contains(taskId)) {
//                ConsolePrintUtil.printYellowLn("[自动调度器] 任务 " + task.getTaskName() + " 已被设置为跳过调度的任务");
                taskBoard.updateTaskStatus(taskId, TaskStatus.COMPLETED, null);
                return;
            }

            // 检查是否最近已经调度过这个任务
            if (recentlyScheduledTaskIds.contains(taskId)) {
                skippedScheduledTaskIds.add(taskId);
                ConsolePrintUtil.printYellowLn("[自动调度器] 任务 " + task.getTaskName() + " 最近已调度，跳过重复调度");
                return;
            }

            if (assigneeId == null || assigneeId.trim().isEmpty()) {
                skippedScheduledTaskIds.add(taskId);
                ConsolePrintUtil.printRedLn("[自动调度器] 任务 " + task.getTaskName() + " 没有指定执行人，跳过调度");

                return;
            }


            AgentInstance assignee = groupChatInstance.getAgent(assigneeId);

            String scheduleMessage = TaskAssignmentMessageBuilder.build(task, assignee);

            Message taskMessage = MessageBuilder.createAgentTextMessage(
                    AGENT_ID_SYSTEM_SCHEDULER,
                    scheduleMessage,
                    new ArrayList<>()
            );


            GroupChatMessageSender.Chat.aggregationEnd(this, null);

            // 3. 提交调度消息
            submitAgentMessage(taskMessage);

            // 3.1 使用内部触发消息，保证严格顺序：计划启动一定在“任务分配通知”之后
            try {
                if (assignee != null) {
                    WorkflowTemplateRegistry workflowRegistry = WorkflowTemplateRegistry.getInstance();
                    if (workflowRegistry.getByTaskName(task.getTaskName()) != null) {
                        String trigger = INTERNAL_PLAN_TRIGGER_PREFIX + assignee.getDefinition().getAgentId() + "|" + task.getTaskName() + "]]";
                        Message internal = MessageBuilder.createAgentTextMessage(AGENT_ID_SYSTEM_SCHEDULER, trigger, Arrays.asList(assigneeId));
                        submitAgentMessage(internal);
                    }
                }
            } catch (Exception e) {
                ConsolePrintUtil.printRedLn("[自动调度器] 启动预制计划失败: " + e.getMessage());
            }

            // 4. 将任务添加到最近调度集合中，防止重复调度
            recentlyScheduledTaskIds.add(taskId);


        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("[自动调度器] 调度任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 处理消息
     */
    private void processMessage(Message message) throws InterruptedException {

        List<String> mentions = message.getMentions();

        // 如果没有@提及，跳过处理
        if (mentions == null || mentions.isEmpty()) return;

        // 增加一次对话次数
        long currentChatNumber = chatNumber.incrementAndGet();

        // 启用权限逻辑的时候才开启
        if (AppConstants.enableAuthLogic) {
            if (currentChatNumber > MAX_CHAT_NUMBER ||
                    messageHistoryManager.size() > MAX_CHAT_NUMBER ||
                    messageQueue.size() > MAX_CHAT_NUMBER) {
                GroupChatMessageSender.Progress.finishWithError(this, "当前对话次数已超过最大限制，请重置对话后继续使用", "请重置对话后继续使用");
                return;
            }

            long currentLeftModelCallingNo = IGroupChatUserInfoService.get()
                    .getCurrentLeftModelCallingNo();
            if (currentLeftModelCallingNo <= 0) {
                GroupChatMessageSender.Progress.finishWithError(this, "当前已达到最大模型调用次数", "在未获得重新授权前无法进行操作");
                return;
            }

            if (currentLeftModelCallingNo == 20) {
                GroupChatMessageSender.Notice.toast(this, AGENT_ID_SYSTEM_SCHEDULER, NotificationEnums.WARNING,
                        "当前授权的模型调用次数已不足20次"
                );
            }

        }

        // 处理每个@提及的智能体
        for (String mentionText : mentions) {

            // 先尝试通过agentId匹配
            AgentInstance agent = groupChatInstance.getAgent(mentionText);
            // 如果没找到，尝试通过displayName匹配
            if (agent == null) agent = findAgentByDisplayName(mentionText);
            if (agent == null) {
                ConsolePrintUtil.printRedLn("[引擎] 未找到智能体: " + mentionText);
                continue;
            }

            String currentAgentId = agent.getDefinition().getAgentId();

            ConsolePrintUtil.printRedLn("[引擎] 激活智能体: " + agent.getDefinition().getAgentName() + " (" + currentAgentId + ")");

            // 先中断旧的执行（递增版本号），再开始新的执行
            agent.interrupt();
            agent.startExecution();

            // 将用户指定的模型设置为实际调用的模型
            String userAssignLlmModel = getEngineConfig().getUserAssignLlmModel();
            if (StrUtil.isNotBlank(userAssignLlmModel) &&
                    LLMConfigManager.isSupportModel(userAssignLlmModel)) {

                // 设置为实际使用的模型
                agent.setEffectiveLlmConfig(
                        LLMConfigManager.getLlmConfig(userAssignLlmModel)
                );

            }


            // 检查是否为系统命令消息（非内部触发消息）
            String text = message.isTextMessage() ? message.getTextPayload().getText() : null;
            ConsolePrintUtil.printRedLn(StrUtil.format("[被触发消息] {}", text));

            // 只拦截系统命令，内部触发消息需要继续执行以启动GraphEngine
            if (GroupChatCommandMessageHandler.isSystemCommand(text)) {
                GroupChatCommandMessageHandler.handleSystemCommand(text);
                return;
            }

            try {
                // 发送智能体思考中的operate消息给前端
                agent.updateAgentStatus(this, AgentStatus.THINKING);

                // 激活智能体处理消息（包括内部触发消息）
                doActivateAgent(agent, message);

                // 仅当未在执行或等待计划时，才将智能体置为IDLE，避免打断等待唤醒后的执行
                agent.updateAgentStatus(this, AgentStatus.IDLE);

            } catch (Exception e) {
                ConsolePrintUtil.printRedLn("[引擎] 智能体 " + agent.getDefinition().getAgentName() + " 处理失败: " + e.getMessage());
                e.printStackTrace();
            }

        }
    }


    /**
     * 激活智能体处理消息
     */
    private void doActivateAgent(AgentInstance agent, Message triggerMessage) {
        String agentId = agent.getDefinition().getAgentId();
        ConsolePrintUtil.printRedLn("[激活智能体] " + agentId +
                " 触发消息: " + (triggerMessage != null ? triggerMessage.getTextPayload().getText() : "null"));

        GraphEngine graphEngine = GraphEngine.Scene.newChat(triggerMessage);

        try {
            runningGraphEngines.put(agentId, graphEngine);
            graphEngine.start(this, agent);

        } catch (Exception e) {
            ConsolePrintUtil.printRedLn("[GraphEngine] 执行失败: " + e.getMessage());
        } finally {
            runningGraphEngines.remove(agentId);
        }
    }


    // ========================= 支撑方法 =========================


    /**
     * 获取消息历史管理器
     */
    public MessageHistoryManager getMessageHistoryManager() {
        return messageHistoryManager;
    }

    /**
     * 获取群组聊天实例
     */
    public GroupChatInstance getGroupChatInstance() {
        return groupChatInstance;
    }

    /**
     * 获取群组定义
     */
    public GroupDefinition getGroupDefinition() {
        return groupChatInstance.getDefinition();
    }

    /**
     * 获取任务看板
     */
    public TaskBoard getTaskBoard() {
        return taskBoard;
    }


    // 创建任务
    public TaskBoardItem createTask(String taskName, String taskDescription, String creatorId, String assigneeId) {
        TaskBoardItem task = taskBoard.addTask(taskName, taskDescription, creatorId, assigneeId);

        // Send OPERATE message to notify frontend
        Message operateMessage = MessageBuilder.createTaskOperateMessage(
                "system", "TASK_CREATED", task);
        submitAgentMessage(operateMessage);

        return task;
    }

    // 批量创建任务
    public List<TaskBoardItem> createTasks(List<TaskBoard.TaskCreationRequest> requests) {
        List<TaskBoardItem> createdTasks = taskBoard.addTasks(requests);

        // Send OPERATE message to notify frontend
        Message operateMessage = MessageBuilder.createTaskOperateMessage(
                "system", "TASK_CREATED", createdTasks);
        submitAgentMessage(operateMessage);

        return createdTasks;
    }


    // 更新任务状态
    public TaskBoardItem updateTaskStatus(String taskId, TaskStatus status, String executorId) {
        TaskBoardItem updatedTask = taskBoard.updateTaskStatus(taskId, status, executorId);

        if (updatedTask != null) {
            // Send OPERATE message to notify frontend
            Message operateMessage = MessageBuilder.createTaskOperateMessage(
                    "system", "TASK_STATUS_UPDATED", updatedTask);
            submitAgentMessage(operateMessage);

            // 当任务被置为IN_PROGRESS时，通过内部触发消息在"任务分配通知"之后启动预制计划
            // 注意：仅在UCP模式下自动触发计划，ReAct模式不需要
            try {
                if (status == TaskStatus.IN_PROGRESS && executorId != null) {
                    // 统一使用GraphEngine模式，通过内部触发消息启动预制计划
                    ai.agent.engine.groupChat.model.instance.AgentInstance assignee = groupChatInstance.getAgent(executorId);
                    if (assignee != null) {
                        WorkflowTemplateRegistry workflowRegistry = WorkflowTemplateRegistry.getInstance();
                        if (workflowRegistry.getByTaskName(updatedTask.getTaskName()) != null) {
                            String trigger = INTERNAL_PLAN_TRIGGER_PREFIX + assignee.getDefinition().getAgentId() + "|" + updatedTask.getTaskName() + "]]";
                            Message internal = MessageBuilder.createAgentTextMessage(AGENT_ID_SYSTEM_SCHEDULER, trigger, java.util.Arrays.asList(executorId));
                            submitAgentMessage(internal);
                        }
                    }
                }
            } catch (Exception e) {
                ConsolePrintUtil.printRedLn("[计划注入] 触发预制计划失败: " + e.getMessage());
            }
        }

        return updatedTask;
    }

    // 批量更新任务状态
    public List<TaskBoardItem> updateTaskStatuses(List<TaskBoard.TaskStatusUpdateRequest> requests) {
        List<TaskBoardItem> updatedTasks = new java.util.ArrayList<>();

        for (TaskBoard.TaskStatusUpdateRequest request : requests) {
            TaskBoardItem updatedTask = taskBoard.updateTaskStatus(
                    request.getTaskId(), request.getStatus(), request.getExecutorId());
            if (updatedTask != null) {
                updatedTasks.add(updatedTask);
            }
        }

        if (!updatedTasks.isEmpty()) {
            // Send OPERATE message to notify frontend
            Message operateMessage = MessageBuilder.createTaskOperateMessage(
                    "system", "TASK_STATUS_BATCH_UPDATED", updatedTasks);
            submitAgentMessage(operateMessage);
        }

        return updatedTasks;
    }

    // 判断引擎是否运行中
    public boolean isRunning() {
        if (!running) {
            stop();
        }
        return running;
    }


    // 判断引擎是否运行在子会话中
    public boolean isRunInSubSession() {
        return groupChatInstance instanceof SubSessionInstance;
    }

    /**
     * 通过displayName查找智能体
     */
    private AgentInstance findAgentByDisplayName(String displayName) {
        return groupChatInstance.getAgentInstances().values().stream()
                .filter(agent -> agent.getDefinition().getAgentName().equals(displayName))
                .findFirst()
                .orElse(null);
    }

    // 检查是否有智能体处于活跃（思考/忙碌）状态
    private boolean isAnyAgentActive() {
        return groupChatInstance.getAgentInstances().values().stream().anyMatch(a -> {
            String st = a.getStatus();
            return AgentStatus.THINKING.toString().equals(st) || AgentStatus.BUSY.toString().equals(st);
        });
    }


    public GroupChatEngine setBusDomain(DomainDto busDomain) {
        BusDomain = busDomain;
        return this;
    }

    public DomainDto getBusDomain() {
        return BusDomain;
    }

    // ========== 引擎工作缓存 ==========

    public void putEngineWorkCache(GCEngineWorkCacheKey key, Object value) {
        if (key == null) return;
        this.engineWorkCache.put(key, value);
    }

    public HashMap<GCEngineWorkCacheKey, Object> getAllEngineWorkCache() {
        return new HashMap<>(this.engineWorkCache);
    }

    public <T> T getEngineWorkCache(GCEngineWorkCacheKey key, Class<T> classz) {
        if (!engineWorkCache.containsKey(key)) return null;
        Object object = engineWorkCache.get(key);
        if (object == null) return null;
        if (!classz.isInstance(object)) {
            ConsolePrintUtil.printRedLn(StrUtil.format("value中存储的内容类型为[{}],不是你希望的[{}]",
                    object.getClass().getName(),
                    classz.getName()
            ));
            return null;
        }

        return classz.cast(object);

    }


    // ========== 内部的一些对象 ==========

    public FrontendCanvasManager getFrontendActionManager() {
        return frontendActionManager;
    }

    public GroupChatEngine setFrontendActionManager(FrontendCanvasManager frontendActionManager) {
        this.frontendActionManager = frontendActionManager;
        return this;
    }

    public GroupChatEngineConfig getEngineConfig() {
        if (engineConfig == null) engineConfig = new GroupChatEngineConfig();
        return engineConfig;
    }

    public GroupChatEngine setEngineConfig(GroupChatEngineConfig engineConfig) {
        this.engineConfig = engineConfig;
        return this;
    }


    public GroupChatEngine setRunning(boolean running) {
        this.running = running;
        return this;
    }

    public AtomicLong getChatNumber() {
        return chatNumber;
    }

    public Map<GCEngineWorkCacheKey, Object> getEngineWorkCache() {
        return engineWorkCache;
    }

    public BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }

    public Set<String> getRecentlyScheduledTaskIds() {
        return recentlyScheduledTaskIds;
    }

    public Set<String> getSkippedScheduledTaskIds() {
        return skippedScheduledTaskIds;
    }

    public ReentrantLock getSchedulingMutex() {
        return schedulingMutex;
    }

    public GroupChatEngine setGroupChatInstance(GroupChatInstance groupChatInstance) {
        this.groupChatInstance = groupChatInstance;
        return this;
    }

    public GroupChatEngine resetMessageHistoryManager(MessageHistoryManager messageHistoryManager) {
        this.messageHistoryManager = messageHistoryManager;
        return this;
    }

    public SubSessionManager getSubSessionManager() {
        return subSessionManager;
    }

    public GroupChatEngine setMessageHistoryManager(MessageHistoryManager messageHistoryManager) {
        this.messageHistoryManager = messageHistoryManager;
        return this;
    }

    public GroupChatEngine setToolSkipStrategy(java.util.function.Function<ai.agent.engine.groupChat.tool.Tool, Boolean> strategy) {
        this.toolSkipStrategy = strategy;
        return this;
    }

    public java.util.function.Function<ai.agent.engine.groupChat.tool.Tool, Boolean> getToolSkipStrategy() {
        return toolSkipStrategy;
    }


    public Map<String /* AgentId */, GraphEngine> getRunningGraphEngines() {
        return runningGraphEngines;
    }

    public Thread getCurrentThread() {
        return currentThread;
    }
}
