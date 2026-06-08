package ai.agent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ParamDeclare {
    
    String description();
    
    boolean required() default false;
    
    String defaultValue() default "";
    
    String type() default "string";
    
    String[] enumValues() default {};
    
    boolean isObject() default false;
}
