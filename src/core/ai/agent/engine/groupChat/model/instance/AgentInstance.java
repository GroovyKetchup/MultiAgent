package ai.agent.engine.groupChat.model.instance;

import ai.agent.constant.AppConstants;
import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.message.payload.operate.AgentStatusOperate;
import ai.agent.dto.llmCalling.ToolDto;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.model.definition.AgentDefinition;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.engine.groupChat.tool.ToolRegistry;
import ai.agent.enums.AgentStatus;
import ai.agent.enums.OperateMessageEnums;
import ai.agent.service.llmCalling.LLMClient;
import ai.agent.util.ConsolePrintUtil;
import ai.agent.util.groupChat.MessageBuilder;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.annotation.JSONField;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 智能体实例
 */
public class AgentInstance implements Serializable {

    // 智能体实例id
    private String instanceId;
    // 智能体定义
    private AgentDefinition definition;
    // 智能体客户端
    private LLMClient llmClient;
    // 实际生效的大模型配置
    private LLMConfig effectiveLlmConfig;

    // 工具注册
    @JSONField(serialize = false, deserialize = false)
    private ToolRegistry toolRegistry;

    // 智能体状态
    private String status;
    // 上次活跃时间
    private Instant lastActiveTime;

    // 打断版本号（每次打断时递增）
    private final AtomicLong interruptVersion;
    // 当前执行版本号（开始执行时记录）
    private final AtomicLong executionVersion;
    // 待处理的打断标志（打断后设为true，新执行开始处理消息时清除）
    private volatile boolean pendingInterrupt = false;


    public AgentInstance(String instanceId, AgentDefinition definition,
                         LLMClient llmClient, LLMConfig effectiveLlmConfig) {
        this.instanceId = instanceId;
        this.definition = definition;
        this.llmClient = llmClient;
        this.effectiveLlmConfig = effectiveLlmConfig;
        this.status = AgentStatus.IDLE.toString();
        this.lastActiveTime = Instant.now();
        this.interruptVersion = new AtomicLong(0);
        this.executionVersion = new AtomicLong(0);
        this.toolRegistry = loadToolRegistry();

    }


    // ========================= 支撑方法 =========================


    // 创建智能体实例
    public static AgentInstance create(AgentDefinition definition, LLMClient llmClient, LLMConfig groupLlmConfig) {
        String instanceId = IdUtil.fastSimpleUUID();
        LLMConfig effectiveLlmConfig = definition.getLlmConfig() != null ? definition.getLlmConfig() : groupLlmConfig;
        return new AgentInstance(instanceId, definition, llmClient, effectiveLlmConfig);
    }

    // 克隆智能体实例
    public AgentInstance clone() {
        if(this.getLlmClient() == null) throw new RuntimeException("LLMClient不得为空");
        if(this.getDefinition() == null) throw new RuntimeException("AgentDefinition不得为空");
        if(this.getEffectiveLlmConfig() == null) throw new RuntimeException("EffectiveLlmConfig不得为空");

        String instanceId = IdUtil.fastSimpleUUID();
        return new AgentInstance(instanceId, this.getDefinition(),
                this.getLlmClient(),
                this.getEffectiveLlmConfig() );
    }


    // 获取可用工具列表
    public List<Tool> getAvailableTools(Function<Tool, Boolean> isSkip) {
        List<Tool> tools = new ArrayList<>();
        if (toolRegistry != null) {
            for (String toolName : toolRegistry.getToolNames()) {
                Tool tool = toolRegistry.get(toolName);
                if (tool != null) {
                    if (isSkip != null && !isSkip.apply(tool)) {
                        tools.add(tool);
                    }
                }
            }
        }

        // 根据名称进行排序，确保前缀匹配缓存的时候正常
        tools.sort(Comparator.comparing(Tool::getName));
        return tools;
    }


    public void updateAgentStatus(GroupChatEngine chatEngine, AgentStatus status) {
        if (chatEngine == null || status == null) return;
        // 创建智能体状态数据（思考中）
        AgentStatusOperate statusData = new AgentStatusOperate(
                this.getDefinition().getAgentId(),
                this.getDefinition().getAgentName(),
                status,
                System.currentTimeMillis()
        );

        Message operateMessage = MessageBuilder.createOperateMessage(
                AppConstants.AGENT_ID_SYSTEM_SCHEDULER,
                OperateMessageEnums.AGENT_STATUS_CHANGE.toString(),
                statusData,
                null
        );

        // 添加到信息记录
        chatEngine.getMessageHistoryManager().addMessage(chatEngine.getBusDomain(), operateMessage);

    }


    public void updateLastActiveTime() {
        this.lastActiveTime = Instant.now();
    }


    // 从定义注册工具注册器
    public ToolRegistry loadToolRegistry() {
        ToolRegistry registry = new ToolRegistry();
        if (definition == null) return null;
        if (CollUtil.isEmpty(definition.getAgentTools()))  return registry;
        for (ToolDto toolDto : definition.getAgentTools()) {
            String classPath = toolDto.getClassPath();
            if (StrUtil.isBlank(classPath)) continue;

            try {
                Tool tool = (Tool) Class.forName(classPath).newInstance();
                registry.register(tool);
            } catch (Exception e) {
                ConsolePrintUtil.printRedLn("[AgentInstance] 创建工具失败: " + toolDto.getName() + ", 原因: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return registry;
    }


    // ========== getter/setter ==========

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        updateLastActiveTime();
    }

    public Instant getLastActiveTime() {
        return lastActiveTime;
    }

    public AtomicLong getInterruptVersion() {
        return interruptVersion;
    }

    public AtomicLong getExecutionVersion() {
        return executionVersion;
    }

    // 打断智能体：递增打断版本号 + 设置待处理标志
    public void interrupt() {
        interruptVersion.incrementAndGet();
        pendingInterrupt = true;
    }

    // 开始执行：记录当前打断版本号 + 清除待处理标志
    public void startExecution() {
        executionVersion.set(interruptVersion.get());
        pendingInterrupt = false;
    }

    // 检查当前执行是否被打断（版本号检查 或 待处理标志）
    public boolean isCurrentExecutionInterrupted() {
        // 待处理标志为true时，即使版本号相等也认为被打断（新执行还没开始处理消息）
        return pendingInterrupt || executionVersion.get() < interruptVersion.get();
    }

    // 检查是否有待处理的打断
    public boolean hasPendingInterrupt() {
        return pendingInterrupt;
    }

    public AgentInstance setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public AgentInstance setDefinition(AgentDefinition definition) {
        this.definition = definition;
        return this;
    }

    public AgentInstance setLlmClient(LLMClient llmClient) {
        this.llmClient = llmClient;
        return this;
    }

    public AgentInstance setEffectiveLlmConfig(LLMConfig effectiveLlmConfig) {
        this.effectiveLlmConfig = effectiveLlmConfig;
        return this;
    }

    public AgentInstance setToolRegistry(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        return this;
    }

    public AgentInstance setLastActiveTime(Instant lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
        return this;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public AgentDefinition getDefinition() {
        return definition;
    }

    public LLMClient getLlmClient() {
        return llmClient;
    }

    public LLMConfig getEffectiveLlmConfig() {
        return effectiveLlmConfig;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }


}
