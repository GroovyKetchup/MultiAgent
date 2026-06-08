package ai.agent.service.groupChat.manager;

import ai.agent.constant.AppConstants;
import ai.agent.constant.GroupChatConstants;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.service.llmCalling.HttpLlmClient;
import ai.agent.service.llmCalling.LLMClient;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

/**
 * Group Chat Instance Manager - Simplified design
 * Creates instances based on group definitions
 */
public class GroupChatInstanceManager {

    // LRU cache for group chat instances, max 3 instances
    private LRUCache<String, GroupChatInstance> instanceCache;

    // LLM client for creating instances
    private final LLMClient llmClient;

    // Singleton instance
    private static volatile GroupChatInstanceManager instance;

    private GroupChatInstanceManager(LLMClient llmClient) {
        this.instanceCache = new LRUCache<>(GroupChatConstants.MAX_GROUP_CHAT_INSTANCES);
        this.llmClient = llmClient;
    }

    /**
     * Get singleton instance with default HttpLlmClient
     */
    public static GroupChatInstanceManager getInstance() {
        return getInstance(new HttpLlmClient());
    }

    // 获取实例管理器
    public static GroupChatInstanceManager getInstance(LLMClient llmClient) {
        if (instance == null) {
            synchronized (GroupChatInstanceManager.class) {
                if (instance == null) {
                    instance = new GroupChatInstanceManager(llmClient);
                }
            }
        }
        return instance;
    }


    // 创建实例
    public GroupChatInstance createGroupChatInstance(GroupDefinition groupDefinition) {
        GroupChatInstance instance = createGroupChatInstance(groupDefinition, llmClient);
        instanceCache.put(instance.getInstanceId(), instance);
        return instance;
    }

    // 重新创建实例
    public GroupChatInstance reCreateGroupChatInstance(GroupDefinition groupDefinition, String instId) {

        GroupChatInstance instance = createGroupChatInstance(groupDefinition, llmClient);
        instance.setInstanceId(instId);
        instanceCache.put(instId, instance);
        return instance;
    }

    // 获取实例
    public GroupChatInstance getGroupChatInstance(String instanceId) {
        if (StrUtil.isBlank(instanceId)) return null;
        GroupChatInstance instance = instanceCache.get(instanceId);
        if (instance != null) {
            instance.updateLastActiveTime();
        }
        return instance;
    }

    // 移除实例
    public void removeInstance(String instanceId) {
        if (instanceCache.isEmpty()) return;
        instanceCache.remove(instanceId);

    }

    // 移除全部
    public void removeAllInstance() {
        if (instanceCache.isEmpty()) return;
        instanceCache = new LRUCache<>(GroupChatConstants.MAX_GROUP_CHAT_INSTANCES);

    }


    /**
     * 创建群组聊天实例
     */
    public static GroupChatInstance createGroupChatInstance(GroupDefinition definition, LLMClient llmClient) {
        String timeStamp = DateTime.now().getTime() + "" + RandomUtil.randomInt(1, 10);
        String instanceId = StrUtil.format(
                "{}{}",
                AppConstants.GROUP_INSTANCE_PREFIX,
                timeStamp
        );
        return new GroupChatInstance(instanceId, definition, llmClient);
    }

}
