package ai.agent.engine.groupChat.model.definition;

import java.io.Serializable;
import java.util.List;

/**
 * Group Definition - 群组定义类
 * 只负责配置信息，不负责创建实例（避免循环依赖）
 */
public class GroupDefinition implements Serializable {
    private  String groupCode;  // Unique identifier for the group definition
    private  String displayName;
    private  String description;
    private  List<AgentDefinition> agentDefinitions;  // List of agent definitions in this group
    private  LLMConfig llmConfig;  // Required LLM config for the group (used as default for agents)
    private  String groupWorkflow;  // 群组工作流程描述

    public GroupDefinition(String groupCode, String displayName, String description,
                           List<AgentDefinition> agentDefinitions, LLMConfig llmConfig) {
        this(groupCode, displayName, description, agentDefinitions, llmConfig, null);
    }

    public GroupDefinition(String groupCode, String displayName, String description,
                           List<AgentDefinition> agentDefinitions, LLMConfig llmConfig, String groupWorkflow) {
        this.groupCode = groupCode;
        this.displayName = displayName;
        this.description = description;
        this.agentDefinitions = agentDefinitions;
        this.llmConfig = llmConfig;
        this.groupWorkflow = groupWorkflow;
    }

    // Getters
    public String getGroupCode() {
        return groupCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<AgentDefinition> getAgentDefinitions() {
        return agentDefinitions;
    }

    public LLMConfig getLlmConfig() {
        return llmConfig;
    }

    public String getGroupWorkflow() {
        return groupWorkflow;
    }

    /**
     * Get agent definition by agent ID
     */
    public AgentDefinition getAgentDefinition(String agentId) {
        return agentDefinitions.stream()
                .filter(def -> def.getAgentId().equals(agentId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get count of agents in this group
     */
    public int getAgentCount() {
        return agentDefinitions.size();
    }

    /**
     * Get system scheduler agent definition
     */
    public AgentDefinition getSystemSchedulerAgent() {
        return agentDefinitions.stream()
                .filter(def -> def.isSysScheduler())
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if group has workflow defined
     */
    public boolean hasWorkflow() {
        return groupWorkflow != null && !groupWorkflow.trim().isEmpty();
    }


    public GroupDefinition setGroupCode(String groupCode) {
        this.groupCode = groupCode;
        return this;
    }

    public GroupDefinition setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public GroupDefinition setDescription(String description) {
        this.description = description;
        return this;
    }

    public GroupDefinition setAgentDefinitions(List<AgentDefinition> agentDefinitions) {
        this.agentDefinitions = agentDefinitions;
        return this;
    }

    public GroupDefinition setLlmConfig(LLMConfig llmConfig) {
        this.llmConfig = llmConfig;
        return this;
    }

    public GroupDefinition setGroupWorkflow(String groupWorkflow) {
        this.groupWorkflow = groupWorkflow;
        return this;
    }
}
