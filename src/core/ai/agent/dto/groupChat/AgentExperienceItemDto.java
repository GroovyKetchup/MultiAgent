package ai.agent.dto.groupChat;

import ai.agent.constant.GroupChatConstants;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.IdUtil;
import gpf.adur.data.Form;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("智能体经验项目Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-15", updateTime = "2025-12-15"
)
public class AgentExperienceItemDto implements Serializable {

    public static final String FORM_MODEL_ID = GroupChatConstants.FormModelId_AgentExperienceContent;

    public static final String FIELD_NAME_EXPERIENCE_CONTENT = "经验内容";
    public static final String FIELD_NAME_ENABLED = "是否启用";
    private String experienceContent;
    private boolean isEnabled;

    public AgentExperienceItemDto() {
    }

    public AgentExperienceItemDto(String experienceContent, boolean isEnabled) {
        this.experienceContent = experienceContent;
        this.isEnabled = isEnabled;
    }

    public static AgentExperienceItemDto newDto(Form row) {
        try {
            if (row == null) return null;
            return new AgentExperienceItemDto()
                    .setExperienceContent(row.getString(AgentExperienceItemDto.FIELD_NAME_EXPERIENCE_CONTENT))
                    .setEnabled(row.getBoolean(AgentExperienceItemDto.FIELD_NAME_ENABLED));

        } catch (Exception e) {
            return null;
        }


    }

    public Form toForm() {
        try {
            String uuid = IdUtil.fastUUID();
            Form form = new Form(FORM_MODEL_ID);
            form.setUuid(uuid).setAttrValue(Form.Code, uuid);
            form.setAttrValue(FIELD_NAME_EXPERIENCE_CONTENT, this.getExperienceContent())
                    .setAttrValue(FIELD_NAME_ENABLED, this.isEnabled());
            return form;
        } catch (Exception e) {
            return null;
        }

    }


    // ========================= getter/setter =========================

    public String getExperienceContent() {
        return experienceContent;
    }

    public AgentExperienceItemDto setExperienceContent(String experienceContent) {
        this.experienceContent = experienceContent;
        return this;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public AgentExperienceItemDto setEnabled(boolean enabled) {
        isEnabled = enabled;
        return this;
    }


}
