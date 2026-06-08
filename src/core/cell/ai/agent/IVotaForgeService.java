package cell.ai.agent;


import ai.agent.constant.AppConstants;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToastProgressAdapter;
import ai.agent.enums.ThreadPoolType;
import ai.agent.service.GroupChatThreadPollManager;
import ai.agent.service.groupChat.manager.GroupChatEngineManager;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatFileUtil;
import ai.agent.util.groupChat.GroupChatMessageSender;
import bap.cells.Cells;
import cell.ServiceCellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.llm.IHtmlGeneratorAction;
import cell.octo.cm.service.IPanelDesignLLMSupportService;
import cell.octo.cm.service.IPanelDesignService;
import cell.octocm.domain.service.IDomainService;
import cell.octocm.workbench.app.IApplicationDeploy;
import cell.productionLine.service.IProductionLineRuntimeService;
import cell.rda.service.IRdaService;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cmn.exception.MultiException;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AttachData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.constant.WorkBenchConst;
import octo.cm.enums.DefaultSystemModule;
import octo.cm.exception.business.DomainException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.util.PanelCategoryUtil;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import productionLine.dtos.runtime.WorkplaceRuntimeStatusDto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Comment("VotaForge服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-05", updateTime = "2025-09-05"
)
// cell.ai.agent.IVotaForgeService
public interface IVotaForgeService extends ServiceCellIntf, IGroupChatBasicService {

    static IVotaForgeService get() {
        return Cells.get(IVotaForgeService.class);
    }


    // 群聊业务域名称模板
    String BUS_DOMAIN_NAME_TEMPLATE = "群聊会话实例_{}";
    // 默认父级业务域
    String DEFAULT_BUS_DOMAIN_PARENT = "OctoCM_workbench";
    // 逻辑删除标志位
    String LOGICAL_DELETE_FLAG = "#del#";


    // 获取业务域
    default DomainDto getBusDomain(String groupChatInstId) {
        try {
            String domainName = buildDomainCode(groupChatInstId);
            IDomainService domainService = IDomainService.get();
            return domainService.getDomainByName(domainName);
        } catch (Exception e) {
            return null;
        }
    }

    // 获取OctoDomainOpObserver
    default OctoDomainOpObserver getOctoDomainOpObserver(String groupChatInstId) {
        if (StrUtil.isBlank(groupChatInstId)) throw DomainException.Builder.busDomainCodeEmpty();
        DomainDto busDomain = getBusDomain(groupChatInstId);
        if (busDomain == null) throw DomainException.Builder.notFoundWithCode(groupChatInstId);
        return new OctoDomainOpObserver(busDomain);

    }

    // 创建业务域
    default DomainDto getOrCreateBusDomain(String groupChatInstId) throws Exception {

        GroupChatEngine groupChatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);
        if (groupChatEngine == null) throw new RuntimeException("创建业务域失败，系统不存在当前群聊实例");
        String domainName = buildDomainCode(groupChatInstId);
        String domainLabel = buildDomainLabel(groupChatInstId);

        IDomainService domainService = IDomainService.get();
        DomainDto existedDomain = domainService.getDomainByName(domainName);

        if (existedDomain != null) {
            // 更新到引擎
            groupChatEngine.setBusDomain(existedDomain);
            return existedDomain;
        }

        // 自己新建一个
        DomainDto domain = domainService.createDomain(
                DEFAULT_BUS_DOMAIN_PARENT,
                domainName,
                domainLabel,
                "由多智能体群聊会话创建出的业务域"
        );

        if (domain == null) throw new RuntimeException("VotaForge创建业务域失败");

        OctoDomainOpObserver octoDomainOpObserver = new OctoDomainOpObserver(domain);

        // 初始化应用
        IApplicationDeploy.get().initDomainDefaultApp(
                null,
                domain,
                domain.getDomainCode(),
                domainLabel
        );

        // 将任务丢到常规任务线程池里
        CompletableFuture.runAsync(() -> {

            try {
                ConsolePrintUtil.printGreenLn("开始初始化默认面板设计...");

                // 加载默认[系统模块]面板设计
                IPanelDesignService.get().loadDefaultPanelDesign(octoDomainOpObserver, DefaultSystemModule.DASHBOARD);
                IPanelDesignService.get().loadDefaultPanelDesign(octoDomainOpObserver, DefaultSystemModule.ORGANIZATION_MANAGEMENT);


            } catch (Exception e) {
                ConsolePrintUtil.printRedLn(
                        StrUtil.format("初始化默认面板设计失败，原因为:\n{}", ExceptionUtils.getFullStackTrace(e))
                );
            }

        }, GroupChatThreadPollManager.get(ThreadPoolType.REGULAR_TASK));


        // 更新到引擎
        groupChatEngine.setBusDomain(domain);

        return domain;


    }

    // 删除业务域
    default void deleteBusDomain(String groupChatInstId) throws Exception {


        try {
            String domainName = buildDomainCode(groupChatInstId);
            IDomainService domainService = IDomainService.get();
            DomainDto existedDomain = domainService.getDomainByName(domainName);
            if (existedDomain == null) return;
            String domainUuid = existedDomain.getDomainUuid();
            domainService.deleteDomain(Progress.newOutput(), domainUuid);
        } catch (Exception e){
            Op.logException(e);
        }


    }

    // 判断删除任务是否完成
    default boolean isUnFinishedDomainDeleteTask(String groupChatInstId) {
//        GroupChatEngine groupChatEngine = GroupChatEngineManager.getOrCreateChatEngine(groupChatInstId);
//        if (groupChatEngine == null) throw new RuntimeException("创建业务域失败，系统不存在当前群聊实例");
//        String domainName = buildDomainName(groupChatInstId);
//
//        IDomainService domainService = IDomainService.get();
//        DomainDto existedDomain = domainService.getDomainByName(domainName);

        return true;

    }

    // 导入文档
    default List<String> importDocument(String groupChatInstId, String fileCode) throws Exception {
        GroupChatEngine chatEngine = getGroupChatEngine(groupChatInstId);
        try (IDao dao = IDaoService.newIDao()) {
            List<AttachData> attachments = GroupChatFileUtil.getAttachments(dao, fileCode);
            if (Op.isEmpty(attachments)) throw new RuntimeException("无法导入文档，因为附件为空");


            DomainDto busDomain = chatEngine.getBusDomain();
            if (busDomain == null) throw new RuntimeException("当前会话尚未注册业务域");


            OctoDomainOpObserver observer = getOctoDomainOpObserver(chatEngine);

            // 这里可以自己写一个progress的实现类
            return IRdaService.get().importAndParseFile(null,
                    attachments, observer);


        }
    }

    // 下发文档解析任务，返回解析任务的流程实例
    default List<String> issuedParseDocumentTask(String groupChatInstId, List<String> docCodes) throws Exception {

        GroupChatEngine chatEngine = getGroupChatEngine(groupChatInstId);
        OctoDomainOpObserver observer = getOctoDomainOpObserver(chatEngine);
        return IRdaService.get().documentSemanticParsing(docCodes,
                null, observer.getDomainCode());

    }

    // 查看文档解析任务执行状态
    default String queryParseDocumentTaskExecuteDetailStr(String groupChatInstId, List<String> processTaskIds) throws Exception {
        if (Op.isEmpty(processTaskIds)) return null;

        Map<String /* 节点名称 */, Boolean /* 数量 */> statusCountMap = queryParseDocumentTaskExecuteDetail(groupChatInstId,
                processTaskIds);
        if (statusCountMap == null) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> nodeStatusEntry : statusCountMap.entrySet()) {
            String key = nodeStatusEntry.getKey();
            Boolean value = nodeStatusEntry.getValue();

            sb.append(
                    StrUtil.format("节点名称:{}, 是否结束:{}\n", key,
                            (value == null || !value) ? "未开始或进行中" : "已完成"
                    )
            );


        }

        return sb.toString();


    }

    // 查看文档解析任务执行状态
    default Map<String, Boolean> queryParseDocumentTaskExecuteDetail(String groupChatInstId, List<String> processTaskIds) throws Exception {
        if (Op.isEmpty(processTaskIds)) return null;


        Map<String /* 节点名称 */, Boolean /* 是否结束 */> statusCountMap = new HashMap<>();

        for (String processTaskId : processTaskIds) {
            List<WorkplaceRuntimeStatusDto> statusDtos = IProductionLineRuntimeService.get()
                    .queryWorkplaceRuntimeStatus(processTaskId);
            if (Op.isEmpty(statusDtos)) continue;

            for (WorkplaceRuntimeStatusDto statusDto : statusDtos) {
                String nodeName = statusDto.getNodeName();
                if (StrUtil.isBlank(nodeName)) continue;

                statusCountMap.put(nodeName, statusDto.isFinish());
            }
        }
        return statusCountMap;


    }

    // 流程是否结束
    default boolean isFinishParseDocumentTask(List<String> processTaskIds) throws Exception {
        if (Op.isEmpty(processTaskIds)) return true;
        for (String processTaskId : processTaskIds) {
            boolean isDone = IProductionLineRuntimeService.get()
                    .isProductionLineFinished(processTaskId);
            // 有一个没完成就抛出去
            if (!isDone) {
                return false;
            }

        }
        return true;

    }

    // 流程是否结束
    default boolean isFinishParseDocumentTaskByNodeChecking(List<String> processTaskIds) throws Exception {
        if (Op.isEmpty(processTaskIds)) return true;

        try {
            Map<String, Boolean> statusMap = queryParseDocumentTaskExecuteDetail(null, processTaskIds);
            if (statusMap == null) return true;
            return statusMap.getOrDefault("结束", false);
        } catch (Exception e) {
            Op.logException(e);
            return false;
        }

    }

    // 发布解析之后的结果
    default void publishDocumentParsedResult(String groupChatInstId, List<String> processTaskIds) throws Exception {
        if (Op.isEmpty(processTaskIds)) throw new RuntimeException("不存在要发布的文档解析结果");
        GroupChatEngine chatEngine = getGroupChatEngine(groupChatInstId);

        OctoDomainOpObserver observer = getOctoDomainOpObserver(chatEngine);

        for (String processTaskId : processTaskIds) {
            IRdaService.get().publishCm(processTaskId, observer, observer.getDomainCode());
        }

    }

    // 将VotaForge中的场景发布到应用
    default List<Form> publishSceneToDefaultApplication(String groupChatInstId) throws Exception {
        GroupChatEngine chatEngine = getGroupChatEngine(groupChatInstId);
        OctoDomainOpObserver octoDomainOpObserver = getOctoDomainOpObserver(chatEngine);

        try (IDao dao = IDaoService.newIDao()) {
            String formModelId = WorkBenchConst.FormModelId_SceneLayer;
            List<String> sceneCodes = doQueryDomainAllFormCodes(dao, octoDomainOpObserver, formModelId);
            if (Op.isEmpty(sceneCodes)) {
                // 可能还没有发不完，等待十秒
                Thread.sleep(10_000);
                sceneCodes = doQueryDomainAllFormCodes(dao, octoDomainOpObserver, formModelId);
            }

            if (Op.isEmpty(sceneCodes)) {
                GroupChatMessageSender.Notice.hint(chatEngine, null,
                        StrUtil.format("没有发现任何可用的场景，请检查意图确认"));
                return null;

            }


            String progressId = IdUtil.fastSimpleUUID();
            GroupChatMessageSender.Progress.open(chatEngine, progressId, "原型发布中");

            try {

                Progress progress = GroupChatToastProgressAdapter.newProgress(chatEngine, progressId);

                List<Form> panelDesignForms = IPanelDesignService.get().publishSceneToDefaultApplicationBatch(
                        progress,
                        octoDomainOpObserver,
                        sceneCodes,
                        true
                );


                if (Op.isEmpty(panelDesignForms)) {
                    GroupChatMessageSender.Notice.hint(chatEngine, null,
                            StrUtil.format("似乎没有发布出任何面板，请检查意图确认"));

                    Thread.sleep(1000);
                }


                return panelDesignForms;


            } finally {

                GroupChatMessageSender.Progress.close(chatEngine, progressId);

            }


        }


    }

    // 使用大模型初始化面板并发布到应用
    default List<Form> llmInitAndPublishToDefaultApplication(String groupChatInstId, List<String> panelDesignCodes, String userNeed) throws Exception {
        GroupChatEngine chatEngine = getGroupChatEngine(groupChatInstId);
        OctoDomainOpObserver octoDomainOpObserver = getOctoDomainOpObserver(chatEngine);

        String progressId = IdUtil.fastSimpleUUID();
        GroupChatMessageSender.Progress.open(chatEngine, progressId, "标准发布中");

        try {

            Progress progress = GroupChatToastProgressAdapter.newProgress(chatEngine, progressId);

            List<Form> panelDesignForms = IPanelDesignLLMSupportService.get().llmInitPanelDesignAndPublishBatch(
                    progress,
                    octoDomainOpObserver,
                    panelDesignCodes,
                    userNeed
            );

            if (Op.isEmpty(panelDesignForms)) {
                GroupChatMessageSender.Progress.addMsg(chatEngine, progressId, "似乎没有发布出任何面板...");
                Thread.sleep(1000);
            }

            return panelDesignForms;


        } finally {

            GroupChatMessageSender.Progress.close(chatEngine, progressId);

        }


    }


    // 标准交付
    default Form standardDelivery(String groupChatInstId, String panelCode, String userNeed) throws Exception {
        OctoDomainOpObserver observer = getOctoDomainOpObserver(groupChatInstId);
//        if (!TestLockUtil.STANDARD_DELIVERY_LOCK.tryLock(10, TimeUnit.MINUTES)) {
//            throw new RuntimeException("等待锁超时");
//        }

        try {

            return IPanelDesignLLMSupportService.get()
                    .llmInitPanelDesignAndPublish(null, observer, panelCode, userNeed);
        } finally {
//            TestLockUtil.STANDARD_DELIVERY_LOCK.unlock();
        }


    }

    // 个性交付
    default void customDelivery(String groupChatInstId, String panelCode, String userNeed) throws Exception {

        OctoDomainOpObserver observer = getOctoDomainOpObserver(groupChatInstId);

        try (IDao dao = IDaoService.newIDao()) {
            Form panelDesign = IPanelDesignService.get().getPanelDesign(dao, observer, panelCode);
            if (panelDesign == null) throw PanelDesignException.Builder.notFoundWithCode(panelCode);

            String finalResult = null;
            String panelDescription = panelDesign.getString("面板描述");

            boolean isDashBoard = PanelCategoryUtil.isDashBoard(panelDesign);
            if (isDashBoard) {
                finalResult = IHtmlGeneratorAction.get()
                        .generateCDPCustomWithDashBoardVersion(observer.getDomainCode(), panelCode, panelDescription, userNeed
                        );
            } else {
                finalResult = IHtmlGeneratorAction.get()
                        .generateCDPCustomWithNormalVersion(observer.getDomainCode(), panelCode, panelDescription, userNeed
                        );
            }

//            panelDesign = IPanelDesignService.get().savePanelWebPageWithCustomHtml(dao, observer, panelCode, finalResult);

            dao.commit();

            // 直接发布
            IPanelDesignService.get().publishPanelDesignToDefaultApplicationBatch(
                    null,
                    observer,
                    Collections.singletonList(
                            panelDesign
                    )
            );


        }


    }


    // 获取当前场景所有的PanelDesign
    // FIXME 这里应该是BusDomainCode去查，而不是与实例ID去绑定
    default List<Form> queryAllPanelDesign(String groupChatInstId) throws Exception {

        OctoDomainOpObserver octoDomainOpObserver = getOctoDomainOpObserver(groupChatInstId);

        try (IDao dao = IDaoService.newIDao()) {
            String formModelId = WorkBenchConst.FormModelId_PanelDesign;
            Cnd cnd = Op.getBusDomainFilterCondition(octoDomainOpObserver, formModelId);
            cnd.where().andNotLike(Op.getFieldCode("面板编号"), LOGICAL_DELETE_FLAG);
            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, cnd, 1, Integer.MAX_VALUE, true, true);

            return queryRs.getDataList();

        }

    }

    // 检查当前业务域是否存在面板设计
    default boolean isExistPanelDesign(String groupChatInstId) {

        OctoDomainOpObserver octoDomainOpObserver = getOctoDomainOpObserver(groupChatInstId);

        try (IDao dao = IDaoService.newIDao()) {
            String formModelId = WorkBenchConst.FormModelId_PanelDesign;
            Cnd cnd = Op.getBusDomainFilterCondition(octoDomainOpObserver, formModelId);
            cnd.where().andNotLike(Op.getFieldCode("面板编号"), LOGICAL_DELETE_FLAG);
            return IFormMgr.get().countForm(dao, formModelId, cnd) > 0;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    // 发布面板设计
    default void publishPanelDesign(String groupChatInstId, String panelCode, String targetPageEntry) throws Exception {

        GroupChatEngine chatEngine = getGroupChatEngine(groupChatInstId);
        OctoDomainOpObserver octoDomainOpObserver = getOctoDomainOpObserver(chatEngine);
        try (IDao dao = IDaoService.newIDao()) {
            Form panelDesign = IPanelDesignService.get().getPanelDesign(dao, octoDomainOpObserver, panelCode);
            if (panelDesign == null)
                throw new RuntimeException(StrUtil.format("无法找到对应的面板设计[{}]", panelCode));


            // 面板名称
            String panelName = panelDesign.getString("面板名称");
            // 更新【页面入口】
            panelDesign.setAttrValue("页面入口", targetPageEntry);


            IPanelDesignService.get().publishPanelDesignToDefaultApplicationBatch(null, octoDomainOpObserver,
                    CollUtil.newArrayList(panelDesign));


        } catch (Exception e) {
            Op.logException(e);
            if (e instanceof MultiException) throw e;
            throw new RuntimeException(e.getMessage());
        }


    }

    // 删除面板设计
    default void deletePanelDesign(String groupChatInstId, String panelCode) throws Exception {
        GroupChatEngine chatEngine = getGroupChatEngine(groupChatInstId);
        OctoDomainOpObserver octoDomainOpObserver = getOctoDomainOpObserver(chatEngine);
        try (IDao dao = IDaoService.newIDao()) {
            String formModelId = WorkBenchConst.FormModelId_PanelDesign;
            Cnd cnd = Op.getBusDomainFilterCondition(octoDomainOpObserver, formModelId);
            cnd.where().andEquals(Op.getFieldCode("面板编号"), panelCode);

            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, cnd, 1, 1, false, false);
            if (queryRs.isEmpty()) return;

            Form panelDesignForm = queryRs.getDataList().get(0);
            panelDesignForm.setAttrValue("面板编号",
                    StrUtil.format("{}{}", LOGICAL_DELETE_FLAG, panelCode));
            panelDesignForm.setAttrValue("面板名称",
                    StrUtil.format("{}{}", LOGICAL_DELETE_FLAG, panelDesignForm.getString("面板名称")));

            IFormMgr.get().updateForm(null, dao, panelDesignForm, octoDomainOpObserver);

            dao.commit();
        }

    }


    // ========================= 支撑方法 =========================


    // 构建业务域标签（名称）
    default String buildDomainLabel(String groupChatInstId) {
        return StrUtil.format(BUS_DOMAIN_NAME_TEMPLATE, groupChatInstId);
    }

    // 构建业务域编号
    default String buildDomainCode(String groupChatInstId) {

        // 之前是群聊实例只有时间戳作为实例编号，后续又移除了这个设计
        // 后续应该对群聊实例创建这块进行一个完整的重构
        if (true) return groupChatInstId;
        return StrUtil.format("{}{}", AppConstants.GROUP_INSTANCE_PREFIX, groupChatInstId);
    }

    // 构建应用名称，目前默认使用业务域创建时默认创建的应用进行发布
    default String buildApplicationName(String groupChatInstId) {
        return buildDomainCode(groupChatInstId);
    }


}
