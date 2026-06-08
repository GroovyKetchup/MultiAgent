package ai.agent.dto.groupChat;

import ai.agent.constant.GroupChatConstants;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Comment("智能体经验Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-12", updateTime = "2025-12-12"
)
public class AgentExperienceDto implements Serializable {

    public static final String FORM_MODEL_ID = GroupChatConstants.FormModelId_AgentExperienceLibrary;
    public static final String FIELD_NAME_AGENT_ID = "智能体ID";
    public static final String FIELD_NAME_AGENT_NAME = "智能体名称";
    public static final String FIELD_NAME_AGENT_EXPERIENCE_ITEMS = "智能体经验内容";

    private String agentId;
    private String agentName;
    private List<AgentExperienceItemDto> experienceItems;
    private String globalExperienceContent;

    // 转换为dto
    public static AgentExperienceDto newDto(Form form) {
        if (form == null) return null;

        try {
            String agentId = form.getString(FIELD_NAME_AGENT_ID);
            String agentName = form.getString(FIELD_NAME_AGENT_NAME);
            TableData experienceItemTd = form.getTable(FIELD_NAME_AGENT_EXPERIENCE_ITEMS);
            List<AgentExperienceItemDto> experienceItems = new ArrayList<>();
            if(experienceItemTd != null && !experienceItemTd.isEmtpy()){
                for (Form row : experienceItemTd.getRows()) {

                    AgentExperienceItemDto itemDto =  AgentExperienceItemDto.newDto(row);
                    if(itemDto == null) continue;
                    experienceItems.add(itemDto);
                }
            }


            if (StrUtil.isBlank(agentName)) agentName = "未知智能体";

            return new AgentExperienceDto()
                    .setAgentId(agentId)
                    .setAgentName(agentName)
                    .setExperienceItems(experienceItems);

        } catch (Exception e) {
            return null;
        }
    }

    // 转换为form
    public Form toForm() {
        try {
            String uuid = IdUtil.fastUUID();
            Form form = new Form(FORM_MODEL_ID);
            form.setUuid(uuid).setAttrValue(Form.Code, uuid);
            form.setAttrValue(FIELD_NAME_AGENT_ID, this.getAgentId());
            form.setAttrValue(FIELD_NAME_AGENT_NAME, this.getAgentName());


            if(CollUtil.isNotEmpty(this.getExperienceItems())){
                TableData experienceItemTd = new TableData(FIELD_NAME_AGENT_EXPERIENCE_ITEMS);
                for (AgentExperienceItemDto itemDto : this.getExperienceItems()) {
                    Form row = itemDto.toForm();
                    if(row != null){
                        experienceItemTd.add(row);
                    }
                }

                form.setAttrValue(FIELD_NAME_AGENT_EXPERIENCE_ITEMS,experienceItemTd);
            }


            return form;
        } catch (Exception e) {
            return null;
        }
    }

    // 转换为文本
    public String convertExperienceItemsToText() {
        if (CollUtil.isEmpty(this.experienceItems)) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.experienceItems.size(); i++) {
            AgentExperienceItemDto item = this.experienceItems.get(i);
            if (item == null || !item.isEnabled()) continue;
            if (StrUtil.isBlank(item.getExperienceContent())) continue;
            sb.append(item.getExperienceContent());
            if (i < this.experienceItems.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    // ========================= getter/setter =========================

    public String getAgentId() {
        return agentId;
    }

    public AgentExperienceDto setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public String getAgentName() {
        return agentName;
    }

    public AgentExperienceDto setAgentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    public String getGlobalExperienceContent() {
        return globalExperienceContent;
    }

    public AgentExperienceDto setGlobalExperienceContent(String globalExperienceContent) {
        this.globalExperienceContent = globalExperienceContent;
        return this;
    }

    public List<AgentExperienceItemDto> getExperienceItems() {
        return experienceItems;
    }

    public AgentExperienceDto setExperienceItems(List<AgentExperienceItemDto> experienceItems) {
        this.experienceItems = experienceItems;
        return this;
    }
}
