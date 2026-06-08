package cell.ai.agent;


import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.service.groupChat.manager.GroupChatEngineManager;
import bap.cells.Cells;
import cell.ServiceCellIntf;
import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.util.EasyOperation;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
import org.jetbrains.annotations.NotNull;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;
import java.util.stream.Collectors;

@Comment("群聊基础服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-05", updateTime = "2025-09-05"
)
// cell.ai.agent.IGroupChatBasicService
public interface IGroupChatBasicService extends ServiceCellIntf {
    static IGroupChatBasicService get() {
        return Cells.get(IGroupChatBasicService.class);
    }

    EasyOperation Op = EasyOperation.get();


    @NotNull
    default List<String> doQueryDomainAllFormCodes(IDao dao, OctoDomainOpObserver octoDomainOpObserver, String formModelId) throws Exception {

        Cnd queryCnd = Op.getBusDomainFilterCondition(octoDomainOpObserver, formModelId);
        ResultSet<Form> sceneRs = IFormMgr.get().queryFormPage(dao, formModelId, queryCnd, 1, Integer.MAX_VALUE, false, false);

        if (sceneRs.isEmpty()) return CollUtil.newArrayList();

        return sceneRs.getDataList().stream().map(
                form -> {
                    try {
                        return form.getString(Form.Code);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        ).collect(Collectors.toList());
    }

    // 检查实例是否存在，最大 5 * 3秒
    default void assertGroupChatInstIsRunning(String groupChatInstId) {
        try {
            boolean isExistedGroupChatInst = false;
            for (int i = 0; i < 5; i++) {
                if (GroupChatEngineManager.isExistedCurrentGroupChatInst(groupChatInstId)) {
                    isExistedGroupChatInst = true;
                    break;
                }
                Thread.sleep(3000);

            }

            if (!isExistedGroupChatInst) {
                throw new RuntimeException(StrUtil.format("对应的群聊实例不存在:[{}]", groupChatInstId));
            }
        } catch (Exception e) {
//            Op.logException(e);
            throw new RuntimeException(e.getMessage());
        }
    }


    @NotNull
    default GroupChatEngine getGroupChatEngine(String groupChatInstId) {
        GroupChatEngine chatEngine = GroupChatEngineManager.getGroupChatEngine(groupChatInstId);
        if (chatEngine == null)
            throw new RuntimeException(StrUtil.format("无法找到群聊实例[{}]对应的群聊引擎", groupChatInstId));
        return chatEngine;
    }


    @NotNull
    default OctoDomainOpObserver getOctoDomainOpObserver(GroupChatEngine chatEngine) {
        if (chatEngine == null) throw new RuntimeException("无法找到对应的群聊引擎");
        DomainDto busDomain = chatEngine.getBusDomain();
        if (busDomain == null) throw new RuntimeException("从群聊引擎中无法找到对应的业务域");
        return new OctoDomainOpObserver(
                busDomain.getDomainCode(),
                busDomain.getDomainUuid()
        );
    }


}
