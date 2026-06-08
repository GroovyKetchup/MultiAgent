package cell.ai.agent.http;


import ai.agent.constant.HttpConstants;
import ai.agent.dto.groupChat.admin.PlatformUsageSituationDto;
import bap.cells.Cells;
import cell.CellIntf;
import cmn.anotation.ClassDeclare;
import cmn.anotation.MethodDeclare;
import cmn.http.anotation.RequestMapping;
import cmn.http.anotation.RequestMethod;
import cmn.http.servlet.mapping.RequestMappingIntf;
import org.nutz.dao.entity.annotation.Comment;

@Comment("群聊-管理后台接口")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-07-22", updateTime = "2025-07-22"
)
@RequestMapping(path = HttpConstants.RequestUrlPrefix_Admin)
public interface IGroupChatAdminController extends CellIntf, RequestMappingIntf {

    static IGroupChatAdminController get() {
        return Cells.get(IGroupChatAdminController.class);
    }

    @MethodDeclare(
            label = "上传文件",
            what = "",
            why = "",
            how = "",
            inputs = {
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_GetUsageSituation, method = {RequestMethod.GET})
    PlatformUsageSituationDto getUsageSituation() throws Exception;



}
