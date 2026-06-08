package ai.agent.dto.groupChat;

import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.service.workflow.DefaultWorkflowBootstrap;
import ai.agent.util.ConsolePrintUtil;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Comment("群聊引擎配置")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-09", updateTime = "2025-09-09"
)
public class GroupChatEngineConfig implements Serializable {

    // ========================= 配置Key =========================
    // 是否开启自动调度
    private static final String CONFIG_KEY_START_TASK_AUTO_SCHEDULE = "startTaskAutoSchedule";
    // 是否开启消息Debug模式
    private static final String CONFIG_KEY_START_MESSAGE_DEBUG_MODE = "startMessageDebugMode";
    // 开启全局子任务模式
    private static final String CONFIG_KEY_START_GLOBAL_SUBTASK_MODE = "startGlobalSubtaskMode";
    // 开启全新上下文模式
    private static final String CONFIG_KEY_START_ALWAYS_FRESH_CONTEXT_MODE = "startAlwaysFreshContextMode";
    // 长时间任务是否短信通知
    private static final String CONFIG_KEY_NOTIFY_SMS_WHEN_LONG_TASK = "notifySmsWhenLongTask";
    // 是否用户指定模型
    private static final String CONFIG_KEY_USER_ASSIGN_LLM_MODEL = "userAssignLlmModel";
    // ReAct最大循环次数
    private static final String CONFIG_KEY_MAX_REACT_LOOPS = "maxReActLoops";
    // 自动任务配置
    private static final String CONFIG_KEY_AUTO_SCHEDULE_TASK_CONFIG = "autoScheduleTaskConfig";
    // 上下文时间限制
    private static final String CONFIG_KEY_CONTEXT_TIME_LIMIT = "contextTimeLimit";

    // ===========================================================

    // 开启任务自动调度机制
    // 默认开启，系统发现任务看板存在未执行的任务的时候会自动推送
    private Boolean startTaskAutoSchedule;

    // 开启消息Debug模式
    // 默认关闭，启动后所有消息都会记录执行日志，包含完整的Prompt
    private Boolean startMessageDebugMode;

    // 长时间任务通知设置
    // 默认关闭，开启后当遇到长时任务会进行短信通知
    private Boolean notifySmsWhenLongTask;

    // 开启全局子任务模式
    // 默认关闭，开启后所有的需求都使用子任务进行接管
    private Boolean startGlobalSubtaskMode;

    // 开启全新上下文模式
    // 默认关闭，开启后对话总是清空上下文，不再追加任何已发生的上下文
    private Boolean startAlwaysFreshContextMode;

    // 系统支持的模型列表
    private Set<String> systemSupportLlmModelNames;

    // 用户指定要调用的模型名称
    // 默认使用：AppConstants.DEFAULT_SYSTEM_USE_LLM_MODEL
    private String userAssignLlmModel;

    // 自动任务配置
    private Map<String, Boolean> autoScheduleTaskConfig;

    // 上下文时间限制（如果有，那么这个之前的消息不允许进入上下文）
    private long contextTimeLimit;


    // ========================= GraphEngine模式配置 =========================

    // GraphEngine最大循环次数
    // 默认50次，防止无限循环
    private Integer maxReActLoops = 50;

    public GroupChatEngineConfig() {
        // 由管理器自动加入
        this.systemSupportLlmModelNames = LLMConfigManager.getSystemSupportModelNames();
        // 自动调度接入
        this.autoScheduleTaskConfig = DefaultWorkflowBootstrap.loadDefaultWorkflowConfig();

        this.startTaskAutoSchedule = true;
        this.startMessageDebugMode = false;
        this.notifySmsWhenLongTask = false;
        this.startGlobalSubtaskMode = false;
        this.startAlwaysFreshContextMode = true;
        
    }

    // ========================= 更新配置 =========================

    // 更新配置
    public void updateConfig(Map<String, Object> newConfigMap) {

        ConsolePrintUtil.printRedLn(StrUtil.format("群聊引擎配置更新前:{}", JSONUtil.toJsonStr(this)));

        for (Map.Entry<String, Object> configEntry : newConfigMap.entrySet()) {

            String configKey = configEntry.getKey();
            Object configValue = configEntry.getValue();

            if (configValue instanceof Boolean) {
                Boolean bolVal = (Boolean) configValue;

                if (CONFIG_KEY_START_TASK_AUTO_SCHEDULE.equals(configKey)) {
                    setStartTaskAutoSchedule(bolVal);
                } else if (CONFIG_KEY_START_MESSAGE_DEBUG_MODE.equals(configKey)) {
                    setStartMessageDebugMode(bolVal);
                } else if (CONFIG_KEY_NOTIFY_SMS_WHEN_LONG_TASK.equals(configKey)) {
                    setNotifySmsWhenLongTask(bolVal);
                } else if (CONFIG_KEY_START_GLOBAL_SUBTASK_MODE.equals(configKey)) {
                    setStartGlobalSubtaskMode(bolVal);
                } else if (CONFIG_KEY_START_ALWAYS_FRESH_CONTEXT_MODE.equals(configKey)) {
                    setStartAlwaysFreshContextMode(bolVal);
                }


            } else if (configValue instanceof String) {
                String strVal = (String) configValue;
                if (CONFIG_KEY_USER_ASSIGN_LLM_MODEL.equals(configKey)) {
                    if (StrUtil.isNotBlank(strVal)) {
                        // 不为空 && 系统支持这个模型
                        if (LLMConfigManager.isSupportModel(strVal)) {
                            setUserAssignLlmModel(strVal);
                        }

                        // 如果是AUTO的话其实就是群聊/智能体用自己配置的模型
                        if (strVal.toUpperCase(Locale.ROOT).equals("AUTO")) {
                            setUserAssignLlmModel("");
                        }

                    }
                }

            } else if (configValue instanceof Integer) {
                Integer intVal = (Integer) configValue;
                if (CONFIG_KEY_MAX_REACT_LOOPS.equals(configKey)) {
                    setMaxReActLoops(intVal);
                }
            } else if (configValue instanceof Map) {
                Map<String, Boolean> mapVal = (Map<String, Boolean>) configValue;
                if (CONFIG_KEY_AUTO_SCHEDULE_TASK_CONFIG.equals(configKey)) {
                    setAutoScheduleTaskConfig(mapVal);
                }
            } else if (configValue instanceof Long) {
                long longVal = (long) configValue;
                if (CONFIG_KEY_CONTEXT_TIME_LIMIT.equals(configKey)) {
                    setContextTimeLimit(longVal);
                }
            }


        }

        ConsolePrintUtil.printRedLn(StrUtil.format("群聊引擎配置更新后:{}", JSONUtil.toJsonStr(this)));


    }

    // 刷新支持的模型名称
    public void refreshSupportModelNames() {
        this.systemSupportLlmModelNames = LLMConfigManager.getSystemSupportModelNames();
    }

    // ========================= getter and setter =========================


    public Boolean isNotifySmsWhenLongTask() {
        return notifySmsWhenLongTask;
    }

    public GroupChatEngineConfig setNotifySmsWhenLongTask(Boolean notifySmsWhenLongTask) {
        this.notifySmsWhenLongTask = notifySmsWhenLongTask;
        return this;
    }

    public Set<String> getSystemSupportLlmModelNames() {
        return systemSupportLlmModelNames;
    }

    public GroupChatEngineConfig setSystemSupportLlmModelNames(Set<String> systemSupportLlmModelNames) {
        this.systemSupportLlmModelNames = systemSupportLlmModelNames;
        return this;
    }

    public Boolean isStartTaskAutoSchedule() {
        return startTaskAutoSchedule;
    }

    public GroupChatEngineConfig setStartTaskAutoSchedule(Boolean startTaskAutoSchedule) {
        this.startTaskAutoSchedule = startTaskAutoSchedule;
        return this;
    }

    public String getUserAssignLlmModel() {
        return userAssignLlmModel;
    }

    public GroupChatEngineConfig setUserAssignLlmModel(String userAssignLlmModel) {
        this.userAssignLlmModel = userAssignLlmModel;
        return this;
    }

    public Boolean isStartMessageDebugMode() {
        return startMessageDebugMode;
    }

    public GroupChatEngineConfig setStartMessageDebugMode(Boolean startMessageDebugMode) {
        this.startMessageDebugMode = startMessageDebugMode;
        return this;
    }

    public Boolean isStartGlobalSubtaskMode() {
        return startGlobalSubtaskMode != null && startGlobalSubtaskMode;

    }

    public GroupChatEngineConfig setStartGlobalSubtaskMode(Boolean startGlobalSubtaskMode) {
        this.startGlobalSubtaskMode = startGlobalSubtaskMode;
        return this;
    }

    public Boolean isStartAlwaysFreshContextMode() {
        return startAlwaysFreshContextMode != null && startAlwaysFreshContextMode;
    }

    public GroupChatEngineConfig setStartAlwaysFreshContextMode(Boolean startAlwaysFreshContextMode) {
        this.startAlwaysFreshContextMode = startAlwaysFreshContextMode;
        return this;
    }
    // ========================= GraphEngine模式配置 getter/setter =========================

    public Integer getMaxReActLoops() {
        return maxReActLoops;
    }

    public GroupChatEngineConfig setMaxReActLoops(Integer maxReActLoops) {
        this.maxReActLoops = maxReActLoops;
        return this;
    }

    public Map<String, Boolean> getAutoScheduleTaskConfig() {
        return autoScheduleTaskConfig;
    }

    public GroupChatEngineConfig setAutoScheduleTaskConfig(Map<String, Boolean> autoScheduleTaskConfig) {
        this.autoScheduleTaskConfig = autoScheduleTaskConfig;
        return this;
    }

    public long getContextTimeLimit() {
        return contextTimeLimit;
    }

    public GroupChatEngineConfig setContextTimeLimit(long contextTimeLimit) {
        this.contextTimeLimit = contextTimeLimit;
        return this;
    }


}
