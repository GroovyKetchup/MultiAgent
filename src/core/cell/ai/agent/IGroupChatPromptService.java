package cell.ai.agent;


import bap.cells.Cells;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("群聊提示词服务类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-10", updateTime = "2025-09-10"
)
// cell.ai.agent.IGroupChatMessageService
public interface IGroupChatPromptService extends IGroupChatBasicService {
    static IGroupChatPromptService get() {
        return Cells.get(IGroupChatPromptService.class);
    }






}
