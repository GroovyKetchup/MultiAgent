package cell.ai.agent;


import ai.agent.dto.groupChat.admin.ModelCallingLogDto;
import bap.cells.Cells;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import gpf.adur.data.Form;
import org.nutz.dao.entity.annotation.Comment;

@Comment("群聊模型调用服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-12", updateTime = "2025-09-12"
)
// cell.ai.agent.IGroupChatModelCallingService
public interface IGroupChatModelCallingService extends IGroupChatBasicService {
    static IGroupChatModelCallingService get() {
        return Cells.get(IGroupChatModelCallingService.class);
    }

    // 为消息添加模型执行日志
    default void addFullTracebackLog(ModelCallingLogDto modelCallingLogDto) {
        if (modelCallingLogDto == null) return;
        try (IDao dao = IDaoService.newIDao()) {
            Form form = modelCallingLogDto.toForm();
            IFormMgr.get().createForm(dao, form);
            dao.commit();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


}
