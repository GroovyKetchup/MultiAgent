package ai.agent.util.groupChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具参数Schema构建器
 * 用于快速构建符合OpenAI Function Calling规范的参数定义
 * 
 * 使用示例：
 * <pre>
 * Map<String, Object> schema = ToolSchemaBuilder.create()
 *     .addStringParam("name", "用户名称", true)
 *     .addIntegerParam("age", "用户年龄", false)
 *     .addEnumParam("status", "状态", new String[]{"active", "inactive"}, true)
 *     .build();
 * </pre>
 */
public class ToolSchemaBuilder {
    
    private final Map<String, Object> schema;
    private final Map<String, Object> properties;
    private final List<String> required;
    
    private ToolSchemaBuilder() {
        this.schema = new HashMap<>();
        this.properties = new HashMap<>();
        this.required = new ArrayList<>();
        
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
    }
    
    /**
     * 创建一个新的Schema构建器
     */
    public static ToolSchemaBuilder create() {
        return new ToolSchemaBuilder();
    }
    
    /**
     * 添加字符串参数
     */
    public ToolSchemaBuilder addStringParam(String name, String description, boolean isRequired) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "string");
        prop.put("description", description);
        properties.put(name, prop);
        
        if (isRequired) {
            required.add(name);
        }
        
        return this;
    }
    
    /**
     * 添加整数参数
     */
    public ToolSchemaBuilder addIntegerParam(String name, String description, boolean isRequired) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "integer");
        prop.put("description", description);
        properties.put(name, prop);
        
        if (isRequired) {
            required.add(name);
        }
        
        return this;
    }
    
    /**
     * 添加数字参数
     */
    public ToolSchemaBuilder addNumberParam(String name, String description, boolean isRequired) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "number");
        prop.put("description", description);
        properties.put(name, prop);
        
        if (isRequired) {
            required.add(name);
        }
        
        return this;
    }
    
    /**
     * 添加布尔参数
     */
    public ToolSchemaBuilder addBooleanParam(String name, String description, boolean isRequired) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "boolean");
        prop.put("description", description);
        properties.put(name, prop);
        
        if (isRequired) {
            required.add(name);
        }
        
        return this;
    }
    
    /**
     * 添加枚举参数
     */
    public ToolSchemaBuilder addEnumParam(String name, String description, String[] enumValues, boolean isRequired) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "string");
        prop.put("description", description);
        prop.put("enum", enumValues);
        properties.put(name, prop);
        
        if (isRequired) {
            required.add(name);
        }
        
        return this;
    }
    
    /**
     * 添加数组参数
     */
    public ToolSchemaBuilder addArrayParam(String name, String description, String itemType, boolean isRequired) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "array");
        prop.put("description", description);
        
        Map<String, Object> items = new HashMap<>();
        items.put("type", itemType);
        prop.put("items", items);
        
        properties.put(name, prop);
        
        if (isRequired) {
            required.add(name);
        }
        
        return this;
    }
    
    /**
     * 添加对象参数
     */
    public ToolSchemaBuilder addObjectParam(String name, String description, Map<String, Object> objectSchema, boolean isRequired) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "object");
        prop.put("description", description);
        prop.putAll(objectSchema);
        properties.put(name, prop);
        
        if (isRequired) {
            required.add(name);
        }
        
        return this;
    }
    
    /**
     * 构建最终的Schema
     */
    public Map<String, Object> build() {
        return schema;
    }
    
    /**
     * 创建一个空参数的Schema（工具无需参数时使用）
     */
    public static Map<String, Object> createEmptySchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        schema.put("required", new ArrayList<>());
        return schema;
    }
}
