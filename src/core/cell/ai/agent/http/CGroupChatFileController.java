package cell.ai.agent.http;

import ai.agent.constant.AppConstants;
import ai.agent.constant.GroupChatConstants;
import ai.agent.dto.RespondDto;
import ai.agent.dto.groupChat.GroupChatEngineConfig;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.service.groupChat.manager.GroupChatInstanceManager;
import ai.agent.service.groupChat.manager.GroupDefinitionManager;
import ai.agent.util.RoomDumpUtil;
import ai.agent.util.WebAppUtil;
import ai.agent.util.groupChat.GroupChatEngineStoreUtil;
import ai.agent.util.groupChat.GroupChatFileUtil;
import bap.cells.BasicCell;
import cell.ai.agent.IGroupChatRoomService;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octocm.domain.service.IDomainService;
import cell.octocm.workbench.app.IApplicationDeploy;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cmn.http.servlet.mapping.RequestMappingContext;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.Form;
import gpf.exception.VerifyException;
import octo.cm.util.ApplicationUtil;
import octo.cm.util.EasyOperation;
import octo.cm.util.FormToJsonConversionUtil;
import octo.cm.util.JsonToFormConversionUtil;
import octocm.domain.dto.DomainDto;
import octocm.domain.fe.WorkbenchDomainMgrTablePanel;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.dump.OctoCM2WorkbenchExtDumpConfig;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;
import web.dto.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Comment("")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-07-22", updateTime = "2025-07-22"
)
public class CGroupChatFileController extends BasicCell implements IGroupChatFileController {

    // 简单操作
    public static final EasyOperation Op = EasyOperation.get();
    public static final String APP_NAME_SEM_FOUNDRY = "VotaForge";
    public static final String APP_NAME_CDP = "CDP";

    RequestMappingContext context;

    @Override
    public RequestMappingContext getContext() {
        return context;
    }

    @Override
    public void setContext(RequestMappingContext context) {
        this.context = context;
    }


    @Override
    public String ping() throws Exception {
        return "pong";
    }

    // 上传文件
    @Override
    public String upload(Pair<String, byte[]> file) throws Exception {

        String fileName = file.getKey();
        byte[] fileBytes = file.getValue();

        return GroupChatFileUtil.upload(fileName, fileBytes);
    }


    // 下载文件
    @Override
    public Pair<String, byte[]> download(String fileCode) throws Exception {
        if (StrUtil.isBlank(fileCode)) throw new VerifyException("未找到这个文件");
        GroupChatFileUtil.ReportFile reportFile = GroupChatFileUtil.download(fileCode);
        if (reportFile == null) throw new VerifyException("未找到这个文件");

        return new Pair<>(reportFile.getFileName(), reportFile.getFileBytes());

    }

    @Override
    public RespondDto<Object> importRoom(Pair<String, byte[]> file) throws Exception {
        byte[] dumpZipBytes = file.getValue();
        if (dumpZipBytes == null || dumpZipBytes.length == 0) {
            throw new VerifyException("上传的文件为空");
        }

        // 解析dump包，提取domainCode和payload字节
        RoomDumpUtil.DumpPackage dumpPackage = RoomDumpUtil.parseDumpPackage(dumpZipBytes);
        String domainCode = dumpPackage.getDomainCode();
        byte[] payload = dumpPackage.getPayloadBytes();

        // 导入业务域数据
        try {

            // 可能会报错，但不影响房间的初始化
            try {
                Progress<?> prog2 = Progress.newOutput();
                OctoCM2WorkbenchExtDumpConfig dumpConfig = new OctoCM2WorkbenchExtDumpConfig(domainCode);
                dumpConfig.importDumpData(prog2, payload);
            } catch (Exception e) {
                System.out.println(ExceptionUtils.getFullStackTrace(e));
            }

            // 如果前缀代表是房间那么就初始化一下
            // 初始化是指去【创建房间】和【创建引擎数据】
            if (domainCode.startsWith(AppConstants.GROUP_INSTANCE_PREFIX)) {
                // 初始化房间和引擎占位数据
                initRoomAfterImport(domainCode, dumpPackage);
            }

        } catch (Exception e) {
            return RespondDto.newError(e.getMessage());
        }
        return RespondDto.newSuccess(StrUtil.format("导入成功", domainCode), null);
    }

    @Override
    public Pair<String, byte[]> exportRoom(String domainCode) throws Exception {
        if (StrUtil.isBlank(domainCode)) {
            throw new VerifyException("业务域编码不能为空");
        }

        Progress<?> prog2 = Progress.newOutput();
        OctoCM2WorkbenchExtDumpConfig dumpConfig = new OctoCM2WorkbenchExtDumpConfig(domainCode);
        byte[] payload = dumpConfig.exportDumpData(prog2, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            // domain.meta
            zos.putNextEntry(new ZipEntry(WorkbenchDomainMgrTablePanel.FILE_NAME_DOMAIN_META));
            zos.write(domainCode.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // payload.zip
            zos.putNextEntry(new ZipEntry(WorkbenchDomainMgrTablePanel.FILE_NAME_PAYLOAD));
            zos.write(payload);
            zos.closeEntry();

            // room.data（可选，房间Form JSON）
            try (IDao dao = IDaoService.newIDao()) {
                Form roomForm = IGroupChatRoomService.get().getRoomInfoByBusDomainCode(dao, domainCode);
                if (roomForm != null) {
                    cn.hutool.json.JSONObject roomJson = FormToJsonConversionUtil.convert(roomForm);
                    if (roomJson != null) {
                        byte[] roomDataBytes = roomJson.toString().getBytes(StandardCharsets.UTF_8);
                        zos.putNextEntry(new ZipEntry(RoomDumpUtil.FILE_ROOM_DATA));
                        zos.write(roomDataBytes);
                        zos.closeEntry();
                    }
                }

                // engine.data（可选，引擎数据JSON字符串）
                Form engineForm = Op.queryFormByCondition(dao, GroupChatConstants.FormModelId_GroupChatEngineData,
                        "房间编号", domainCode, null);
                if (engineForm != null) {
                    String engineDataJson = engineForm.getString("引擎数据");
                    if (StrUtil.isNotBlank(engineDataJson)) {
                        zos.putNextEntry(new ZipEntry(RoomDumpUtil.FILE_ENGINE_DATA));
                        zos.write(engineDataJson.getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();
                    }
                }
            }
        }
        String fileName = StrUtil.format("{}_业务域导出_{}.zip", domainCode, System.currentTimeMillis());
        return new Pair<>(fileName, baos.toByteArray());
    }

    @Override
    public String updateFrontendPackageVotaForge(Pair<String, byte[]> file) throws Exception {
        return updateFrontendPackage(APP_NAME_SEM_FOUNDRY, file.getValue());
    }

    @Override
    public String updateFrontendPackageCDP(Pair<String, byte[]> file) throws Exception {
        return updateFrontendPackage(APP_NAME_CDP, file.getValue());
    }

    // ========================= 支撑方法 =========================


    // 导入后初始化房间记录和引擎占位数据
    private static void initRoomAfterImport(String domainCode, RoomDumpUtil.DumpPackage dumpPackage) throws Exception {

        // 获取已导入的业务域信息
        IDomainService domainService = IDomainService.get();
        DomainDto busDomain = domainService.getDomainByCode(domainCode);
        if (busDomain == null || StrUtil.hasBlank(busDomain.getDomainCode(), busDomain.getDomainName())) {
            throw new VerifyException(StrUtil.format("导入后未找到业务域[{}]，初始化失败", domainCode));
        }

        try (IDao dao = IDaoService.newIDao()) {

            // ---- 恢复房间记录 ----
            if (IGroupChatRoomService.get().getRoomInfoByBusDomainCode(dao, domainCode) == null) {
                if (dumpPackage.hasRoomData()) {
                    // 有 room.data：直接用导出的 JSON 创建房间 Form
                    JSONObject roomJson = JSONUtil.parseObj(dumpPackage.getRoomDataJson());
                    Form roomForm = new Form(GroupChatConstants.FormModelId_GroupChatRoom);
                    roomForm = JsonToFormConversionUtil.convert(roomForm, roomJson);
                    IFormMgr.get().createForm(dao, roomForm);
                } else {
                    // fallback：重新创建房间记录
                    IGroupChatRoomService.get().createRoomInfo(
                            dao,
                            domainCode,
                            busDomain.getDomainName(),
                            AppConstants.DEFAULT_USER_CODE,
                            domainCode
                    );
                }
            }

            // ---- 恢复引擎数据 ----
            if (dumpPackage.hasEngineData()) {
                // 有 engine.data：直接写入引擎数据 Form
                Form engineForm = Op.queryFormByCondition(dao, GroupChatConstants.FormModelId_GroupChatEngineData,
                        "房间编号", domainCode, null);
                boolean isCreate = engineForm == null;
                if (isCreate) {
                    engineForm = Op.newForm(GroupChatConstants.FormModelId_GroupChatEngineData)
                            .setAttrValue("房间编号", domainCode);
                }
                engineForm.setAttrValue("引擎数据", dumpPackage.getEngineDataJson());
                if (isCreate) {
                    IFormMgr.get().createForm(dao, engineForm);
                } else {
                    IFormMgr.get().updateForm(dao, engineForm);
                }
            } else {
                // fallback：构建占位引擎并保存
                GroupDefinition groupDefinition = GroupDefinitionManager.getDefinition("default");
                if (groupDefinition != null) {
                    GroupChatInstance groupChatInst = GroupChatInstanceManager.getInstance()
                            .reCreateGroupChatInstance(groupDefinition, domainCode);
                    GroupChatEngine chatEngine = new GroupChatEngine(groupChatInst);
                    chatEngine.setBusDomain(busDomain);
                    chatEngine.setEngineConfig(new GroupChatEngineConfig());
                    GroupChatEngineStoreUtil.saveTotalData(chatEngine);
                }
            }

            // 发布应用
            try {
                String applicationCode = ApplicationUtil
                        .getDefaultPublishApplicationCode(new OctoDomainOpObserver(busDomain));
                if (StrUtil.isNotBlank(applicationCode)) {
                    Form appForm = ApplicationUtil.queryApplicationFormByAppCode(dao, applicationCode);
                    if (appForm != null) {
                        IApplicationDeploy.get().deploy(Progress.newOutput(), dao, appForm, new OctoDomainOpObserver(busDomain));
                    }
                }
            } catch (Exception e) {
                System.out.println(ExceptionUtils.getFullStackTrace(e));
            }

            dao.commit();
        }
    }


    /**
     * 更新前端包（备份+上传策略）
     * 1. 将现有前端包改名为 backup_时间戳_webAppName
     * 2. 上传新的前端包
     *
     * @param webAppName 前端包名称
     * @param zipBytes   zip文件字节数组
     * @return 操作结果信息
     */
    private static String updateFrontendPackage(String webAppName, byte[] zipBytes) throws Exception {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new VerifyException("上传的文件为空");
        }
        return WebAppUtil.backupAndUpload(zipBytes, webAppName);
    }


    // ========================= CDN管理 =========================

    public static final String APP_NAME_PRIVATE_CDN = "PrivateCDN";

    @Override
    public RespondDto<Object> cdnListFiles(String path) throws Exception {
        // 检查PrivateCDN应用是否存在
        if (!WebAppUtil.exists(APP_NAME_PRIVATE_CDN)) {
            return RespondDto.newError("CDN不存在，请先上传CDN包");
        }

        // 规范化路径：空或null视为根目录
        if (StrUtil.isBlank(path)) {
            path = "";
        }

        // 安全校验：禁止路径穿越字符
        if (path.contains("..") || path.contains("\\")) {
            return RespondDto.newError("路径包含非法字符");
        }

        // 构建目标目录并做canonical path校验
        File cdnRoot = new File(WebAppUtil.WEBAPPS_ROOT, APP_NAME_PRIVATE_CDN).getCanonicalFile();
        File targetDir = new File(cdnRoot, path).getCanonicalFile();

        // 安全校验：canonical path必须在CDN根目录下
        if (!targetDir.getCanonicalPath().startsWith(cdnRoot.getCanonicalPath())) {
            return RespondDto.newError("禁止访问CDN目录之外的路径");
        }

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return RespondDto.newError(StrUtil.format("路径[{}]不存在或不是目录", path));
        }

        // 列出一层文件/目录
        File[] children = targetDir.listFiles();
        List<JSONObject> fileList = new ArrayList<>();
        if (children != null) {
            for (File child : children) {
                JSONObject item = new JSONObject();
                item.set("name", child.getName());
                item.set("isDirectory", child.isDirectory());
                item.set("size", child.isDirectory() ? 0 : child.length());
                item.set("lastModified", child.lastModified());
                fileList.add(item);
            }
        }

        return RespondDto.newSuccess("获取成功", fileList);
    }

    @Override
    public RespondDto<Object> cdnUploadDump(Pair<String, byte[]> file) throws Exception {
        byte[] zipBytes = file.getValue();
        if (zipBytes == null || zipBytes.length == 0) {
            return RespondDto.newError("上传的文件为空");
        }

        try {
            String result = WebAppUtil.backupAndUpload(zipBytes, APP_NAME_PRIVATE_CDN);
            return RespondDto.newSuccess(result, null);
        } catch (Exception e) {
            return RespondDto.newError(StrUtil.format("CDN包上传失败: {}", e.getMessage()));
        }
    }

}
