package ai.agent.dto.groupChat.admin;

import ai.agent.constant.GroupChatConstants;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("模型调用日志Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-12", updateTime = "2025-09-12"
)
public class ModelCallingLogDto implements Serializable {

    public static final String FORM_MODEL_ID = GroupChatConstants.FormModelId_ModelCallingLog;
    public static final String FIELD_NAME_SOURCE_BUSINESS_DOMAIN = "来源业务域";
    public static final String FIELD_NAME_CALLER = "调用者";
    public static final String FIELD_NAME_TRIGGER = "触发者";
    public static final String FIELD_NAME_TRIGGER_MESSAGE = "触发消息";
    public static final String FIELD_NAME_MESSAGE_WINDOW = "消息窗口";
    public static final String FIELD_NAME_RESPONSE_CONTENT = "响应内容";
    public static final String FIELD_NAME_CREATE_TIME = "创建时间";

    private String sourceBusinessDomain;
    private String caller;
    private String trigger;
    private String triggerMessage;
    private String messageWindow;
    private String responseContent;
    private Long createTime;


    // 从Form中转换
    public static ModelCallingLogDto newDto(Form modelCallingLogForm) {
        if (modelCallingLogForm == null) return null;

        try {
            String sourceBusinessDomain = modelCallingLogForm.getString(FIELD_NAME_SOURCE_BUSINESS_DOMAIN);
            String caller = modelCallingLogForm.getString(FIELD_NAME_CALLER);
            String trigger = modelCallingLogForm.getString(FIELD_NAME_TRIGGER);
            String triggerMessage = modelCallingLogForm.getString(FIELD_NAME_TRIGGER_MESSAGE);
            String messageWindow = modelCallingLogForm.getString(FIELD_NAME_MESSAGE_WINDOW);
            String responseContent = modelCallingLogForm.getString(FIELD_NAME_RESPONSE_CONTENT);
            Long createTime = modelCallingLogForm.getLong(FIELD_NAME_CREATE_TIME);

            if (StrUtil.isBlank(sourceBusinessDomain)) sourceBusinessDomain = "未知业务域";
            if (StrUtil.isBlank(caller)) caller = "未知调用者";
            if (StrUtil.isBlank(trigger)) trigger = "未知触发者";

            return new ModelCallingLogDto()
                    .setSourceBusinessDomain(sourceBusinessDomain)
                    .setCaller(caller)
                    .setTrigger(trigger)
                    .setTriggerMessage(triggerMessage)
                    .setMessageWindow(messageWindow)
                    .setResponseContent(responseContent)
                    .setCreateTime(createTime)
                    ;

        } catch (Exception e) {
            return null;
        }

    }


    // 转换为Form
    public Form toForm() {
        try {
            String uuid = IdUtil.fastUUID();
            Form form = new Form(FORM_MODEL_ID);
            form.setUuid(uuid).setAttrValue(Form.Code, uuid);
            form.setAttrValue(FIELD_NAME_SOURCE_BUSINESS_DOMAIN, this.getSourceBusinessDomain());
            form.setAttrValue(FIELD_NAME_CALLER, this.getCaller());
            form.setAttrValue(FIELD_NAME_TRIGGER, this.getTrigger());
            form.setAttrValue(FIELD_NAME_TRIGGER_MESSAGE, this.getTriggerMessage());
            form.setAttrValue(FIELD_NAME_MESSAGE_WINDOW, this.getMessageWindow());
            form.setAttrValue(FIELD_NAME_RESPONSE_CONTENT, this.getResponseContent());
            form.setAttrValue(FIELD_NAME_CREATE_TIME, this.getCreateTime());
            return form;
        } catch (Exception e) {
            return null;
        }


    }

    public String getSourceBusinessDomain() {
        return sourceBusinessDomain;
    }

    public ModelCallingLogDto setSourceBusinessDomain(String sourceBusinessDomain) {
        this.sourceBusinessDomain = sourceBusinessDomain;
        return this;
    }

    public String getCaller() {
        return caller;
    }

    public ModelCallingLogDto setCaller(String caller) {
        this.caller = caller;
        return this;
    }

    public String getTrigger() {
        return trigger;
    }

    public ModelCallingLogDto setTrigger(String trigger) {
        this.trigger = trigger;
        return this;
    }

    public String getTriggerMessage() {
        return triggerMessage;
    }

    public ModelCallingLogDto setTriggerMessage(String triggerMessage) {
        this.triggerMessage = triggerMessage;
        return this;
    }

    public String getMessageWindow() {
        return messageWindow;
    }

    public ModelCallingLogDto setMessageWindow(String messageWindow) {
        this.messageWindow = messageWindow;
        return this;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public ModelCallingLogDto setResponseContent(String responseContent) {
        this.responseContent = responseContent;
        return this;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public ModelCallingLogDto setCreateTime(Long createTime) {
        this.createTime = createTime;
        return this;
    }
}
