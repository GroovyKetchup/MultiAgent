package ai.agent.engine.groupChat.tool.impl.canvas.descriptor;

import ai.agent.dto.groupChat.message.Message;
import ai.agent.util.groupChat.MessageBuilder;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static ai.agent.enums.OperateMessageEnums.CANVAS_OPEN;

/**
 * 画布类型注册表
 * 管理所有可用的画布类型描述
 */
public class CanvasTypeRegistry {

    /**
     * 画布类型描述数据对象
     */
    public static class CanvasType {
        private final String type;
        private final String name;
        private final String description;
        private final String paramsStructure;
        private final String example;

        public CanvasType(String type, String name, String description, String paramsStructure, String example) {
            this.type = type;
            this.name = name;
            this.description = description;
            this.paramsStructure = paramsStructure;
            this.example = example;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getParamsStructure() {
            return paramsStructure;
        }

        public String getExample() {
            return example;
        }

        public String generateFullDescription() {
            StringBuilder sb = new StringBuilder();
            sb.append("【").append(name).append("】\n");
            sb.append("类型标识: ").append(type).append("\n");
            sb.append("说明: ").append(description).append("\n");
            if (paramsStructure != null && !paramsStructure.isEmpty()) {
                sb.append("参数结构:\n").append(paramsStructure).append("\n");
            }
            if (example != null && !example.isEmpty()) {
                sb.append("示例:\n").append(example).append("\n");
            }
            return sb.toString();
        }
    }

    private static final Map<String, CanvasType> REGISTRY = new LinkedHashMap<>();

    static {
        registerAllCanvasTypes();
    }


    public static Message mockAdjustPanelMessage(String senderId, String subSessionId) {

        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("canvasType", "adjust_panel");
        paramsMap.put("canvasParams", MapUtil.of("panelCode", "IML_00003"));

        // 如果是子会话，添加targetCanvasSessionId
        if (StrUtil.isNotBlank(subSessionId)) {
            paramsMap.put("targetCanvasSessionId", subSessionId);
        }

        return MessageBuilder.createOperateMessage(
                senderId,
                CANVAS_OPEN.toString(),
                paramsMap,
                null
        );

    }


    // ========================= 支撑方法 =========================

    private static void registerAllCanvasTypes() {

        registerPreviewIntent();
        registerAdjustPanel();
        registerSimpleAdjustDashboard();
        registerCustomDelivery();
        registerAutomationTest();
        registerCodeAgent();

    }

    private static void registerPreviewIntent() {
        JSONObject example = new JSONObject()
                .set("canvasType", "preview_intent")
                .set("canvasParams", new JSONObject());

        register("preview_intent", "意图确认",
                "意图确认界面可以管理以及查阅业务视角下的系统",
                "无需传递任何参数",
                JSONUtil.toJsonPrettyStr(example));
    }

    private static void registerAdjustPanel() {
        JSONObject paramsStructure = new JSONObject()
                .set("panelCode", "面板编号(必填)");

        JSONObject example = new JSONObject()
                .set("canvasType", "adjust_panel")
                .set("canvasParams", new JSONObject().set("panelCode", "IML_00003"));

        register("adjust_panel", "调整面板",
                "打开指定面板的调整页面，用于配置和调整面板参数",
                JSONUtil.toJsonPrettyStr(paramsStructure),
                JSONUtil.toJsonPrettyStr(example));
    }

    private static void registerSimpleAdjustDashboard() {
        JSONObject paramsStructure = new JSONObject()
                .set("panelCode", "面板编号(必填)");

        JSONObject example = new JSONObject()
                .set("canvasType", "standard_delivery")
                .set("canvasParams", new JSONObject().set("panelCode", "IML_00003"));

        register("standard_delivery", "标准交付",
                "打开指定数据看板的调整页面，用于快速配置数据看板参数",
                JSONUtil.toJsonPrettyStr(paramsStructure),
                JSONUtil.toJsonPrettyStr(example));
    }

    private static void registerCustomDelivery() {
        JSONObject paramsStructure = new JSONObject()
                .set("panelCode", "面板编号(必填)");

        JSONObject example = new JSONObject()
                .set("canvasType", "custom_delivery")
                .set("canvasParams", new JSONObject().set("panelCode", "IML_00003"));

        register("custom_delivery", "个性交付",
                "打开指定面板的个性交付页面，用于自定义面板的样式和交互",
                JSONUtil.toJsonPrettyStr(paramsStructure),
                JSONUtil.toJsonPrettyStr(example));
    }

    private static void registerAutomationTest() {
        JSONObject paramsStructure = new JSONObject()
                .set("targetPanelCode", "目标面板编号(必填，多个用英文逗号分隔)");

        JSONObject singleExample = new JSONObject()
                .set("canvasType", "automation_test")
                .set("canvasParams", new JSONObject().set("targetPanelCode", "IML_00003"));

        JSONObject multiExample = new JSONObject()
                .set("canvasType", "automation_test")
                .set("canvasParams", new JSONObject().set("targetPanelCode", "IML_00003,IML_00004,IML_00005"));

        String exampleStr = "单个面板:\n" + JSONUtil.toJsonPrettyStr(singleExample) +
                "\n\n多个面板并行测试:\n" + JSONUtil.toJsonPrettyStr(multiExample);

        register("automation_test", "自动化测试",
                "打开自动化测试页面，支持对一个或多个面板进行并行自动化测试和质量检查",
                JSONUtil.toJsonPrettyStr(paramsStructure),
                exampleStr);
    }

    private static void registerCodeAgent() {
        JSONObject example = new JSONObject()
                .set("canvasType", "code_agent")
                .set("canvasParams", new JSONObject());

        register("code_agent", "代码智能体",
                "打开AI辅助代码开发画布，提供代码生成、分析和修改等能力",
                "无需传递任何参数",
                JSONUtil.toJsonPrettyStr(example));
    }

    public static void register(String type, String name, String description, String paramsStructure, String example) {
        REGISTRY.put(type, new CanvasType(type, name, description, paramsStructure, example));
    }

    public static CanvasType get(String type) {
        return REGISTRY.get(type);
    }

    public static Set<String> getAllTypes() {
        return REGISTRY.keySet();
    }

    public static boolean isValidType(String type) {
        return REGISTRY.containsKey(type);
    }

    /**
     * 生成工具描述文本
     */
    public static String generateToolDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("打开画布(Canvas)来展示特定的页面内容。\n\n");
        desc.append("参数说明：\n");
        desc.append("- canvasType: 必填，画布类型，支持以下类型：\n");

        int index = 1;
        for (CanvasType canvasType : REGISTRY.values()) {
            desc.append(String.format("  %d. %s - %s\n",
                    index++,
                    canvasType.getType(),
                    canvasType.getName()
            ));
        }

        desc.append("\n- canvasParams: 可选，JSON对象，根据canvasType提供不同的参数\n\n");

        for (CanvasType canvasType : REGISTRY.values()) {
            desc.append(canvasType.generateFullDescription());
            desc.append("\n");
        }

        desc.append("注意事项：\n");
        desc.append("1. canvasType 必须是上述类型之一\n");
        desc.append("2. 请根据各类型的参数要求提供正确的 canvasParams\n");
        desc.append("3. 如果参数需要使用面板编号（PanelCode），务必使用工具[GetWorkCacheTool]获取正确的面板消息\n");
        desc.append("警告:\n");
        desc.append("1. 必须依照参数要求进行打开画布\n");
        desc.append("2. 如果当前画布是已经被打开的状态，请谨慎判断是否需要重复打开\n");

        return desc.toString();
    }
}
