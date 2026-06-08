package cell.ai.agent;


import ai.agent.dto.groupChat.AgentChatRecordDto;
import bap.cells.Cells;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octocm.domain.dto.DomainDto;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Comment("群聊消息服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-10", updateTime = "2025-09-10"
)
// cell.ai.agent.IGroupChatMessageService
public interface IGroupChatMessageService extends IGroupChatBasicService {
    static IGroupChatMessageService get() {
        return Cells.get(IGroupChatMessageService.class);
    }


    // 添加用户聊天记录
    default void add(DomainDto domainDto, String msgCode, String msgType,
                     String msgAttachmentFileCode, String msgContent) {

        if (domainDto == null) return;

        AgentChatRecordDto agentChatRecordDto = new AgentChatRecordDto()
                .setSourceBusDomain(domainDto.getDomainCode())
                .setMsgCode(msgCode)
                .setMsgType(msgType)
                .setMsgContent(msgContent)
                .setMsgAttachmentFileCode(msgAttachmentFileCode)
                .setCreateTime(System.currentTimeMillis());

        try (IDao dao = IDaoService.newIDao()) {
            Form form = agentChatRecordDto.toForm();
            form.setAttrValue(Form.Owner, domainDto.getDomainUuid());
            IFormMgr.get().createForm(dao, form);
            dao.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    // 获取全部聊天记录
    default List<AgentChatRecordDto> queryAll() {
        return queryAllByBusDomainCode(null);

    }

    // 获取全部聊天记录
    default List<AgentChatRecordDto> queryAllByBusDomainCode(String sourceBusDomainCode) {
        try (IDao dao = IDaoService.newIDao()) {
            Cnd cnd = Cnd.NEW();
            if (StrUtil.isNotBlank(sourceBusDomainCode)) {
                cnd.where().andEquals(
                        Op.getFieldCode("来源业务域"),
                        sourceBusDomainCode
                );
            }
            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, AgentChatRecordDto.FORM_MODEL_ID, cnd, 1, Integer.MAX_VALUE, true, true);
            if (queryRs.isEmpty()) return new ArrayList<>();

            return queryRs.getDataList()
                    .stream().map(AgentChatRecordDto::newDto)
                    .collect(Collectors.toList());


        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    // 删除聊天记录根据消息编号
    default void removeByMsgCodes(IDao dao, String sourceBusDomainCode, List<String> msgCodes) throws Exception {
        if (StrUtil.hasBlank(sourceBusDomainCode) || Op.isEmpty(msgCodes)) return;

        Cnd cnd = Cnd.NEW();
        cnd.where().andInStrList(Op.getFieldCode("信息编号"), msgCodes)
                .andEquals(Op.getFieldCode("来源业务域"), sourceBusDomainCode);

        IFormMgr.get().deleteForm(dao, AgentChatRecordDto.FORM_MODEL_ID, cnd);


    }


    // 删除全部聊天记录
    default void removeAllByBusDomainCode(IDao dao, String sourceBusDomainCode) {

        if (StrUtil.isBlank(sourceBusDomainCode)) throw new RuntimeException("来源业务域不得为空");
        try {
            Cnd cnd = Cnd.NEW();
            cnd.where().andEquals(
                    Op.getFieldCode("来源业务域"),
                    sourceBusDomainCode
            );
            IFormMgr.get().deleteForm(dao, AgentChatRecordDto.FORM_MODEL_ID, cnd);
        } catch (Exception e) {
            Op.logException(e);
        }


    }


}
