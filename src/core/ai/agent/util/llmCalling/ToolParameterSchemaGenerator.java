package ai.agent.util.llmCalling;

import ai.agent.annotation.ToolParameter;
import ai.agent.engine.groupChat.tool.Tool;
import ai.agent.util.ConsolePrintUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具参数Schema生成器
 * 基于@ToolParameter注解自动生成Function Calling的参数Schema
 */
public class ToolParameterSchemaGenerator {

    /**
     * 从工具类中提取参数Schema
     * 
     * @param toolClass 工具类
     * @return 参数Schema的Map表示
     */
    public static Map<String, Object> generateSchema(Class<? extends Tool> toolClass) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        // 遍历所有字段，查找带有@ToolParameter注解的字段
        Field[] fields = toolClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ToolParameter.class)) {
                ToolParameter annotation = field.getAnnotation(ToolParameter.class);
                
                try {
                    // 获取参数名称（字段的值）
                    field.setAccessible(true);
                    Object fieldValue = field.get(null); // 静态字段
                    if (fieldValue instanceof String) {
                        String paramName = (String) fieldValue;
                        
                        // 构建参数定义
                        Map<String, Object> paramDef = new HashMap<>();
                        paramDef.put("type", annotation.type());
                        paramDef.put("description", annotation.description());
                        
                        // 处理枚举值
                        if (annotation.enumValues().length > 0) {
                            paramDef.put("enum", annotation.enumValues());
                        }
                        
                        // 处理数组类型
                        if ("array".equals(annotation.type())) {
                            Map<String, Object> items = new HashMap<>();
                            items.put("type", annotation.itemType());
                            paramDef.put("items", items);
                        }
                        
                        properties.put(paramName, paramDef);
                        
                        // 处理必填项
                        if (annotation.required()) {
                            required.add(paramName);
                        }
                    }
                } catch (IllegalAccessException e) {
                    ConsolePrintUtil.printRedLn("无法访问字段: " + field.getName());
                }
            }
        }
        
        schema.put("properties", properties);
        schema.put("required", required);
        
        return schema;
    }

    /**
     * 从工具实例中提取参数Schema
     * 
     * @param tool 工具实例
     * @return 参数Schema的Map表示
     */
    public static Map<String, Object> generateSchema(Tool tool) {
        return generateSchema(tool.getClass());
    }
}
