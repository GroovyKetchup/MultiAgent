package ai.agent.constant;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("Http请求常量")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-05", updateTime = "2025-09-05"
)
public class HttpConstants {

    // Http请求前缀
    public static final String RequestUrlPrefix_Admin = "/GroupChat-admin/api";
    public static final String RequestUrlPrefix_Evolve = "/VotaForge-Evolve/api";
    public static final String RequestUrlPath_GetUsageSituation = "/getUsageSituation";
    public static final String RequestUrlPath_QueryAgentExperienceByAgentId = "/queryAgentExperienceByAgentId";
    public static final String RequestUrlPath_SaveAgentExperience = "/saveAgentExperience";


    public static final String RequestUrlPrefix_FileServer = "/GroupChat-file/api";
    // Http请求地址上传文件
    public static final String RequestUrlPath_UploadFile = "/upload";
    // Http请求地址下载文件
    public static final String RequestUrlPath_DownloadFile = "/download";


    // 上传前端包VotaForge
    public static final String RequestUrlPath_UpdateFrontendPackage_VotaForge = "/updateFrontendPackage/VotaForge";
    // 上传前端包CDP
    public static final String RequestUrlPath_UpdateFrontendPackage_CDP = "/updateFrontendPackage/CDP";

    // 导入房间（业务域）
    public static final String RequestUrlPath_Import_Room = "/importRoom";

    // 导出房间（业务域）
    public static final String RequestUrlPath_Export_Room = "/exportRoom";

    // CDN文件列表
    public static final String RequestUrlPath_CDN_ListFiles = "/cdn/listFiles";
    // 上传CDN dump包
    public static final String RequestUrlPath_CDN_UploadDump = "/cdn/uploadDump";


}
