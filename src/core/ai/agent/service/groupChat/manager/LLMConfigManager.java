package ai.agent.service.groupChat.manager;


import ai.agent.constant.AppConstants;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.util.ConsolePrintUtil;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Comment("大模型配置管理类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-09", updateTime = "2025-09-09"
)
public class LLMConfigManager {

    public static final String AUTO_MODEL_CONFIG_NAME_FAST = "默认模型(快速)";
    public static final String AUTO_MODEL_CONFIG_NAME_DEFAULT = "默认模型(标准)";
    public static final String AUTO_MODEL_CONFIG_NAME_HIGH_PERFORMANCE = "默认模型(高性能)";


    // 系统支持的默认大模型配置
    private static Map<String /* 模型名称 */, LLMConfig> SYSTEM_SUPPORT_LLM_MODELS = new ConcurrentHashMap<>();


    static {

        try {
            loadModels();
        } catch (Exception e) {
            ConsolePrintUtil.printRedLn(StrUtil
                    .format("LLMConfigManager: 初始化LLMConfig失败！{}", e.getMessage()));
        }

    }

    // ========================= 系统方法 =========================

    public static void loadModels() {

        ConsolePrintUtil.printGreenLn(StrUtil.format("正在初始化LLMConfig..."));

        SYSTEM_SUPPORT_LLM_MODELS = new ConcurrentHashMap<>();
        try (IDao dao = IDaoService.newIDao()) {
            List<LLMConfig> llmConfigs = OrchManager.queryAllLLMConfig(dao);
            if (!CollUtil.isEmpty(llmConfigs)) {
                for (LLMConfig llmConfig : llmConfigs) {
                    String modelName = llmConfig.getModel();
                    if (StrUtil.isBlank(modelName)) continue;
                    SYSTEM_SUPPORT_LLM_MODELS.put(
                            modelName,
                            llmConfig
                    );
                }
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        ConsolePrintUtil.printGreenLn(StrUtil.format("初始化LLMConfig完成！已入库{}个模型, 列表如下:{}", SYSTEM_SUPPORT_LLM_MODELS.size(),
                JSONUtil.toJsonStr(SYSTEM_SUPPORT_LLM_MODELS.keySet())));
    }


// ========================= 暴漏的外部方法 =========================


    // 返回系统提供的模型列表
    public static Set<String> getSystemSupportModelNames() {
        return SYSTEM_SUPPORT_LLM_MODELS.keySet();
    }

    // 是否支持某个模型
    public static boolean isSupportModel(String llmModelName) {
        return SYSTEM_SUPPORT_LLM_MODELS.containsKey(llmModelName);
    }


    // 获取系统配置
    public static LLMConfig getLlmConfig(String llmModelName) {
        if (!isSupportModel(llmModelName)) {
            throw new RuntimeException(StrUtil.format("系统不支持模型[{}]", llmModelName));
        }

        return SYSTEM_SUPPORT_LLM_MODELS.get(llmModelName);

    }

    // 获取系统配置
    public static LLMConfig getLlmConfigByConfigName(String configName) {
        if (StrUtil.isBlank(configName)) return null;
        if (SYSTEM_SUPPORT_LLM_MODELS.isEmpty()) return null;
        for (LLMConfig config : SYSTEM_SUPPORT_LLM_MODELS.values()) {
            if (config.getConfigName().equals(configName)) {
                return config;
            }

        }
        return null;
    }


    // 获取系统AUTO模型
    public static LLMConfig getAutoLlmConfig(AutoModelType type) {

        try {
            // 先根据这里的类型进行选择
            LLMConfig llmConfig = getLlmConfigByConfigName(type.getConfigName());
            if (llmConfig != null) return llmConfig;

            // 如果默认模型不存在，那么就使用硬编码的默认模型
            List<String> preferModelNames = CollUtil.newArrayList(
                    AppConstants.LLM_MODEL_NAME_DEEPSEEK_CHAT
            );

            return getAutoLlmConfig(preferModelNames);
        } catch (Exception e) {
            return null;
        }

    }

    // 获取系统AUTO模型
    public static LLMConfig getAutoLlmConfig(List<String> preferModelNames) {

        List<String> supportModelNames = new ArrayList<>(getSystemSupportModelNames());

        if (CollUtil.isEmpty(supportModelNames)) throw new RuntimeException("系统中没有任何可用的大模型配置！");

        ConsolePrintUtil.printWhiteLn(StrUtil.format("系统支持的模型列表:{}", JSONUtil.toJsonStr(supportModelNames)));
        ConsolePrintUtil.printWhiteLn(StrUtil.format("优先使用的模型:{}", JSONUtil.toJsonStr(preferModelNames)));


        // 1、先拿优先的模型
        LLMConfig fianlLlmConfig = null;
        for (String preferModelName : preferModelNames) {
            LLMConfig llmConfig = null;

            try {
                llmConfig = getLlmConfig(preferModelName);
            } catch (Exception ignored) {
            }
            if (llmConfig != null) {
                fianlLlmConfig = llmConfig;
                break;
            }
        }

        // 2、优先的模型都没有，那就取第一个
        String finalLlmConfigName = supportModelNames.get(0);
        ConsolePrintUtil.printWhiteLn(StrUtil.format("准备使用模型:{}",
                finalLlmConfigName));
        if (fianlLlmConfig == null) {
            try {
                fianlLlmConfig = getLlmConfig(finalLlmConfigName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        ConsolePrintUtil.printWhiteLn(StrUtil.format("最终选择的模型:{}",
                finalLlmConfigName));


        return fianlLlmConfig;

    }


    public enum AutoModelType {
        @JSONField(name = "FAST")
        FAST("默认模型(快速)"),
        @JSONField(name = "DEFAULT")
        DEFAULT("默认模型(标准)"),
        @JSONField(name = "HIGH_PERFORMANCE")
        HIGH_PERFORMANCE("默认模型(高性能)"),
        ;

        private final String configName;

        AutoModelType(String configName) {
            this.configName = configName;
        }

        public String getConfigName() {
            return configName;
        }
    }
}
