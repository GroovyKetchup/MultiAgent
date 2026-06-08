package cell.ai.agent;


import bap.cells.Cells;
import cell.ServiceCellIntf;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("聊天引擎管理Cell")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-18", updateTime = "2025-09-18"
)
// FIXME 后面再统一整改，现在这个是有问题的
public interface IGroupChatEngineServiceCell extends ServiceCellIntf {

    static IGroupChatEngineServiceCell get() {
        return Cells.get(IGroupChatEngineServiceCell.class);
    }

    void log();

}
