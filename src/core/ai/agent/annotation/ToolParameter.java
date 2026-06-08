package ai.agent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具参数注解
 * 用于标注工具类中的参数常量，自动生成Function Calling的参数Schema
 * 
 * 使用示例：
 * <pre>
 * public class MyTool implements Tool {
 *     @ToolParameter(description = "用户名称", required = true)
 *     public static final String PARAM_NAME = "name";
 *     
 *     @ToolParameter(description = "用户年龄", type = "integer")
 *     public static final String PARAM_AGE = "age";
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ToolParameter {
    
    /**
     * 参数描述
     */
    String description();
    
    /**
     * 参数类型：string, integer, number, boolean, array, object
     */
    String type() default "string";
    
    /**
     * 是否必填
     */
    boolean required() default false;
    
    /**
     * 枚举值（仅当type为string时有效）
     */
    String[] enumValues() default {};
    
    /**
     * 数组元素类型（仅当type为array时有效）
     */
    String itemType() default "string";
}
