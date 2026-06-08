package cell.ai.agent;

import ai.agent.constant.AppConstants;
import ai.agent.constant.SubSessionConstants;
import ai.agent.dto.RespondDto;
import ai.agent.dto.frontendCalling.FrontendActionDto;
import ai.agent.dto.groupChat.*;
import ai.agent.dto.groupChat.canvas.CanvasStatus;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.AttachmentPayload;
import ai.agent.dto.groupChat.taskboard.TaskExecutionItemDto;
import ai.agent.dto.groupChat.textStyle.ErrorItemDto;
import ai.agent.dto.llmCalling.ToolDto;
import ai.agent.engine.graph.GraphEngine;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.engine.groupChat.session.SubSessionManager;
import ai.agent.engine.groupChat.session.SubSessionMode;
import ai.agent.engine.groupChat.tool.ToolRegistry;
import ai.agent.enums.AgentStatus;
import ai.agent.enums.GCEngineWorkCacheKey;
import ai.agent.enums.OperateMessageEnums;
import ai.agent.enums.ThreadPoolType;
import ai.agent.service.GroupChatThreadPollManager;
import ai.agent.service.frontendCalling.FrontendAsyncCallService;
import ai.agent.service.groupChat.MessageHistoryManager;
import ai.agent.service.groupChat.manager.GroupChatEngineManager;
import ai.agent.service.groupChat.manager.GroupChatInstanceManager;
import ai.agent.service.groupChat.manager.GroupDefinitionManager;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.service.llmCalling.HttpLlmClient;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.UserInfoUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import ai.agent.util.groupChat.MessageBuilder;
import ai.agent.util.groupChat.WorkCacheUtil;
import cell.ServiceCellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.service.IPanelDesignService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import fe.cmn.panel.PanelContext;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.dc.http.AppUserInfo;
import gpf.exception.VerifyException;
import octo.cm.dto.ErrorDto;
import octo.cm.util.ApplicationUtil;
import octo.cm.util.EasyOperation;
import octo.cm.util.PanelDesignPublishErrorContext;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static ai.agent.constant.GroupChatConstants.FormModelId_GroupChatFile;
import static ai.agent.constant.GroupChatConstants.INTERNAL_PLAN_TRIGGER_PREFIX;

@Comment("群聊前端服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-02", updateTime = "2025-09-02"
)
//  cell.ai.agent.IGroupChatFrontEndService
public interface IGroupChatFrontEndService extends ServiceCellIntf, IGroupChatBasicService {


    EasyOperation Op = EasyOperation.get();


    // 心跳
    @SuppressWarnings("ping")
    default String ping(PanelContext panelContext) {
        return "pong";
    }


    // 鉴权的逻辑
    @SuppressWarnings("unused")
    default RespondDto auth(PanelContext panelContext, String token, String sourceAppUrl) {
        if (StrUtil.isBlank(token)) return RespondDto.newError("令牌为空或非法，请联系平台方重新授权");

        try {
            IGroupChatUserInfoService.get().checkAndTakeEffectToken(token, sourceAppUrl);
        } catch (Exception e) {
            return RespondDto.newError(e.getMessage());
        }


        return RespondDto.newSuccess("鉴权通过", null);
    }


    @SuppressWarnings("unused")
    default RespondDto getGroupChatRoom(PanelContext panelContext, String groupChatInstId) {


        try {

            ConsolePrintUtil.printGreenLn(StrUtil.format("尝试恢复群聊房间:[{}]", groupChatInstId));

            GroupChatInstanceManager manager = GroupChatInstanceManager.getInstance();

            // 初始化GroupChatEngine
            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);
            if (chatEngine == null) throw new RuntimeException("恢复群聊引擎失败！");

            GroupChatInstance groupChatInst = chatEngine.getGroupChatInstance();
            if (groupChatInst == null) throw new RuntimeException("恢复的群聊引擎中不存在实例信息！");

            // 设置引擎配置
            if (chatEngine.getEngineConfig() == null) {
                chatEngine.setEngineConfig(new GroupChatEngineConfig());
            }

            ConsolePrintUtil.printGreenLn(
                    StrUtil.format("初始化群聊引擎配置:{}", JSONUtil.toJsonStr(chatEngine.getEngineConfig()))
            );


            boolean isNewRoom = checkNewRoom(chatEngine);

            // 返回给前端统一的Dto
            GroupChatRoomDto groupChatInfo = buildGroupChatRoomDto(chatEngine, isNewRoom);

            return RespondDto.newSuccess("恢复群聊房间成功", groupChatInfo);


        } catch (Exception e) {
            Op.logException(e);
            return RespondDto.newError("恢复群聊房间出现异常 " + e.getMessage());
        }
    }


    // 创建房间
    @SuppressWarnings("unused")
    default RespondDto createGroupChatRoom(PanelContext panelContext, String groupChatId) {


        try {
            AppUserInfo appUserInfo = UserInfoUtil.parseUserInfo(panelContext);
            if (appUserInfo == null) throw new RuntimeException("账号异常，可能需要重新登录");


            GroupChatInstanceManager manager = GroupChatInstanceManager.getInstance();

            // 群聊定义编号
            String groupChatDefinitionCode = StrUtil.isBlank(groupChatId) ? "default" : groupChatId;

            // 通过code拿到定义
            GroupDefinition groupDefinition = GroupDefinitionManager.getDefinition(groupChatDefinitionCode);
            if (groupDefinition == null) {
                return RespondDto.newError(StrUtil.format("不存在群组: [{}]", groupChatDefinitionCode));
            }

            // 更新定义
            GroupDefinitionManager.tryUpdateDefinition(groupDefinition);

            // 创建群组实例
            // @SEE InstanceFactory.createGroupChatInstance
            GroupChatInstance groupChatInst = manager.createGroupChatInstance(groupDefinition);
            String groupChatInstInstId = groupChatInst.getInstanceId();
            ConsolePrintUtil.printGreenLn(
                    StrUtil.format("创建房间新创建了一个群聊实例:{}", groupChatInstInstId)
            );

            // 从系统中加载LLMConfig
            LLMConfigManager.loadModels();

            // 初始化GroupChatEngine
            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstInstId);
            if (chatEngine == null) throw new RuntimeException("创建群聊引擎失败！");

            // 在VotaForge创建对应的业务域
            DomainDto busDomain = IVotaForgeService.get().getOrCreateBusDomain(groupChatInstInstId);
            if (busDomain == null) throw new RuntimeException("VotaForge创建业务域失败！");

            // 设置引擎配置
            chatEngine.setEngineConfig(new GroupChatEngineConfig());

            // 在后台创建房间
            try (IDao dao = IDaoService.newIDao()) {
                IGroupChatRoomService.get()
                        .createRoomInfo(dao, groupChatInstInstId,
                                busDomain.getDomainName(),
                                appUserInfo.getUserId(),
                                busDomain.getDomainCode()
                        );
                dao.commit();
            }

            // 添加系统初始化消息
            chatEngine.getMessageHistoryManager().addMessage(busDomain, MessageBuilder.createAgentTextMessage(
                    AppConstants.AGENT_ID_SYSTEM_DEFAULT,
                    AppConstants.CHAT_BEGIN_WELCOME_MESSAGE,
                    null
            ));


            // 返回给前端统一的Dto
            GroupChatRoomDto groupChatInfo = buildGroupChatRoomDto(chatEngine, true);
            return RespondDto.newSuccess("创建群聊房间成功", groupChatInfo);


        } catch (Exception e) {
            Op.logException(e);
            return RespondDto.newError("创建群聊房间失败，请尝试退出后重新登录 ");
        }
    }

    // 重启房间
    @SuppressWarnings("unused")
    default RespondDto reStartChatRoom(PanelContext panelContext, String groupChatInstId) throws Exception {


        try {
            // 断言存在群聊实例
            assertGroupChatInstIsRunning(groupChatInstId);

            GroupChatEngine chatEngine = GroupChatEngineManager
                    .getGroupChatEngine(groupChatInstId);

            assert chatEngine != null;

            GroupChatInstanceManager groupChatInstManager = GroupChatInstanceManager.getInstance();
            GroupChatInstance groupChatInst = groupChatInstManager.getGroupChatInstance(groupChatInstId);


            // 打断自己
            chatEngine.reStart();


            // 删除已经存在的业务域
        } catch (Exception e) {
            Op.logException(e);
        }


        return RespondDto.newSuccess("重启成功", null);
    }


    // 用户向群组实例发送信息
    @SuppressWarnings("unused")
    default RespondDto userSendMessageToChatRoom(PanelContext panelContext,
                                                 String groupChatInstId, String message) {


        GroupChatInstanceManager groupChatInstManager = GroupChatInstanceManager.getInstance();
        GroupChatInstance groupChatInst = groupChatInstManager.getGroupChatInstance(groupChatInstId);

        assertGroupChatInstIsRunning(groupChatInstId);

        // 集成新的GroupChatEngine机制
        try {
            // 获取或创建GroupChatEngine
            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInst.getInstanceId());
            // 提交用户消息到新的引擎
            chatEngine.submitUserMessage(message);


        } catch (Exception e) {
            Op.logException(e);
            return RespondDto.newError("发送消息失败:" + e.getMessage());

        }

        return RespondDto.newSuccess("", true);
    }

    // 获取群组聊天消息历史（结构化数据）
    @SuppressWarnings("unused")
    default RespondDto queryGroupChatMessages(PanelContext panelContext, String groupChatInstId,
                                              Long anchorTimestamp, Long diffVal) {


        try {

            GroupChatInstanceManager groupChatInstManager = GroupChatInstanceManager.getInstance();
            GroupChatInstance groupChatInst = groupChatInstManager.getGroupChatInstance(groupChatInstId);

            assertGroupChatInstIsRunning(groupChatInstId);

            // 获取GroupChatEngine
            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInst.getInstanceId());


            // 过滤Predicate
            Predicate<Message> messagePredicate = msg -> {
                if (!msg.isTextMessage()) return true;
                String text = msg.getTextPayload().getText();
                if (StrUtil.isBlank(text)) return false;
                if (text.startsWith(INTERNAL_PLAN_TRIGGER_PREFIX)) return false;
                return true;
            };

            // 获取消息历史
            List<Message> messages;
            if (anchorTimestamp != null && diffVal != null) {


                int beforeCount = diffVal < 0 ? Math.toIntExact(diffVal) * -1 : 0;
                int afterCount = diffVal > 0 ? Math.toIntExact(diffVal) : 0;

                messages = chatEngine.getMessageHistoryManager()
                        // 防止RPC库直接解析为了Long
                        .getContextMessagesByTimestamp(anchorTimestamp, beforeCount, afterCount).stream()
                        .filter(messagePredicate)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);


            } else {

                // 获取所有消息
                messages = chatEngine.getMessageHistoryManager().getFullHistory();

            }

            return RespondDto.newSuccess("获取消息成功", messages);

        } catch (Exception e) {
            return RespondDto.newError("获取消息失败: " + JSONUtil.toJsonStr(e));
        }
    }

    // 获取聊天消息历史
    // FIXME 只是为了兼容之前的，后续移除
    @SuppressWarnings("unused")
    default RespondDto queryGroupChatMessages(PanelContext panelContext, String groupChatInstId,
                                              Long afterTimestamp) {
        return queryGroupChatMessages(panelContext, groupChatInstId, afterTimestamp, 10L);
    }

    // 删除聊天信息
    @SuppressWarnings("unused")
    default RespondDto deleteGroupChatMessages(PanelContext panelContext, String groupChatInstId,
                                               List<String> msgIds) {

        try {
            assertGroupChatInstIsRunning(groupChatInstId);
            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);


            // 1、从内存中删除
            MessageHistoryManager messageHistoryManager = chatEngine.getMessageHistoryManager();
            messageHistoryManager.deleteMessageByIds(msgIds);

            // 2、从数据库中删除
            try (IDao dao = IDaoService.newIDao()) {
                String domainCode = chatEngine.getBusDomain().getDomainCode();

                IGroupChatMessageService.get().removeByMsgCodes(dao, domainCode, msgIds);
                dao.commit();

            } catch (Exception dbException) {
                Op.logException(dbException);
                throw new RuntimeException("数据库删除消息失败: " + dbException.getMessage(), dbException);
            }

            return RespondDto.newSuccess("删除消息成功", true);

        } catch (Exception e) {
            Op.logException(e);
            return RespondDto.newError("删除消息失败: " + e.getMessage());
        }


    }


    // 用户发送附件消息到房间
    @SuppressWarnings("unused")
    default RespondDto userSendAttachmentMessageToChatRoom(PanelContext panelContext,
                                                           String groupChatInstId, String fileCode, String fileName) {

        try {
            // 发送附件消息
            GroupChatInstanceManager groupChatInstManager = GroupChatInstanceManager.getInstance();
            GroupChatInstance groupChatInst = groupChatInstManager.getGroupChatInstance(groupChatInstId);
            if (groupChatInst != null) {
                GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInst.getInstanceId());
                Message attachMsg = MessageBuilder
                        .createUserAttachmentMessage(fileCode, fileName);

                chatEngine.submitAgentMessage(attachMsg);

            }

            return RespondDto.newSuccess("发送成功", null);
        } catch (Exception e) {
            return RespondDto.newError("发送附件消息失败");
        }
    }

    // 查询系统内所有附件
    @SuppressWarnings("unused")
    default RespondDto querySystemAttachments(PanelContext panelContext,
                                              String groupChatInstId) {
        try {

            try (IDao dao = IDaoService.newIDao()) {
                ResultSet<Form> fileFormRs = IFormMgr.get().queryFormPage(dao, FormModelId_GroupChatFile, null, 1, Integer.MAX_VALUE, true, true);
                List<AttachmentPayload> attachments = new ArrayList<>();
                for (Form fileForm : fileFormRs.getDataList()) {

                    String fileCode = fileForm.getString(Form.Code);
                    String fileName = fileForm.getString("名称");
                    if (StrUtil.hasBlank(fileCode, fileName)) continue;
                    if (fileName.startsWith("PanelX_SYSTEM_DEFAULT_")) continue;
                    if (fileName.startsWith("PanelX_USER_UPLOAD_")) continue;
                    AttachmentPayload payload = new AttachmentPayload(fileCode, fileName);
                    attachments.add(payload);

                }

                return RespondDto.newSuccess("获取成功", attachments);

            }

        } catch (Exception e) {
            return RespondDto.newError("获取失败");
        }
    }

    // 获取系统当前配置
    @SuppressWarnings("unused")
    default RespondDto getGroupChatCurrentSystemConfig(PanelContext panelContext, String groupChatInstId) {

        assertGroupChatInstIsRunning(groupChatInstId);

        GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);

        // TODO 如果后面增加了动态管理大模型配置的时候，这里需要给配置项加入
        GroupChatEngineConfig engineConfig = chatEngine.getEngineConfig();
        if (engineConfig == null) {
            engineConfig = new GroupChatEngineConfig();
            chatEngine.setEngineConfig(engineConfig);
        }

        return RespondDto.newSuccess("获取成功", engineConfig);

    }


    // 更新系统当前配置
    @SuppressWarnings("unused")
    default RespondDto updateGroupChatCurrentSystemConfig(PanelContext panelContext,
                                                          String groupChatInstId, Map<String, Object> newConfig) {
        assertGroupChatInstIsRunning(groupChatInstId);

        if (newConfig == null || newConfig.isEmpty()) throw new RuntimeException("请输入正确的系统配置");

        GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);
        GroupChatEngineConfig engineConfig = chatEngine.getEngineConfig();

        if (engineConfig == null) return RespondDto.newError("数据异常，当前群聊引擎不存在配置，因此无法更新");

        engineConfig.updateConfig(newConfig);


        // FIXME 后续将这种逻辑重构为，需要被更新的组件注册到系统配置，然后系统配置更新的时候通知这些组件
        MessageHistoryManager messageHistoryManager = chatEngine.getMessageHistoryManager();
        if (messageHistoryManager != null && engineConfig.getContextTimeLimit() > 0) {
            messageHistoryManager.setContextTimeLimit(engineConfig.getContextTimeLimit());
        }


        return RespondDto.newSuccess("更新成功", engineConfig);
    }


    // 查询当前业务域所有的面板设计
    @SuppressWarnings("unused")
    default RespondDto queryAllPanelDesign(PanelContext panelContext,
                                           String groupChatInstId) {
        try {
            List<Form> panelDesigns = IVotaForgeService.get().queryAllPanelDesign(groupChatInstId);
            return RespondDto.newSuccess("获取成功",
                    WorkCacheUtil.convertPdsToWorkCachePds(panelDesigns));


        } catch (Exception e) {
            return RespondDto.newError("获取失败");
        }
    }


    // 发布面板设计
    @SuppressWarnings("unused")
    default RespondDto publishPanelDesign(PanelContext panelContext,
                                          String groupChatInstId, String panelCode, String targetPageEntry) {
        try {
            if (StrUtil.isBlank(panelCode)) throw new RuntimeException("面板编号不得为空");
            if (StrUtil.isBlank(targetPageEntry)) throw new RuntimeException("页面入口不得为空");

            assertGroupChatInstIsRunning(groupChatInstId);

            // 负责这事的智能体
            String responsibleAgentId = AppConstants.AGENT_ID_BASIC_DELIVERY;

            GroupChatEngine chatEngine = GroupChatEngineManager.getGroupChatEngine(groupChatInstId);
            OctoDomainOpObserver octoDomainOpObserver = new OctoDomainOpObserver(chatEngine.getBusDomain());


            Message taskMessage = null;
            TaskExecutionItemDto taskExecutionDto = null;

            try (IDao dao = IDaoService.newIDao()) {

                Form panelDesignForm = IPanelDesignService.get().getPanelDesign(dao, octoDomainOpObserver, panelCode);
                if (panelDesignForm == null) throw new RuntimeException("无法找到该面板设计");
                String panelName = panelDesignForm.getString("面板名称");

                // 创建任务列表
                taskExecutionDto = new TaskExecutionItemDto()
                        .setStatus(TaskExecutionItemDto.Status.PENDING)
                        .setName("发布面板")
                        .setDescription(StrUtil.format("发布[{}({})→{}]",
                                panelName, panelCode, targetPageEntry));

                taskMessage = GroupChatMessageSender.Task.create(chatEngine, responsibleAgentId,
                        Collections.singletonList(taskExecutionDto
                                .setStatus(TaskExecutionItemDto.Status.IN_PROGRESS)
                        ));


                // 实际的发布面板代码
                IVotaForgeService.get()
                        .publishPanelDesign(groupChatInstId, panelCode, targetPageEntry);

                if (PanelDesignPublishErrorContext.hasError()) {
                    throw new RuntimeException("发布失败");
                }


                // 标记任务执行成功
                GroupChatMessageSender.Task.updateItem(chatEngine, responsibleAgentId,
                        taskMessage.getMsgId(),
                        taskExecutionDto.setStatus(TaskExecutionItemDto.Status.COMPLETED)
                );


            } catch (Exception e) {

                // 标记任务执行失败
                if (taskMessage != null && taskExecutionDto != null) {

                    List<ErrorDto> publishErrors = PanelDesignPublishErrorContext.getErrorsAndClear();
                    List<ErrorItemDto> errorItemDtos = null;

                    if (Op.isEmpty(publishErrors)) {
                        errorItemDtos = Collections.singletonList(new ErrorItemDto()
                                .setErrorName("发布面板失败")
                                .setErrorDesc(e.getMessage())
                                .setIndex(1));
                    } else {
                        errorItemDtos = ErrorItemDto.fromPanelDesignPublishError(publishErrors);
                    }

                    GroupChatMessageSender.Task.updateItem(chatEngine, responsibleAgentId,
                            taskMessage.getMsgId(),
                            taskExecutionDto.setStatus(TaskExecutionItemDto.Status.ERROR)
                                    .setErrors(errorItemDtos)
                    );
                }

            }


            return RespondDto.newSuccess("发布成功", null);

        } catch (Exception e) {
            return RespondDto.newError(e.getMessage());
        }
    }


    // 删除面板设计
    @SuppressWarnings("unused")
    default RespondDto deletePanelDesign(PanelContext panelContext,
                                         String groupChatInstId, String panelCode) {
        try {

            IVotaForgeService.get().deletePanelDesign(groupChatInstId, panelCode);
            return RespondDto.newSuccess("操作成功", null);

        } catch (Exception e) {
            return RespondDto.newError("获取失败");
        }
    }


    // 调用大模型
    @SuppressWarnings("unused")
    default RespondDto callLLm(PanelContext panelContext, String groupChatInstId,
                               String systemPrompt, String userPrompt) {

        try {


            ConsolePrintUtil.printGreenLn(StrUtil.format("客户端主动调用大模型, \nsystemPrompt:{}, \nuserPrompt:{}",
                    systemPrompt, userPrompt));

            // 将用户指定的模型设置为实际调用的模型
            LLMConfig llmConfig = LLMConfigManager.getAutoLlmConfig(LLMConfigManager.AutoModelType.HIGH_PERFORMANCE);

            String responds = new HttpLlmClient()
                    .callLlm(llmConfig, systemPrompt, userPrompt);

            return RespondDto.newSuccess("调用成功", responds);
        } catch (Exception e) {
            Op.logException(e);
            return RespondDto.newError("调用失败");

        }
    }


    // 打断智能体发言
    @SuppressWarnings("unused")
    default RespondDto interruptAllAgentSpeak(PanelContext panelContext,
                                              String groupChatInstId) {

        try {
            assertGroupChatInstIsRunning(groupChatInstId);
            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);
            // MODE1: 等待任务完成后的打断（使用版本号机制）
            for (AgentInstance agentInstance : chatEngine.getGroupChatInstance().getAgentInstances().values()) {
                agentInstance.interrupt();
                agentInstance.updateAgentStatus(chatEngine, AgentStatus.IDLE);
            }

            // MODE2: 强制打断正在运行的GraphEngine
            Map<String, GraphEngine> runningGraphEngines = chatEngine.getRunningGraphEngines();
            if (!Op.isEmpty(runningGraphEngines)) {
                for (GraphEngine graphEngine : runningGraphEngines.values()) {
                    graphEngine.interrupt();
                }
            }


            // 通知对话已打断
            GroupChatMessageSender.Notice.hint(chatEngine, null,
                    StrUtil.format("你打断了对话"));

            // 通知对话结束
            GroupChatMessageSender.Chat.chatEnd(chatEngine);


            return RespondDto.newSuccess("操作成功", null);
        } catch (Exception e) {
            return RespondDto.newError("操作失败:" + ExceptionUtils.getFullStackTrace(e));

        }

    }


    // 前端操作画布
    // operationType: 操作类型，@see OperateMessageEnums.CANVAS_OPEN(CANVAS_CLOSE)
    // operationName: 操作名称（具体的，要做什么事情）
    @SuppressWarnings("unused")
    default RespondDto operateCanvas(PanelContext panelContext,
                                     String groupChatInstId,
                                     String operationType,
                                     String operationName,
                                     LinkedHashMap operationObj) {

        try {
            if (StrUtil.isBlank(operationType)) throw new VerifyException("操作类型不得为空");

            JSONObject operationJsonObj = JSONUtil.parseObj(operationObj);
            String senderId = operationJsonObj.getStr("senderId");
            String canvasType = operationJsonObj.getStr("canvasType");
            Map<String, Object> canvasParamsObj = operationJsonObj.getBean("canvasParams", Map.class);
            if (StrUtil.isBlank(senderId)) throw new VerifyException("操作人ID(AgentId)不得为空");
            if (StrUtil.isBlank(canvasType)) throw new VerifyException("画布类型不得为空");


            assertGroupChatInstIsRunning(groupChatInstId);
            GroupChatEngine primaryEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);
            GroupChatInstance groupChatInstance = primaryEngine.getGroupChatInstance();
            AgentDefinition agentDefinition = groupChatInstance.getDefinition().getAgentDefinition(senderId);
            if (agentDefinition == null) throw new VerifyException(StrUtil.format("不存在Id为[{}]的智能体", senderId));

            if (OperateMessageEnums.CANVAS_OPEN.toString().equals(operationType)) {

                if (!AppConstants.enableAlwaysSubCanvasMode) {
                    // 直接打开主画布
                    GroupChatMessageSender.Canvas.open(primaryEngine,
                            senderId, canvasType, canvasParamsObj);
                } else {

                    try {

                        SubSessionManager subSessionManager = primaryEngine.getSubSessionManager();

                        // 判断是否需要排队
                        if (subSessionManager.isQueuingRequired()) {
                            throw new RuntimeException(StrUtil.format("子任务最大数量为{}, 请清理子任务列表！",
                                    SubSessionConstants.MAX_CONCURRENT_SESSIONS));
                        }


                        // 显示全屏加载
                        GroupChatMessageSender.System.showFullScreenLoading(primaryEngine, "");

                        // 创建子会话，在子画布中打开
                        String sessionName = StrUtil.isNotBlank(operationName) ? operationName : "打开画布";
                        SubSessionInstance subSession = subSessionManager
                                // 创建子会话
                                .createSubSession(groupChatInstance.getInstanceId(),
                                        agentDefinition,
                                        sessionName,
                                        SubSessionMode.ASYNC, primaryEngine
                                );

                        // 启动会话
                        String sessionId = subSession.getInstanceId();
                        GroupChatEngine subEngine = subSessionManager.startSubSession(sessionId);


                        String agentId = agentDefinition.getAgentId();

                        String welcomeText = null;
                        if (StrUtil.isNotBlank(operationName)) {
                            welcomeText = StrUtil.format("Hi，我已打开画布, 我可以为你做什么？", operationName);
                        } else {
                            welcomeText = StrUtil.format("Hi，我是{}, 我可以为你做什么？", agentDefinition.getAgentName());
                        }

                        // 欢迎消息
                        Message wecomeMessage = Message.createTextMessage(agentId,
                                welcomeText, null);

                        subSession.getMessageHistoryManager().addMessageOnlyHistory(wecomeMessage);
                        GroupChatMessageSender.Chat.aggregationBegin(subEngine, agentId);

                        // 发送打开画布的指令
                        GroupChatMessageSender.Canvas.open(subEngine, senderId, canvasType, canvasParamsObj,
                                subSession.getInstanceId());


                        // 下发预览指令
                        GroupChatMessageSender.Canvas.preview(primaryEngine, sessionId);

                        // 标记为等待回复
                        subSession.markAsWaitingReply();
                    } finally {
                        GroupChatMessageSender.System.closeFullScreenLoading(primaryEngine);

                    }


                }


            } else if (OperateMessageEnums.CANVAS_CLOSE.toString().equals(operationType)) {

                GroupChatMessageSender.Canvas.close(primaryEngine);

            } else {
                throw new VerifyException("不存在这样的操作类型");
            }

            return RespondDto.newSuccess("操作成功", null);
        } catch (RuntimeException e) {
            return RespondDto.newError("操作失败:" + ExceptionUtils.getFullStackTrace(e));


        }
    }

    // 前端注册动作
    // 不过目前前端主要是Canvas变动的时候进行注册
    // FIXME 前端RPC库似乎还不能很好的转换Dto，因此使用LinkedHashMap
    @SuppressWarnings("unused")
    default RespondDto registerCanvas(PanelContext panelContext,
                                      String groupChatInstId,
                                      LinkedHashMap currentCanvasStatusObj,
                                      String operationGuidance,
                                      List<LinkedHashMap> actionObjs) {
        try {

            assertGroupChatInstIsRunning(groupChatInstId);

            CanvasStatus currentCanvasStatus =
                    safeJsonToBean(currentCanvasStatusObj, CanvasStatus.class);

            List<FrontendActionDto> actions = new ArrayList<>();
            if (!Op.isEmpty(actionObjs)) {
                for (LinkedHashMap actionObj : actionObjs) {
                    FrontendActionDto action = safeJsonToBean(actionObj, FrontendActionDto.class);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }

            ConsolePrintUtil.printGreenLn(
                    StrUtil.format("前端注册动作, \ncurrentCanvasStatus:{}, \noperationGuidance.length:{}, \nactions.size:{}",
                            JSONUtil.toJsonStr(currentCanvasStatus), operationGuidance.length(), actions.size()
                    )
            );


            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);

            chatEngine.getFrontendActionManager()
                    .register(currentCanvasStatus, operationGuidance, actions, true);

            // 保存到缓存
            chatEngine.putEngineWorkCache(GCEngineWorkCacheKey.CURRENT_CANVAS_STATUS,
                    currentCanvasStatus
            );

            return RespondDto.newSuccess("注册成功", null);
        } catch (VerifyException e) {
            return RespondDto.newError("注册失败:" + ExceptionUtils.getFullStackTrace(e));

        }


    }


    // 前端提交动作执行结果
    @SuppressWarnings("unused")
    default RespondDto submitActionResponse(PanelContext panelContext,
                                            String groupChatInstId,
                                            String requestId,
                                            Object result) {
        try {

            FrontendAsyncCallService.submitResponse(requestId, result);

            return RespondDto.newSuccess("提交成功", null);
        } catch (VerifyException e) {
            return RespondDto.newError("提交失败:" + ExceptionUtils.getFullStackTrace(e));

        }


    }


    // 用户提交评价
    @SuppressWarnings("unused")
    default RespondDto submitTaskEvaluation(PanelContext panelContext,
                                            String groupChatInstId,
                                            List<String> relatedAgentIds,
                                            String messageContext,
                                            String category,
                                            String taskComment
    ) {

        try {
            if (Op.isEmpty(relatedAgentIds)) throw new VerifyException("涉及的智能体列表不得为空");
            if (StrUtil.isBlank(messageContext)) throw new VerifyException("选中的消息不得为空");

            assertGroupChatInstIsRunning(groupChatInstId);
            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);

            // 实际上反思节点并不需要智能体实例
            // 但是图引擎目前的设计需要通过智能体实例拿到实际生效的LLM配置
            // 因此暂时这样处理
            AgentInstance firstAgentInst = chatEngine.getGroupChatInstance()
                    .getAgent(relatedAgentIds.get(0));
            if (firstAgentInst == null) throw new VerifyException(StrUtil.format("AgentId[{}]对应的智能体实例不存在",
                    relatedAgentIds.get(0)));

            // 获取智能体的ID
            String agentId = firstAgentInst.getDefinition().getAgentId();

            // 将任务丢到常规任务线程池里
            CompletableFuture.runAsync(() -> {

                GraphEngine graphEngine = GraphEngine.Scene.newReflection(
                        new TaskEvaluationDto(
                                relatedAgentIds,
                                messageContext,
                                category,
                                taskComment
                        )
                );

                graphEngine.start(chatEngine, firstAgentInst);

            }, GroupChatThreadPollManager.get(ThreadPoolType.REGULAR_TASK));


            return RespondDto.newSuccess("提交成功", null);

        } catch (Exception e) {
            return RespondDto.newError("提交失败:" + ExceptionUtils.getFullStackTrace(e));

        }


    }


    // ========================= 支撑方法 =========================


    // 构建[群聊房间]所需的Dto
    default GroupChatRoomDto buildGroupChatRoomDto(GroupChatEngine chatEngine, boolean isNewRoom) throws Exception {

        DomainDto busDomain = chatEngine.getBusDomain();
        OctoDomainOpObserver octoDomainOpObserver = new OctoDomainOpObserver(busDomain);

        List<AgentInfoDto> agentList = new ArrayList<>();
        GroupChatInstance groupChatInstance = chatEngine.getGroupChatInstance();
        for (AgentInstance agentInstance : groupChatInstance.getAgentInstances().values()) {
            AgentDefinition definition = agentInstance.getDefinition();

            ToolRegistry toolRegistry = agentInstance.getToolRegistry();
            List<ToolDto> tools = Collections.emptyList();
            if (toolRegistry != null) {
                tools = toolRegistry.getToolDtos();
            }

            AgentInfoDto agentInfo = new AgentInfoDto(
                    definition.getAgentId(),
                    definition.getAgentAvatar(),
                    definition.getAgentName(),
                    definition.getAgentBusinessRole(),
                    definition.getAgentDescription(),
                    agentInstance.getStatus(),
                    definition.isSysScheduler(),
                    tools,
                    definition.isOnline()
            );
            agentList.add(agentInfo);
        }

        // 对智能体列表做一个固定的排序
        agentList = sortAgentList(agentList);

        GroupChatRoomDto chatRoomDto = new GroupChatRoomDto(
                isNewRoom,
                groupChatInstance.getInstanceId(),
                groupChatInstance.getDisplayName(),
                agentList,
                chatEngine.getTaskBoard().getAllTasks()
        );

        String applicationCode = ApplicationUtil.getDefaultPublishApplicationCode(octoDomainOpObserver);
        if (StrUtil.isBlank(applicationCode)) applicationCode = octoDomainOpObserver.getDomainCode();

        // 设置应用编号
        chatRoomDto.setAppCode(applicationCode);


        Message lastMessage = chatEngine.getMessageHistoryManager().getLastMessage();
        long lastMessageTimestamp = lastMessage == null ? 0 : lastMessage.getTimestamp();

        // 设置最后一条消息时间
        chatRoomDto.setLastMessageSendTimestamp(lastMessageTimestamp);


        return chatRoomDto;
    }


    // 检查是否为新房间
    default boolean checkNewRoom(GroupChatEngine chatEngine) {
        if (chatEngine == null) throw new VerifyException("无法检查是否为新房间，因为群聊引擎为空");

        MessageHistoryManager historyManager = chatEngine.getMessageHistoryManager();
        if (historyManager == null) throw new VerifyException("无法检查是否为新房间，因为群聊引擎的会话历史管理器为空");

        // 如果没有聊天记录，则认为为新房间
        if (historyManager.isEmpty()) {
            // 并且业务域的面板设计是空的
            if (!IVotaForgeService.get()
                    .isExistPanelDesign(chatEngine.getBusDomain().getDomainCode())) {
                return true;

            }

        }

        // 如果只有一条消息，并且 senderId 是系统默认的，则认为为新房间
        if (historyManager.size() == 1) {
            String senderId = historyManager.getLastMessage().getSenderId();
            return StrUtil.isNotBlank(senderId)
                    && senderId.equals(AppConstants.AGENT_ID_SYSTEM_DEFAULT);
        }

        return false;


    }


    // 安全JSON To Java Bean
    default <T> T safeJsonToBean(Object object, Class<T> beanClass) {
        try {
            return JSONUtil.toBean(JSONUtil.toJsonStr(object),
                    beanClass);
        } catch (Exception e) {

            ConsolePrintUtil.printRedLn(
                    StrUtil.format(
                            "转换失败(safeJsonToBean), object:\n{}\n, err:\n{}",
                            JSONUtil.toJsonStr(object),
                            ExceptionUtils.getFullStackTrace(e)
                    )
            );
            return null;
        }
    }

    @SuppressWarnings("unused")
    default RespondDto getMessageContext(PanelContext panelContext, String groupChatInstId, String msgId) {
        try {
            if (StrUtil.isBlank(groupChatInstId) || StrUtil.isBlank(msgId)) {
                return RespondDto.newError("参数不能为空");
            }

            GroupChatInstanceManager groupChatInstManager = GroupChatInstanceManager.getInstance();
            GroupChatInstance groupChatInst = groupChatInstManager.getGroupChatInstance(groupChatInstId);

            if (groupChatInst == null) {
                return RespondDto.newError("群聊实例不存在");
            }

            GroupChatEngine chatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInst.getInstanceId());
            MessageHistoryManager messageHistoryManager = chatEngine.getMessageHistoryManager();

            MessageContextDto contextDto = messageHistoryManager.getMessageContext(msgId);

            if (contextDto == null) {
                return RespondDto.newError("未找到该消息的上下文记录");
            }

            return RespondDto.newSuccess("查询成功", contextDto);

        } catch (Exception e) {
            Op.logException(e);
            return RespondDto.newError("查询消息上下文失败: " + e.getMessage());
        }
    }


    // 排序
    // FIXME 小trick了，随手写的固定顺序
    default List<AgentInfoDto> sortAgentList(List<AgentInfoDto> agentList) {
        if (Op.isEmpty(agentList) || agentList.size() == 1) return agentList;
        Map<String, Integer> seqMap = new HashMap<>();
        seqMap.put(AppConstants.AGENT_ID_SYSTEM_SCHEDULER, 1);
        seqMap.put(AppConstants.AGENT_ID_REQUIREMENT_STRUCTURING, 2);
        seqMap.put(AppConstants.AGENT_ID_ROUGH_DELIVERY, 3);
        seqMap.put(AppConstants.AGENT_ID_BASIC_DELIVERY, 4);
        seqMap.put(AppConstants.AGENT_ID_FINE_DECORATION, 5);
        seqMap.put(AppConstants.AGENT_ID_CODING, 6);

        AgentInfoDto[] arr = new AgentInfoDto[agentList.size()];
        List<AgentInfoDto> unknowList = new ArrayList<>();
        for (AgentInfoDto agentInfoDto : agentList) {
            Integer seq = seqMap.get(agentInfoDto.getAgentId());
            if (seq == null) {
                unknowList.add(agentInfoDto);
            } else {
                if (seq >= arr.length) {
                    unknowList.add(agentInfoDto);
                } else {
                    arr[seq] = agentInfoDto;
                }
            }
        }

        List<AgentInfoDto> result = new ArrayList<>();
        for (AgentInfoDto agentInfoDto : arr) {
            if (agentInfoDto != null) {
                result.add(agentInfoDto);
            }
        }
        result.addAll(unknowList);

        return result;

    }

}
