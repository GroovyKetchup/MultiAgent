package ai.agent.engine.groupChat.model.definition;

import ai.agent.constant.GroupChatConstants;
import com.kwaidoo.ms.tool.CmnUtil;
import gpf.adur.data.Form;

import java.io.Serializable;

/**
 * 系统设置定义类
 */
public class SystemSettingsDefinition implements Serializable {

    public static final String FORM_MODEL_ID = GroupChatConstants.FormModelId_SystemSettings;

    // 配置名称
    private String configName;
    // 生效规则
    private String effectRule;
    // 配置表
    private String configTable;
    // 创建时间
    private Long createTime;
    // 修改时间
    private Long updateTime;
    // 是否启用
    private boolean enabled;

    public static SystemSettingsDefinition fromForm(Form form) throws Exception {
        if (form == null) return null;
        SystemSettingsDefinition def = new SystemSettingsDefinition();
        def.configName = form.getString("配置名称");
        def.effectRule = form.getString("生效规则");
        def.configTable = form.getString("配置表");
        def.createTime = form.getLong("创建时间");
        def.updateTime = form.getLong("修改时间");
        def.enabled = CmnUtil.getBoolean(form.getBoolean("是否启用"), false);
        return def;
    }

    public String getConfigName() {
        return configName;
    }

    public SystemSettingsDefinition setConfigName(String configName) {
        this.configName = configName;
        return this;
    }

    public String getEffectRule() {
        return effectRule;
    }

    public SystemSettingsDefinition setEffectRule(String effectRule) {
        this.effectRule = effectRule;
        return this;
    }

    public String getConfigTable() {
        return configTable;
    }

    public SystemSettingsDefinition setConfigTable(String configTable) {
        this.configTable = configTable;
        return this;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public SystemSettingsDefinition setCreateTime(Long createTime) {
        this.createTime = createTime;
        return this;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public SystemSettingsDefinition setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SystemSettingsDefinition setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
