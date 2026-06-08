package cell.ai.agent;


import bap.cells.Cells;
import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.util.EasyOperation;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

import static ai.agent.constant.GroupChatConstants.FormModelId_GroupChatRoom;
import static ai.agent.constant.GroupChatConstants.UserModelId;

@Comment("群聊房间服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-10", updateTime = "2025-09-10"
)
// cell.ai.agent.IGroupChatRoomService
public interface IGroupChatRoomService extends IGroupChatBasicService {
    static IGroupChatRoomService get() {
        return Cells.get(IGroupChatRoomService.class);
    }

    EasyOperation Op = EasyOperation.get();

    // 获取房间信息列表
    default List<Form> getRoomInfoList(IDao dao) throws Exception {

        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FormModelId_GroupChatRoom, null,
                1, Integer.MAX_VALUE, false, false);
        return queryRs.getDataList();
    }


    // 获取房间
    default Form getRoomInfo(IDao dao, String roomCode, Boolean isGetChatMsgs) throws Exception {
        return IFormMgr.get().queryFormByCode(dao, FormModelId_GroupChatRoom, roomCode);
    }

    // 获取房间
    default Form getRoomInfoByBusDomainCode(IDao dao, String busDomainCode) {
        if (StrUtil.isBlank(busDomainCode)) return null;
        try {
            return Op.queryFormByCondition(dao, FormModelId_GroupChatRoom,
                    "所属业务域", busDomainCode, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    // 获取房间
    default boolean changeRoomName(IDao dao, String busDomainCode, String roomName) {
        if (StrUtil.hasBlank(busDomainCode, roomName)) return false;
        try {
            Form form = getRoomInfoByBusDomainCode(dao, busDomainCode);
            if (form == null) return false;
            form.setAttrValue("房间名称", roomName);
            IFormMgr.get().updateForm(dao, form);

            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    // 添加房间
    default void createRoomInfo(IDao dao, String roomCode, String roomName,
                                String creatorCode, String createdBusDomainCode) {
        if (StrUtil.hasBlank(roomName, creatorCode, createdBusDomainCode))
            throw new RuntimeException("房间名称/创建者编号/被创建的业务域编号不得为空");

        try {
            Form form = new Form(FormModelId_GroupChatRoom);
            AssociationData creatorAc = new AssociationData(UserModelId, creatorCode);
            form.setAttrValue(Form.Code, roomCode)
                    .setAttrValue("房间名称", roomName)
                    .setAttrValue("创建者", creatorAc)
                    .setAttrValue("房间成员", CollUtil.newArrayList(creatorAc))
                    .setAttrValue("创建时间", System.currentTimeMillis())
                    .setAttrValue("所属业务域", createdBusDomainCode);

            IFormMgr.get().createForm(dao, form);

            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    default void deleteRoomInfoByBusDomainCode(IDao dao, String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) return;
        Cnd cnd = Cnd.NEW();
        cnd.where().andEquals(Op.getFieldCode("所属业务域"), busDomainCode);
        IFormMgr.get().deleteForm(dao, FormModelId_GroupChatRoom, cnd);


    }


}
