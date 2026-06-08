package cell.ai.agent.http;


import ai.agent.constant.HttpConstants;
import ai.agent.dto.RespondDto;
import bap.cells.Cells;
import cell.CellIntf;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.http.anotation.RequestMapping;
import cmn.http.anotation.RequestMethod;
import cmn.http.servlet.mapping.RequestMappingIntf;
import org.nutz.dao.entity.annotation.Comment;
import web.dto.Pair;

@Comment("群聊-文件服务")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-07-22", updateTime = "2025-07-22"
)
@RequestMapping(path = HttpConstants.RequestUrlPrefix_FileServer)
public interface IGroupChatFileController extends CellIntf, RequestMappingIntf {

    static IGroupChatFileController get() {
        return Cells.get(IGroupChatFileController.class);
    }

    @MethodDeclare(
            label = "ping",
            what = "",
            why = "",
            how = "",
            inputs = {
            }
    )
    @RequestMapping(path = "/ping", method = {RequestMethod.GET})
    String ping() throws Exception;

    @MethodDeclare(
            label = "上传文件",
            what = "",
            why = "",
            how = "",
            inputs = {
                    @InputDeclare(name = "file", label = "文件", desc = ""),
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_UploadFile, method = {RequestMethod.POST})
    String upload(Pair<String, byte[]> file) throws Exception;


    @MethodDeclare(
            label = "下载文件",
            what = "",
            why = "",
            how = "",
            inputs = {
                    @InputDeclare(name = "fileCode", label = "文件编号", desc = "")
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_DownloadFile, method = {RequestMethod.GET, RequestMethod.POST})
    Pair<String, byte[]> download(String fileCode) throws Exception;



    @MethodDeclare(
            label = "上传前端包VotaForge",
            what = "",
            why = "",
            how = "",
            inputs = {
                    @InputDeclare(name = "file", label = "文件", desc = ""),
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_UpdateFrontendPackage_VotaForge, method = {RequestMethod.POST})
    String updateFrontendPackageVotaForge(Pair<String, byte[]> file) throws Exception;


    @MethodDeclare(
            label = "上传前端包CDP",
            what = "",
            why = "",
            how = "",
            inputs = {
                    @InputDeclare(name = "file", label = "文件", desc = ""),
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_UpdateFrontendPackage_CDP, method = {RequestMethod.POST})
    String updateFrontendPackageCDP(Pair<String, byte[]> file) throws Exception;


    @MethodDeclare(
            label = "导入房间（业务域）",
            what = "",
            why = "",
            how = "",
            inputs = {
                    @InputDeclare(name = "file", label = "业务域dump包", desc = "zip格式的业务域导出包，包含domain.meta和payload.zip"),
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_Import_Room, method = {RequestMethod.POST})
    RespondDto<Object> importRoom(Pair<String, byte[]> file) throws Exception;


    @MethodDeclare(
            label = "导出房间（业务域）",
            what = "",
            why = "",
            how = "",
            inputs = {
                    @InputDeclare(name = "domainCode", label = "业务域编码", desc = "")
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_Export_Room, method = {RequestMethod.GET, RequestMethod.POST})
    Pair<String, byte[]> exportRoom(String domainCode) throws Exception;


    @MethodDeclare(
            label = "获取CDN文件列表",
            what = "获取PrivateCDN应用下指定路径的文件列表（仅一层）",
            why = "管理CDN静态资源",
            how = "读取PrivateCDN应用目录下的文件，做好路径安全校验",
            inputs = {
                    @InputDeclare(name = "path", label = "相对路径", desc = "相对于PrivateCDN应用根目录的路径，空或/表示根目录"),
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_CDN_ListFiles, method = {RequestMethod.GET, RequestMethod.POST})
    RespondDto<Object> cdnListFiles(String path) throws Exception;


    @MethodDeclare(
            label = "上传CDN dump包",
            what = "上传CDN的dump压缩包，备份旧包并部署新包",
            why = "更新CDN静态资源",
            how = "使用WebAppUtil.backupAndUpload进行备份和上传",
            inputs = {
                    @InputDeclare(name = "file", label = "CDN dump压缩包", desc = "zip格式的CDN资源包"),
            }
    )
    @RequestMapping(path = HttpConstants.RequestUrlPath_CDN_UploadDump, method = {RequestMethod.POST})
    RespondDto<Object> cdnUploadDump(Pair<String, byte[]> file) throws Exception;






}
