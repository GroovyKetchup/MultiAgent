package ai.agent.util.groupChat;

import ai.agent.constant.GroupChatConstants;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AttachData;
import gpf.adur.data.Form;
import gpf.exception.VerifyException;
import octo.cm.util.EasyOperation;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

@Comment("群聊文件工具")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-05", updateTime = "2025-09-05"
)
public class GroupChatFileUtil {

    public static final EasyOperation Op = EasyOperation.get();


    public static String upload(String fileName, byte[] fileBytes) throws Exception {
        if (fileBytes == null || fileBytes.length == 0)
            throw new VerifyException("文件为空");

        if (StrUtil.isBlank(fileName)) {
            // 这个无伤大雅，开发者看到自会处理
            fileName = IdUtil.fastSimpleUUID();
        }

        AttachData attachData = new AttachData().setName(fileName).setContent(fileBytes);

        Form form = Op.newForm(GroupChatConstants.FormModelId_GroupChatFile);
        form.setAttrValue("名称", fileName);
        form.setAttrValue("文件", CollUtil.newArrayList(attachData));
        form.setAttrValue("创建时间", System.currentTimeMillis());

        try (IDao dao = IDaoService.newIDao()) {
            form = IFormMgr.get().createForm(dao, form);
            dao.commit();
        }


        return form.getString(Form.Code);
    }


    // 查询附件列表
    public static List<AttachData> getAttachments(IDao dao, String fileCode) throws Exception {
        Form form = IFormMgr.get().queryFormByCode(dao, GroupChatConstants.FormModelId_GroupChatFile, fileCode);
        if (form == null) return null;

        List<AttachData> attachments = form.getAttachments("文件");
        if (CollUtil.isEmpty(attachments)) return null;
        return attachments;
    }

    // 下载文件
    public static ReportFile download(String fileCode) throws Exception {
        if (StrUtil.isBlank(fileCode)) return null;
        try (IDao dao = IDaoService.newIDao()) {
            Form form = IFormMgr.get().queryFormByCode(dao, GroupChatConstants.FormModelId_GroupChatFile, fileCode);
            if (form == null) return null;

            List<AttachData> attachments = form.getAttachments("文件");
            if (CollUtil.isEmpty(attachments)) return null;

            byte[] bytes = attachments.get(0).getContent();
            String fileName = form.getString("名称");

            if (StrUtil.isBlank(fileName)) fileName = IdUtil.fastSimpleUUID();

            Long createTime = Op.getLongOrDefault(form, "创建时间", System.currentTimeMillis());


            return new ReportFile(fileCode, fileName, bytes, createTime);
        }
    }


    public static class ReportFile {
        String code;
        String fileName;
        byte[] fileBytes;
        long createTime;

        public ReportFile(String code, String fileName, byte[] fileBytes, long createTime) {
            this.code = code;
            this.fileName = fileName;
            this.fileBytes = fileBytes;
            this.createTime = createTime;
        }

        public String getCode() {
            return code;
        }

        public ReportFile setCode(String code) {
            this.code = code;
            return this;
        }

        public String getFileName() {
            return fileName;
        }

        public ReportFile setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public byte[] getFileBytes() {
            return fileBytes;
        }

        public ReportFile setFileBytes(byte[] fileBytes) {
            this.fileBytes = fileBytes;
            return this;
        }

        public long getCreateTime() {
            return createTime;
        }

        public ReportFile setCreateTime(long createTime) {
            this.createTime = createTime;
            return this;
        }
    }


}
