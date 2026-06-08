package cell.ai.agent;

import ai.agent.constant.AppConstants;
import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.taskboard.*;
import ai.agent.dto.groupChat.textStyle.ErrorItemDto;
import ai.agent.dto.groupChat.workCache.WorkCachePanelDesignDto;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.enums.NotificationEnums;
import ai.agent.service.groupChat.manager.GroupChatEngineManager;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import cell.ServiceCellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.octo.cm.service.IPanelDesignService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leavay.common.util.GsonUtil;
import fe.cmn.panel.PanelContext;
import gpf.adur.data.Form;
import octo.cm.util.EasyOperation;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Comment("交付任务前端服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-24", updateTime = "2025-12-24"
)
// cell.ai.agent.IDeliveryTaskFrontEndService
public interface IDeliveryTaskFrontEndService extends ServiceCellIntf {

    EasyOperation Op = EasyOperation.get();

    String STANDARD_TASK_NAME_DELIVERY = "标准交付任务";
    String STANDARD_TASK_DESCRIPTION_TEMPLATE = "对面板[{}({})]进行标准交付";
    String STANDARD_NOTICE_TASK_SUBMITTED = "标准交付任务已下发";
    String STANDARD_TASK_DESCRIPTION_SUBMITTED_FAILED = "任务提交失败";

    String CUSTOM_TASK_NAME_DELIVERY = "个性交付任务";
    String CUSTOM_TASK_DESCRIPTION_TEMPLATE = "对面板[{}({})]进行个性交付";
    String CUSTOM_NOTICE_TASK_SUBMITTED = "个性交付任务已下发";
    String CUSTOM_TASK_DESCRIPTION_SUBMITTED_FAILED = "任务提交失败";


    @SuppressWarnings("unused")
    default RespondDto executeStandardDelivery(PanelContext panelContext, String groupChatInstId,
                                               List<String> panelCodes, String userNeed) {

        GroupChatEngine chatEngine = null;
        String responsibleAgentId = AppConstants.AGENT_ID_BASIC_DELIVERY;
        try {
            if (CollUtil.isEmpty(panelCodes)) throw new RuntimeException("面板编号列表不能为空");

            if (StrUtil.isBlank(groupChatInstId)) throw new RuntimeException("群聊实例ID不能为空");

            chatEngine = GroupChatEngineManager.getGroupChatEngine(groupChatInstId);
            if (chatEngine == null) throw new RuntimeException("群聊实例ID不存在");

            GroupChatEngine finalChatEngine = chatEngine;
            String finalUserNeed = StrUtil.isBlank(userNeed) ? "" : userNeed;
            OctoDomainOpObserver octoDomainOpObserver = new OctoDomainOpObserver(finalChatEngine.getBusDomain());

            Map<String, TaskExecutionItemDto> tasks = new HashMap<>();
            List<CompletableFuture> futures = new ArrayList<>();
            List<Form> panelDesignForms = new ArrayList<>();

            // 构建任务列表
            try (IDao dao = IDaoService.newIDao()) {

                for (String panelCode : panelCodes) {
                    TaskExecutionItemDto taskItem = null;
                    try {
                        Form panelDesignForm = IPanelDesignService.get().getPanelDesign(dao, octoDomainOpObserver, panelCode);
                        if (panelDesignForm == null) throw new RuntimeException("无法找到该面板设计");
                        String panelName = panelDesignForm.getString("面板名称");
                        String taskDescription = StrUtil.format(STANDARD_TASK_DESCRIPTION_TEMPLATE, panelName, panelCode);
                        taskItem = GroupChatMessageSender.Task.newPending(STANDARD_TASK_NAME_DELIVERY, taskDescription);


                        StandardDeliveryParams deliveryParams = new StandardDeliveryParams()
                                .setGroupChatInstId(groupChatInstId)
                                .setPanelCodes(Collections.singletonList(panelCode))
                                .setUserNeed(finalUserNeed);

                        taskItem.setMetaInfo(new TaskExecutionMetaInfo()
                                .setType(TaskExecutionType.STANDARD_DELIVERY)
                                .setParams(deliveryParams)
                        );

                    } catch (Exception e) {

                        taskItem = GroupChatMessageSender.Task.newPending(STANDARD_TASK_NAME_DELIVERY, STANDARD_TASK_DESCRIPTION_SUBMITTED_FAILED)
                                .setStatus(TaskExecutionItemDto.Status.ERROR)
                                .setErrors(Collections.singletonList(new ErrorItemDto()
                                        .setIndex(1)
                                        .setErrorNameWithMaxLength(e.getMessage())
                                        .setErrorDesc(ExceptionUtils.getMessage(e))
                                ))
                        ;

                    }

                    tasks.put(panelCode, taskItem);


                }
            }


            // 发送任务构建的通知
            GroupChatMessageSender.Notice.hint(chatEngine, responsibleAgentId,
                    STANDARD_NOTICE_TASK_SUBMITTED);

            // 创建任务列表
            Message message = GroupChatMessageSender.Task.create(chatEngine, responsibleAgentId,
                    new ArrayList<>(tasks.values()));

            // 将任务丢到Future中运行
            for (Map.Entry<String, TaskExecutionItemDto> taskEntry : tasks.entrySet()) {
                String panelCode = taskEntry.getKey();
                TaskExecutionItemDto taskItem = taskEntry.getValue();
                CompletableFuture future = CompletableFuture.runAsync(() -> {
                    try {

                        // 标记任务正在执行
                        GroupChatMessageSender.Task.updateItem(finalChatEngine, responsibleAgentId,
                                message.getMsgId(),
                                taskItem.setStatus(TaskExecutionItemDto.Status.IN_PROGRESS)
                        );

                        // 实际执行标准交付
                        Form form = IVotaForgeService.get().standardDelivery(groupChatInstId, panelCode, finalUserNeed);
                        if (form == null) throw new RuntimeException("标准交付发布失败，未能产出面板设计Form");
                        panelDesignForms.add(form);

                        // 标记任务执行成功
                        GroupChatMessageSender.Task.updateItem(finalChatEngine, responsibleAgentId,
                                message.getMsgId(),
                                taskItem.setStatus(TaskExecutionItemDto.Status.COMPLETED)
                        );

                    } catch (Exception e) {

                        // 添加错误信息
                        taskItem.setErrors(Collections.singletonList(new ErrorItemDto()
                                .setIndex(1)
                                .setErrorNameWithMaxLength(e.getMessage())
                                .setErrorDesc(ExceptionUtils.getFullStackTrace(e))
                        ));

                        // 标记任务执行失败
                        GroupChatMessageSender.Task.updateItem(finalChatEngine, responsibleAgentId,
                                message.getMsgId(),
                                taskItem.setStatus(TaskExecutionItemDto.Status.ERROR)
                        );


                    }
                });
                futures.add(future);
            }


            CompletableFuture<Void> allDoneFuture =
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            // 阻塞等待所有任务完成
            allDoneFuture.thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList())).join();


            // 转换为dto传递出去
            List<WorkCachePanelDesignDto> dtos = convertPanelDesignFormToWorkCacheDto(panelDesignForms);

            GroupChatMessageSender.Notice.toast(chatEngine, responsibleAgentId,
                    NotificationEnums.SUCCESS, "标准交付任务已执行完毕");

            return RespondDto.newSuccess("标准交付任务已执行完毕", dtos);


        } catch (Exception e) {
            if (chatEngine != null) {
                GroupChatMessageSender.StyleText.error(chatEngine, responsibleAgentId,
                        Collections.singletonList(new ErrorItemDto()
                                .setErrorName("标准交付任务没有下发或执行成功")
                                .setErrorDesc(e.getMessage())
                                .setIndex(1))
                );

            }

            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return RespondDto.newError("标准发布失败: " + e.getMessage());
        }
    }


    @SuppressWarnings("unused")
    default RespondDto executeCustomDelivery(PanelContext panelContext, String groupChatInstId,
                                             List<String> panelCodes, String userNeed) {

        GroupChatEngine chatEngine = null;
        String responsibleAgentId = AppConstants.AGENT_ID_FINE_DECORATION;
        try {
            if (CollUtil.isEmpty(panelCodes)) throw new RuntimeException("面板编号列表不能为空");

            if (StrUtil.isBlank(groupChatInstId)) throw new RuntimeException("群聊实例ID不能为空");

            chatEngine = GroupChatEngineManager.getGroupChatEngine(groupChatInstId);
            if (chatEngine == null) throw new RuntimeException("群聊实例ID不存在");

            GroupChatEngine finalChatEngine = chatEngine;
//            String finalUserNeed = StrUtil.isBlank(userNeed) ? "" : userNeed;
            String finalUserNeed = "";
            OctoDomainOpObserver octoDomainOpObserver = new OctoDomainOpObserver(finalChatEngine.getBusDomain());

            Map<String, TaskExecutionItemDto> tasks = new HashMap<>();
            List<CompletableFuture> futures = new ArrayList<>();

            // 构建任务列表
            try (IDao dao = IDaoService.newIDao()) {

                for (String panelCode : panelCodes) {
                    TaskExecutionItemDto taskItem = null;
                    try {
                        Form panelDesignForm = IPanelDesignService.get().getPanelDesign(dao, octoDomainOpObserver, panelCode);
                        if (panelDesignForm == null) throw new RuntimeException("无法找到该面板设计");
                        String panelName = panelDesignForm.getString("面板名称");
                        taskItem = GroupChatMessageSender.Task.newPending(CUSTOM_TASK_NAME_DELIVERY,
                                StrUtil.format(CUSTOM_TASK_DESCRIPTION_TEMPLATE, panelName, panelCode));

                        CustomDeliveryParams deliveryParams = new CustomDeliveryParams()
                                .setGroupChatInstId(groupChatInstId)
                                .setPanelCodes(Collections.singletonList(panelCode))
                                .setUserNeed(finalUserNeed);

                        taskItem.setMetaInfo(new TaskExecutionMetaInfo()
                                .setType(TaskExecutionType.CUSTOM_DELIVERY)
                                .setParams(deliveryParams)
                        );

                    } catch (Exception e) {

                        taskItem = GroupChatMessageSender.Task.newPending(CUSTOM_TASK_NAME_DELIVERY, CUSTOM_TASK_DESCRIPTION_SUBMITTED_FAILED)
                                .setStatus(TaskExecutionItemDto.Status.ERROR)
                                .setErrors(Collections.singletonList(new ErrorItemDto()
                                        .setIndex(1)
                                        .setErrorNameWithMaxLength(e.getMessage())
                                        .setErrorDesc(ExceptionUtils.getMessage(e))
                                ))
                        ;

                    }

                    tasks.put(panelCode, taskItem);


                }
            }


            // 发送任务构建的通知
            GroupChatMessageSender.Notice.hint(chatEngine, responsibleAgentId,
                    CUSTOM_NOTICE_TASK_SUBMITTED);

            // 创建任务列表
            Message message = GroupChatMessageSender.Task.create(chatEngine, responsibleAgentId,
                    new ArrayList<>(tasks.values()));

            // 将任务丢到Future中运行
            for (Map.Entry<String, TaskExecutionItemDto> taskEntry : tasks.entrySet()) {
                String panelCode = taskEntry.getKey();
                TaskExecutionItemDto taskItem = taskEntry.getValue();
                CompletableFuture future = CompletableFuture.runAsync(() -> {
                    try {

                        // 标记任务正在执行
                        GroupChatMessageSender.Task.updateItem(finalChatEngine, responsibleAgentId,
                                message.getMsgId(),
                                taskItem.setStatus(TaskExecutionItemDto.Status.IN_PROGRESS)
                        );

                        // 实际执行个性交付
                        IVotaForgeService.get().customDelivery(groupChatInstId, panelCode, finalUserNeed);

                        // 标记任务执行成功
                        GroupChatMessageSender.Task.updateItem(finalChatEngine, responsibleAgentId,
                                message.getMsgId(),
                                taskItem.setStatus(TaskExecutionItemDto.Status.COMPLETED)
                        );

                    } catch (Exception e) {

                        // 添加错误信息
                        taskItem.setErrors(Collections.singletonList(new ErrorItemDto()
                                .setIndex(1)
                                .setErrorNameWithMaxLength(e.getMessage())
                                .setErrorDesc(ExceptionUtils.getFullStackTrace(e))
                        ));

                        // 标记任务执行失败
                        GroupChatMessageSender.Task.updateItem(finalChatEngine, responsibleAgentId,
                                message.getMsgId(),
                                taskItem.setStatus(TaskExecutionItemDto.Status.ERROR)
                        );


                    }
                });
                futures.add(future);
            }


            CompletableFuture<Void> allDoneFuture =
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            // 阻塞等待所有任务完成
            allDoneFuture.thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList())).join();

            GroupChatMessageSender.Notice.toast(chatEngine, responsibleAgentId,
                    NotificationEnums.SUCCESS, "个性交付任务已执行完毕");

            return RespondDto.newSuccess("个性交付任务已执行完毕", null);


        } catch (Exception e) {
            if (chatEngine != null) {
                GroupChatMessageSender.StyleText.error(chatEngine, responsibleAgentId,
                        Collections.singletonList(new ErrorItemDto()
                                .setErrorName("个性交付任务没有下发或执行成功")
                                .setErrorDesc(e.getMessage())
                                .setIndex(1))
                );

            }

            ConsolePrintUtil.printRedLn(ExceptionUtils.getFullStackTrace(e));
            return RespondDto.newError("个性发布失败: " + e.getMessage());
        }
    }



    // 根据任务元信息重试任务
    @SuppressWarnings("unused")
//    default RespondDto retryTask(PanelContext panelContext, TaskExecutionMetaInfo metaInfo) {
    default RespondDto retryTask(PanelContext panelContext, LinkedHashMap  metaInfoObj) {
        try {
            if(metaInfoObj== null) throw new RuntimeException("任务元信息不能为空");
//            TaskExecutionMetaInfo metaInfo = JSONUtil.toBean(JSONUtil.toJsonStr(metaInfoObj), TaskExecutionMetaInfo.class);
            TaskExecutionMetaInfo metaInfo = GsonUtil.fromJson(GsonUtil.toJson(metaInfoObj), TaskExecutionMetaInfo.class);
            if (metaInfo == null) throw new RuntimeException("任务元信息不能为空");
            if (metaInfo.getType() == null) throw new RuntimeException("任务类型不能为空");
            if (metaInfo.getParams() == null) throw new RuntimeException("任务参数不能为空");

            switch (metaInfo.getType()) {
                case STANDARD_DELIVERY:
                    StandardDeliveryParams standardParams = metaInfo.getTypedParams(StandardDeliveryParams.class);
                    return executeStandardDelivery(panelContext,
                            standardParams.getGroupChatInstId(),
                            standardParams.getPanelCodes(),
                            standardParams.getUserNeed());

                case CUSTOM_DELIVERY:
                    CustomDeliveryParams customParams = metaInfo.getTypedParams(CustomDeliveryParams.class);
                    return executeCustomDelivery(panelContext,
                            customParams.getGroupChatInstId(),
                            customParams.getPanelCodes(),
                            customParams.getUserNeed());

                default:
                    throw new RuntimeException("不支持的任务类型: " + metaInfo.getType());
            }
        } catch (Exception e) {
            return RespondDto.newError("任务重试失败: " + e.getMessage());
        }
    }


    // ========================= 支撑方法 =========================


    // 从Form转换为缓存Dto
    static List<WorkCachePanelDesignDto> convertPanelDesignFormToWorkCacheDto(List<Form> llmInitedPanelDesigns) {
        List<WorkCachePanelDesignDto> dtos = new ArrayList<>();
        if (CollUtil.isNotEmpty(llmInitedPanelDesigns)) {
            for (Form llmInitedPanelDesign : llmInitedPanelDesigns) {
                WorkCachePanelDesignDto dto = WorkCachePanelDesignDto.newDto(llmInitedPanelDesign);
                if (dto != null) {
                    dtos.add(dto);
                }
            }
        }
        return dtos;
    }


}
