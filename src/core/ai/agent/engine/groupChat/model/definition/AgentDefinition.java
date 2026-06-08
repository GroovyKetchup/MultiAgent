package ai.agent.engine.groupChat.model.definition;

import ai.agent.constant.GroupChatConstants;
import ai.agent.dto.llmCalling.ToolDto;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;

import java.io.Serializable;
import java.util.List;

/**
 * Agent Definition - 智能体定义类
 * 只负责配置信息，不负责创建实例（避免循环依赖）
 */
public class AgentDefinition implements Serializable {

    public static final String FORM_MODEL_ID = GroupChatConstants.FormModelId_AgentDefinition;

    // 智能体ID
    private String agentId;
    // 智能体头像(URL or base64)
    private String agentAvatar;
    // 智能体姓名
    private String agentName;
    // 商业职务
    private String agentBusinessRole;
    // 角色介绍
    private String agentDescription;

    // 内置提示词
    private String agentPrompt;

    // 可以使用的工具集合
    private List<ToolDto> agentTools;
    // LLM 配置
    private LLMConfig llmConfig;
    // 是否总控智能体
    private boolean isSysScheduler;
    // 是否在线（或是是否启用）
    private boolean isOnline;

    public AgentDefinition(String agentId, String agentName, String agentAvatar, String agentBusinessRole, String agentDescription,
                           String agentPrompt, List<ToolDto> agentTools, boolean isSysScheduler, boolean isOnline) {
        this.agentId = agentId;
        this.agentAvatar = agentAvatar;
        this.agentName = agentName;
        this.agentBusinessRole = agentBusinessRole;
        this.agentDescription = agentDescription;
        this.agentPrompt = agentPrompt;
        this.agentTools = agentTools;
        this.isSysScheduler = isSysScheduler;
        this.isOnline = isOnline;
    }


    // 基于用户提供的Form进行更新这个定义
    public void updateByForm(Form agentDefinitionForm) {
        if (agentDefinitionForm == null) return;
        if (!FORM_MODEL_ID.equals(agentDefinitionForm.getFormModelId())) return;
        try {
            String agentId = agentDefinitionForm.getString("智能体ID");
            if (StrUtil.isBlank(agentId)) return;

            String agentName = agentDefinitionForm.getString("智能体名称");
            String agentAvatar = agentDefinitionForm.getString("智能体头像");
            String agentBusinessRole = agentDefinitionForm.getString("智能体商业职务");
            String agentDescription = agentDefinitionForm.getString("智能体介绍");

            if (StrUtil.isNotBlank(agentName)) this.agentName = agentName;
            if (StrUtil.isNotBlank(agentAvatar)) this.agentAvatar = agentAvatar;
            if (StrUtil.isNotBlank(agentBusinessRole)) this.agentBusinessRole = agentBusinessRole;
            if (StrUtil.isNotBlank(agentDescription)) this.agentDescription = agentDescription;

        } catch (Exception e) {
            return;
        }


    }


    // ========================= 支撑方法 =========================


    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getAgentDescription() {
        return agentDescription;
    }

    public List<ToolDto> getAgentTools() {
        return agentTools;
    }

    public LLMConfig getLlmConfig() {
        return llmConfig;
    }

    public boolean isSysScheduler() {
        return isSysScheduler;
    }

    public AgentDefinition setAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public String getAgentAvatar() {
        return agentAvatar;
    }

    public AgentDefinition setAgentAvatar(String agentAvatar) {
        this.agentAvatar = agentAvatar;
        return this;
    }

    public AgentDefinition setAgentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    public String getAgentBusinessRole() {
        return agentBusinessRole;
    }

    public AgentDefinition setAgentBusinessRole(String agentBusinessRole) {
        this.agentBusinessRole = agentBusinessRole;
        return this;
    }

    public AgentDefinition setAgentDescription(String agentDescription) {
        this.agentDescription = agentDescription;
        return this;
    }

    public AgentDefinition setAgentTools(List<ToolDto> agentTools) {
        this.agentTools = agentTools;
        return this;
    }

    public AgentDefinition setLlmConfig(LLMConfig llmConfig) {
        this.llmConfig = llmConfig;
        return this;
    }

    public AgentDefinition setSysScheduler(boolean sysScheduler) {
        isSysScheduler = sysScheduler;
        return this;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public AgentDefinition setOnline(boolean online) {
        isOnline = online;
        return this;
    }

    public String getAgentPrompt() {
        return agentPrompt;
    }

    public AgentDefinition setAgentPrompt(String agentPrompt) {
        this.agentPrompt = agentPrompt;
        return this;
    }
}
