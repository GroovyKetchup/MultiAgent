package ai.agent.dto.groupChat;

import ai.agent.constant.GroupChatConstants;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.IdUtil;
import gpf.adur.data.Form;
import org.nutz.dao.entity.annotation.Comment;

@Comment("用户聊天记录Dto")
@ClassDeclare(
        label = "",
        what = "",
        why = "",
        how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-16", updateTime = "2025-09-16"
)
public class AgentChatRecordDto {

    public static final String FORM_MODEL_ID = GroupChatConstants.FormModelId_GroupChatUserChatRecord;
    public static final String FIELD_NAME_SOURCE_BUS_DOMAIN = "来源业务域";
    public static final String FIELD_NAME_MSG_CODE = "信息编号";
    public static final String FIELD_NAME_MSG_TYPE = "信息类型";
    public static final String FIELD_NAME_MSG_ATTACHMENT_FILE_CODE = "信息附件";
    public static final String FIELD_NAME_MSG_CONTENT = "信息内容";
    public static final String FIELD_NAME_CREATE_TIME = "创建时间";

    private String sourceBusDomain;
    private String msgCode;
    private String msgType;
    private String msgAttachmentFileCode;
    private String msgContent;
    private Long createTime;

    /**
     * 从Form对象转换成Dto
     *
     * @param recordForm Form数据对象
     * @return UserChatRecordDto 实例
     */
    public static AgentChatRecordDto newDto(Form recordForm) {
        if (recordForm == null) return null;

        try {
            String sourceBusinessArea = recordForm.getString(FIELD_NAME_SOURCE_BUS_DOMAIN);
            String msgCode = recordForm.getString(FIELD_NAME_MSG_CODE);
            String msgType = recordForm.getString(FIELD_NAME_MSG_TYPE);
            String msgAttachment = recordForm.getString(FIELD_NAME_MSG_ATTACHMENT_FILE_CODE);
            String msgContent = recordForm.getString(FIELD_NAME_MSG_CONTENT);
            Long createTime = recordForm.getLong(FIELD_NAME_CREATE_TIME);

            return new AgentChatRecordDto()
                    .setSourceBusDomain(sourceBusinessArea)
                    .setMsgCode(msgCode)
                    .setMsgType(msgType)
                    .setMsgAttachmentFileCode(msgAttachment)
                    .setMsgContent(msgContent)
                    .setCreateTime(createTime);

        } catch (Exception e) {
            // 可以在这里添加日志记录
            return null;
        }
    }

    /**
     * 将Dto转换为Form对象
     *
     * @return Form 数据对象
     */
    public Form toForm() {
        try {
            String uuid = IdUtil.fastSimpleUUID();
            Form form = new Form(FORM_MODEL_ID);
            form.setUuid(uuid).setAttrValue(Form.Code, uuid);
            form.setAttrValue(FIELD_NAME_MSG_CODE, this.getMsgCode());
            form.setAttrValue(FIELD_NAME_SOURCE_BUS_DOMAIN, this.getSourceBusDomain());
            form.setAttrValue(FIELD_NAME_MSG_TYPE, this.getMsgType());
            form.setAttrValue(FIELD_NAME_MSG_ATTACHMENT_FILE_CODE, this.getMsgAttachmentFileCode());
            form.setAttrValue(FIELD_NAME_MSG_CONTENT, this.getMsgContent());
            form.setAttrValue(FIELD_NAME_CREATE_TIME, this.getCreateTime());

            return form;
        } catch (Exception e) {
            // 可以在这里添加日志记录
            return null;
        }
    }


    // ========================= getter/setter =========================


    public String getMsgCode() {
        return msgCode;
    }

    public AgentChatRecordDto setMsgCode(String msgCode) {
        this.msgCode = msgCode;
        return this;
    }

    public String getSourceBusDomain() {
        return sourceBusDomain;
    }

    public AgentChatRecordDto setSourceBusDomain(String sourceBusDomain) {
        this.sourceBusDomain = sourceBusDomain;
        return this;
    }

    public String getMsgType() {
        return msgType;
    }

    public AgentChatRecordDto setMsgType(String msgType) {
        this.msgType = msgType;
        return this;
    }

    public String getMsgAttachmentFileCode() {
        return msgAttachmentFileCode;
    }

    public AgentChatRecordDto setMsgAttachmentFileCode(String msgAttachmentFileCode) {
        this.msgAttachmentFileCode = msgAttachmentFileCode;
        return this;
    }

    public String getMsgContent() {
        return msgContent;
    }

    public AgentChatRecordDto setMsgContent(String msgContent) {
        this.msgContent = msgContent;
        return this;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public AgentChatRecordDto setCreateTime(Long createTime) {
        this.createTime = createTime;
        return this;
    }
}