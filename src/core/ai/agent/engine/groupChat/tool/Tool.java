package ai.agent.engine.groupChat.tool;

import ai.agent.dto.llmCalling.ToolDto;
import ai.agent.engine.groupChat.adapter.GroupChatToolContextAdapter;
import ai.agent.util.llmCalling.ToolParameterSchemaGenerator;

import java.io.Serializable;
import java.util.Map;

public interface Tool extends Serializable {


    String getName();

    String getCnName();

    String getDescription();

    String execute(ToolContext ctx, Map<String, Object> params);




    /**
     * 获取工具参数的JSON Schema定义
     * 用于Function Calling，返回符合OpenAI Function Calling规范的参数定义
     * <p>
     * 默认实现：通过@ToolParameter注解自动生成Schema
     * 如果工具类中没有使用注解，可以重写此方法手动返回Schema
     *
     * @return 参数schema的Map表示，如果工具无参数则返回null
     */
    default Map<String, Object> getParameterSchema() {
        // 尝试通过注解自动生成Schema
        Map<String, Object> schema = ToolParameterSchemaGenerator.generateSchema(this);

        // 如果没有找到任何参数定义，返回null
        if (schema != null && schema.containsKey("properties")) {
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            if (properties.isEmpty()) {
                return null;
            }
        }

        return schema;
    }


    // 转换成dto
    default ToolDto convertToDto() {
        return new ToolDto()
                .setName(getName())
                .setCnName(getCnName())
                .setDescription(getDescription())
                .setClassPath(this.getClass().getName())
                ;

    }

    default GroupChatToolContextAdapter convertToGroupChatToolContext(ToolContext ctx) {
        if (ctx instanceof GroupChatToolContextAdapter) {
            return (GroupChatToolContextAdapter) ctx;
        }

        return null;
    }


    default String getCreatorId(ToolContext ctx) {
        if (ctx instanceof GroupChatToolContextAdapter) {
            GroupChatToolContextAdapter adapter =
                    (GroupChatToolContextAdapter) ctx;
            String agentId = adapter.getCurrentAgentId();
            return agentId != null ? agentId : "system";
        }
        return "system";
    }


    /**
     * 获取群组实例ID
     */
    default String getGroupInstanceId(ToolContext ctx) {
        if (ctx instanceof GroupChatToolContextAdapter) {
            return ((GroupChatToolContextAdapter) ctx).getGroupInstanceId();
        }
        return null;
    }


}

