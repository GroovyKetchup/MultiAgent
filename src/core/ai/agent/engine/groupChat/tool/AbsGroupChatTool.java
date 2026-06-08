package ai.agent.engine.groupChat.tool;

import ai.agent.annotation.ParamDeclare;
import ai.agent.annotation.ToolDeclare;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.engine.groupChat.model.instance.AgentInstance;
import ai.agent.engine.groupChat.model.instance.GroupChatInstance;
import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbsGroupChatTool implements Tool {

    protected transient GroupChatToolContextAdapter context;
    protected transient GroupChatEngine engine;
    protected transient String currentAgentId;
    protected transient String groupInstanceId;

    @Override
    public final String execute(ToolContext ctx, Map<String, Object> params) {
        this.context = convertToGroupChatToolContext(ctx);
        if (this.context == null) {
            return "Error: 无效的工具上下文，该工具仅支持GroupChat环境";
        }

        this.engine = context.getChatEngine();
        this.currentAgentId = context.getCurrentAgentId();
        this.groupInstanceId = context.getGroupInstanceId();

        try {
            String validationError = validateAndInjectParams(params);
            if (validationError != null) {
                return validationError;
            }

            return executeInternal();

        } catch (Exception e) {
            return "Error: 工具执行失败 - " + e.getMessage();
        }
    }

    protected abstract String executeInternal();

    private String validateAndInjectParams(Map<String, Object> params) {
        List<String> missingParams = new ArrayList<>();
        List<String> invalidParams = new ArrayList<>();

        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ParamDeclare.class)) {
                ParamDeclare paramAnnotation = field.getAnnotation(ParamDeclare.class);
                String fieldName = field.getName();

                Object paramValue = params != null ? params.get(fieldName) : null;

                if (paramValue == null || "null".equals(String.valueOf(paramValue))) {
                    if (paramAnnotation.required()) {
                        missingParams.add(String.format("%s (%s)", fieldName, paramAnnotation.description()));
                    } else if (!paramAnnotation.defaultValue().isEmpty()) {
                        paramValue = paramAnnotation.defaultValue();
                    }
                }

                if (paramValue != null && !"null".equals(String.valueOf(paramValue))) {
                    try {
                        field.setAccessible(true);
                        Object convertedValue = convertParamValue(paramValue, field.getType(), paramAnnotation);
                        field.set(this, convertedValue);
                    } catch (Exception e) {
                        invalidParams.add(String.format("%s (类型转换失败: %s)", fieldName, e.getMessage()));
                    }
                }
            }
        }

        if (!missingParams.isEmpty()) {
            return String.format("Error: 缺少必填参数: %s", String.join(", ", missingParams));
        }

        if (!invalidParams.isEmpty()) {
            return String.format("Error: 参数格式错误: %s", String.join(", ", invalidParams));
        }

        List<String> unexpectedParams = findUnexpectedParams(params);
        if (!unexpectedParams.isEmpty()) {
            return String.format("Warning: 检测到未定义的参数: %s。请检查参数名称是否正确。\n可用参数: %s",
                    String.join(", ", unexpectedParams),
                    getAvailableParamNames());
        }

        return null;
    }

    private Object convertParamValue(Object value, Class<?> targetType, ParamDeclare paramAnnotation) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (paramAnnotation.isObject()) {
            if (value instanceof Map) {
                return cn.hutool.json.JSONUtil.toBean(
                        cn.hutool.json.JSONUtil.toJsonStr(value),
                        targetType
                );
            } else if (value instanceof String) {
                return cn.hutool.json.JSONUtil.toBean((String) value, targetType);
            } else if (value instanceof java.util.List) {
                return cn.hutool.json.JSONUtil.toBean(
                        cn.hutool.json.JSONUtil.toJsonStr(value),
                        targetType
                );
            }
            return value;
        }

        String strValue = String.valueOf(value);

        if (targetType == String.class) {
            return strValue;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(strValue);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(strValue);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(strValue);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(strValue);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(strValue);
        }

        return value;
    }

    private List<String> findUnexpectedParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> declaredParams = new ArrayList<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ParamDeclare.class)) {
                declaredParams.add(field.getName());
            }
        }

        List<String> unexpectedParams = new ArrayList<>();
        for (String paramName : params.keySet()) {
            if (!declaredParams.contains(paramName)) {
                unexpectedParams.add(paramName);
            }
        }

        return unexpectedParams;
    }

    private String getAvailableParamNames() {
        List<String> paramNames = new ArrayList<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ParamDeclare.class)) {
                ParamDeclare annotation = field.getAnnotation(ParamDeclare.class);
                paramNames.add(String.format("%s (%s)", field.getName(), annotation.description()));
            }
        }
        return String.join(", ", paramNames);
    }

    @Override
    public String getName() {
        ToolDeclare declare = this.getClass().getAnnotation(ToolDeclare.class);
        return declare != null ? declare.name() : this.getClass().getSimpleName();
    }

    @Override
    public String getCnName() {
        ToolDeclare declare = this.getClass().getAnnotation(ToolDeclare.class);
        return declare != null ? declare.cnName() : getName();
    }

    @Override
    public String getDescription() {
        ToolDeclare declare = this.getClass().getAnnotation(ToolDeclare.class);
        return declare != null ? declare.description() : "";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return generateSchemaFromParamDeclare();
    }

    private Map<String, Object> generateSchemaFromParamDeclare() {
        java.util.Map<String, Object> schema = new java.util.HashMap<>();
        schema.put("type", "object");

        java.util.Map<String, Object> properties = new java.util.HashMap<>();
        List<String> required = new ArrayList<>();

        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ParamDeclare.class)) {
                ParamDeclare annotation = field.getAnnotation(ParamDeclare.class);
                String paramName = field.getName();

                java.util.Map<String, Object> paramDef;

                if (annotation.isObject()) {
                    paramDef = ai.agent.util.JsonSchemaGenerator.generateSchema(field.getType());
                    paramDef.put("description", annotation.description());
                } else {
                    paramDef = new java.util.HashMap<>();
                    paramDef.put("type", annotation.type());
                    paramDef.put("description", annotation.description());

                    if (annotation.enumValues().length > 0) {
                        paramDef.put("enum", annotation.enumValues());
                    }
                }

                properties.put(paramName, paramDef);

                if (annotation.required()) {
                    required.add(paramName);
                }
            }
        }

        schema.put("properties", properties);
        schema.put("required", required);

        return schema;
    }


    // 获取业务域编号
    protected String getBusDomainCode() {
        try {
            return engine.getBusDomain().getDomainCode();
        } catch (Exception e) {
            return null;
        }
    }


    // 获取智能体实例
    protected AgentInstance getAgentInstance() {
        if (engine == null || StrUtil.isBlank(currentAgentId)) {
            return null;
        }
        return getGroupInstance().getAgent(currentAgentId);
    }

    // 获取群聊实例
    protected GroupChatInstance getGroupInstance() {
        if (engine == null) {
            return null;
        }
        return engine.getGroupChatInstance();
    }


}
