package ai.agent.service.groupChat.manager;

import ai.agent.dto.groupChat.GroupChatEngineConfig;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.GroupDefinition;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import ai.agent.engine.groupChat.session.SubSessionInstance;
import ai.agent.engine.groupChat.session.SubSessionMode;
import ai.agent.engine.groupChat.tool.impl.canvas.descriptor.CanvasTypeRegistry;
import ai.agent.service.groupChat.MessageHistoryManager;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.GroupChatEngineStoreUtil;
import cell.ai.agent.IGroupChatEngineServiceCell;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import octocm.domain.dto.DomainDto;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Comment("群聊引擎管理类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-07", updateTime = "2025-09-07"
)
public class GroupChatEngineManager {

    public static final int MAX_CHAT_ENGINE_NO = 10;
    // 群聊引擎缓存类
    private static Map<String, GroupChatEngine> ENGINE_CACHE = new ConcurrentHashMap<>();
    // 是否启用模拟子会话模式
    private static Boolean enableMockSubSessionMode = false;

    public static boolean isExistedCurrentGroupChatInst(String groupChatInstId) {
        if (StrUtil.isBlank(groupChatInstId)) return false;
        return ENGINE_CACHE.containsKey(groupChatInstId);
    }

    // 获取或创建GroupChatEngine
    public static GroupChatEngine getOrCreateChatEngine(String groupChatInstId) {
        GroupChatEngine chatEngine = getGroupChatEngine(groupChatInstId);
        if (chatEngine == null) {

            // 尝试从归档加载
            chatEngine = GroupChatEngineStoreUtil.tryLoadStoredData(groupChatInstId);

            if (chatEngine != null) {

                GroupDefinition groupDefinition = GroupDefinitionManager.getDefinition(chatEngine.getGroupChatInstance().getGroupCode());
                GroupChatInstance groupChatInst = GroupChatInstanceManager.getInstance()
                        .reCreateGroupChatInstance(groupDefinition, chatEngine.getGroupChatInstance().getInstanceId());
                chatEngine.setGroupChatInstance(groupChatInst);

                ENGINE_CACHE.put(chatEngine.getBusDomain().getDomainCode(),
                        chatEngine);
                ENGINE_CACHE.put(chatEngine.getGroupChatInstance().getInstanceId(),
                        chatEngine);

                // 更新定义
                GroupDefinitionManager.tryUpdateDefinition(groupDefinition);

                // FIXME 这里最好从房间取
                DomainDto busDomain = chatEngine.getBusDomain();
                GroupChatEngineConfig engineConfig = chatEngine.getEngineConfig();
                if (busDomain != null && engineConfig != null) {

                    // 1、刷新支持的模型列表
                    engineConfig.refreshSupportModelNames();

                    // 2、从配置中更新历史对话管理器的相关设置
                    MessageHistoryManager messageHistoryManager = chatEngine.getMessageHistoryManager();
                    if (messageHistoryManager != null && engineConfig.getContextTimeLimit() > 0) {
                        messageHistoryManager.setContextTimeLimit(engineConfig.getContextTimeLimit());
                    }

                }


                chatEngine.start(); // 启动引擎


                // 测试
                if (enableMockSubSessionMode) {
                    mockSubSession(chatEngine);
                }

                return chatEngine;
            }

        }


        if (chatEngine != null) return chatEngine;

        if (ENGINE_CACHE.size() > MAX_CHAT_ENGINE_NO) {
            try {
                GroupChatEngine groupChatEngine = ENGINE_CACHE.values().iterator().next();
                groupChatEngine.stop();
                ConsolePrintUtil.printRedLn(StrUtil.format("ChatEngine数量超过系统最大数量[{}], 移除房间[{}]", MAX_CHAT_ENGINE_NO, groupChatEngine
                        .getBusDomain().getDomainCode()));
            } catch (Exception ignored) {

            }
        }

        GroupChatInstanceManager groupChatInstManager = GroupChatInstanceManager.getInstance();
        GroupChatInstance groupChatInst = groupChatInstManager.getGroupChatInstance(groupChatInstId);

        if (groupChatInst == null)
            throw new RuntimeException(StrUtil.format("无法创建聊天引擎，未找到群聊实例[{}]", groupChatInstId));


        try {
            // FIXME 后面再统一整改
            IGroupChatEngineServiceCell engineServiceCell = IGroupChatEngineServiceCell.get();
            engineServiceCell.log();
        } catch (Exception ignored) {

        }
        return ENGINE_CACHE.computeIfAbsent(groupChatInstId, id -> {

            GroupChatEngine engine = createGroupChatEngine(groupChatInst);

            // 保存到cell统一管理
            engine.start(); // 启动引擎
            return engine;
        });
    }


    private static GroupChatEngine createGroupChatEngine(GroupChatInstance groupChatInst) {
        return new GroupChatEngine(groupChatInst);
    }

    public static GroupChatEngine getGroupChatEngine(String groupChatInstId) {
        if (ENGINE_CACHE.containsKey(groupChatInstId)) return ENGINE_CACHE.get(groupChatInstId);
        return null;
    }


    // ========================= 支撑方法 =========================

    // 模拟创建子会话
    private static void mockSubSession(GroupChatEngine chatEngine) {

        GroupChatInstance groupChatInstance = chatEngine.getGroupChatInstance();
        AgentDefinition agentDefinition = groupChatInstance.getDefinition().getAgentDefinitions().get(0);

//        mockSubSession(chatEngine, agentDefinition).markAsFailed("测试失败");
//        mockSubSession(chatEngine, agentDefinition).markAsWaitingReply();
//        mockSubSession(chatEngine, agentDefinition).markAsCompleted("测试成功");
        mockSubSession(chatEngine, agentDefinition).markAsRunning();


    }

    // 模拟创建子会话
    private static SubSessionInstance mockSubSession(GroupChatEngine chatEngine, AgentDefinition agentDefinition) {
        GroupChatInstance groupChatInstance = chatEngine.getGroupChatInstance();

        SubSessionInstance subSession = chatEngine.getSubSessionManager()
                // 创建子会话
                .createSubSession(groupChatInstance.getInstanceId(),
                        agentDefinition,
                        StrUtil.format("{}测试任务{}", agentDefinition.getAgentName(), RandomUtil.randomInt(1, 100)),
                        SubSessionMode.ASYNC, chatEngine
                );


        // 启动会话
        String sessionId = subSession.getInstanceId();
        chatEngine.getSubSessionManager()
                .startSubSession(sessionId);

        Message textMessage = Message.createTextMessage(agentDefinition.getAgentId(),
                StrUtil.format("我是{}", agentDefinition.getAgentName()), null);
        subSession.getMessageHistoryManager().addMessageOnlyHistory(textMessage);

        Message adjustCanvasMessage = CanvasTypeRegistry.mockAdjustPanelMessage(agentDefinition.getAgentId(), sessionId);
        subSession.getMessageHistoryManager().addMessageOnlyHistory(adjustCanvasMessage);
        subSession.getMessageHistoryManager().addMessage(chatEngine.getBusDomain(), adjustCanvasMessage);


        return subSession;
    }


    // 获取所有对话引擎
    public static List<GroupChatEngine> getAllEngines() {
        if (ENGINE_CACHE.isEmpty()) return new ArrayList<>();

        return new ArrayList<>(ENGINE_CACHE.values());
    }


    public static void removeChatEngine(String groupChatInstId) {
        ENGINE_CACHE.remove(groupChatInstId);
    }

    public static void removeAllChatEngine() {
        ENGINE_CACHE = new ConcurrentHashMap<>();
    }

    public static void stopAllChatEngine() {
        ConsolePrintUtil.printGreenLn(
                StrUtil.format("GroupChatEngineServiceCell: stop All GroupChatEngine.")
        );
        Collection<GroupChatEngine> values = ENGINE_CACHE.values();
        if (CollUtil.isNotEmpty(values)) {
            for (GroupChatEngine value : values) {
                value.stop();
            }
        }
    }

    public static void storeAllChatEngine() {
        ConsolePrintUtil.printGreenLn(
                StrUtil.format("GroupChatEngineServiceCell: store All storeAllChatEngine.")
        );
        Collection<GroupChatEngine> values = ENGINE_CACHE.values();
        if (CollUtil.isNotEmpty(values)) {
            for (GroupChatEngine value : values) {
                // 存储起来
                GroupChatEngineStoreUtil.saveTotalData(value);
            }
        }
    }

    public static int size() {
        return ENGINE_CACHE.size();
    }


}
