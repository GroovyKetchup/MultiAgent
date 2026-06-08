package ai.agent.engine.groupChat.model.instance;

import ai.agent.constant.GroupChatConstants;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.service.llmCalling.LLMClient;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Group Chat Instance - Manages agent instances for group chat
 * Used by GroupChatEngine for agent management
 */
public class GroupChatInstance implements Serializable {

    // Basic properties
    private String instanceId;
    private GroupDefinition definition;
    private LLMClient llmClient;

    // Agent instances managed by this group
    private final Map<String, AgentInstance> agentInstances = new LinkedHashMap<>();

    // Instance state
    private String status;
    private Instant createTime;
    private Instant lastActiveTime;

    public GroupChatInstance() {
        this.definition = null;
        this.instanceId = null;
        this.llmClient = null;

    }

    public GroupChatInstance(String instanceId, GroupDefinition definition, LLMClient llmClient) {
        this.instanceId = instanceId;
        this.definition = definition;
        this.llmClient = llmClient;
        this.status = GroupChatConstants.GROUP_CHAT_STATUS_ACTIVE;
        this.createTime = Instant.now();
        this.lastActiveTime = Instant.now();

        // Create agent instances from definition
        initAgentInstances();
    }

    // ========================= 支撑方法 =========================


    // 初始化智能体实例
    private void initAgentInstances() {
        for (AgentDefinition agentDef : definition.getAgentDefinitions()) {
            AgentInstance agentInstance = AgentInstance.create(
                    agentDef, llmClient, definition.getLlmConfig()
            );
            agentInstances.put(agentDef.getAgentId(), agentInstance);
        }
    }


    // ========================= getter/setter =========================


    /**
     * Update last active time
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = Instant.now();
    }


    /**
     * Get agent instance by agent ID
     */
    public AgentInstance getAgent(String agentId) {
        updateLastActiveTime();
        return agentInstances.get(agentId);
    }

    /**
     * Get all agent instances
     */
    public Map<String, AgentInstance> getAgentInstances() {
        return new LinkedHashMap<>(agentInstances);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public GroupDefinition getDefinition() {
        return definition;
    }

    public LLMClient getLlmClient() {
        return llmClient;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Instant getLastActiveTime() {
        return lastActiveTime;
    }

    // ========== State management ==========
    public void setStatus(String status) {
        this.status = status;
        updateLastActiveTime();
    }

    /**
     * Check if group chat is active
     */
    public boolean isActive() {
        return GroupChatConstants.GROUP_CHAT_STATUS_ACTIVE.equals(status);
    }

    /**
     * Get agent count
     */
    public int getAgentCount() {
        return agentInstances.size();
    }

    /**
     * Get group display name
     */
    public String getDisplayName() {
        return definition.getDisplayName();
    }

    /**
     * Get group code
     */
    public String getGroupCode() {
        return definition.getGroupCode();
    }

    public GroupChatInstance setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public GroupChatInstance setDefinition(GroupDefinition definition) {
        this.definition = definition;
        return this;
    }

    public GroupChatInstance setLlmClient(LLMClient llmClient) {
        this.llmClient = llmClient;
        return this;
    }

    public GroupChatInstance setCreateTime(Instant createTime) {
        this.createTime = createTime;
        return this;
    }

    public GroupChatInstance setLastActiveTime(Instant lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
        return this;
    }
}
