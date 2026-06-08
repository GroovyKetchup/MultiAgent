package ai.agent.util;

import org.nutz.dao.entity.annotation.Comment;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonSchemaGenerator {

    public static Map<String, Object> generateSchema(Class<?> clazz) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().startsWith("this$")) continue;
            
            String fieldName = field.getName();
            Map<String, Object> fieldSchema = generateFieldSchema(field);
            properties.put(fieldName, fieldSchema);
            
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null && fieldSchema.get("description") == null) {
                fieldSchema.put("description", comment.value());
            }
        }
        
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        
        return schema;
    }
    
    private static Map<String, Object> generateFieldSchema(Field field) {
        Map<String, Object> fieldSchema = new HashMap<>();
        
        Class<?> fieldType = field.getType();
        
        if (fieldType == String.class) {
            fieldSchema.put("type", "string");
        } else if (fieldType == Integer.class || fieldType == int.class) {
            fieldSchema.put("type", "integer");
        } else if (fieldType == Long.class || fieldType == long.class) {
            fieldSchema.put("type", "integer");
        } else if (fieldType == Double.class || fieldType == double.class || 
                   fieldType == Float.class || fieldType == float.class) {
            fieldSchema.put("type", "number");
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            fieldSchema.put("type", "boolean");
        } else if (List.class.isAssignableFrom(fieldType)) {
            fieldSchema.put("type", "array");
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericType;
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    Class<?> itemClass = (Class<?>) typeArgs[0];
                    if (isComplexType(itemClass)) {
                        fieldSchema.put("items", generateSchema(itemClass));
                    } else {
                        Map<String, Object> itemSchema = new HashMap<>();
                        itemSchema.put("type", getSimpleTypeName(itemClass));
                        fieldSchema.put("items", itemSchema);
                    }
                }
            }
        } else if (isComplexType(fieldType)) {
            fieldSchema = generateSchema(fieldType);
        } else {
            fieldSchema.put("type", "string");
        }
        
        Comment comment = field.getAnnotation(Comment.class);
        if (comment != null) {
            fieldSchema.put("description", comment.value());
        }
        
        return fieldSchema;
    }
    
    private static boolean isComplexType(Class<?> clazz) {
        return !clazz.isPrimitive() && 
               !clazz.getName().startsWith("java.lang") &&
               !clazz.getName().startsWith("java.util") &&
               !clazz.isEnum();
    }
    
    private static String getSimpleTypeName(Class<?> clazz) {
        if (clazz == String.class) return "string";
        if (clazz == Integer.class || clazz == int.class) return "integer";
        if (clazz == Long.class || clazz == long.class) return "integer";
        if (clazz == Double.class || clazz == double.class) return "number";
        if (clazz == Float.class || clazz == float.class) return "number";
        if (clazz == Boolean.class || clazz == boolean.class) return "boolean";
        return "string";
    }
}
