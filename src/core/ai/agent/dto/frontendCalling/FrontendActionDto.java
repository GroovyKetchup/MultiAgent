package ai.agent.dto.frontendCalling;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.Map;

@Comment("前端动作dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-21", updateTime = "2025-11-21"
)
public class FrontendActionDto implements Serializable {

    // 动作名称
    private String actionName;
    // 动作别名
    private String actionAlias;
    // 动作参数
    private Map<String, Object> actionParameterSchema;
    // 动作描述
    private String actionDescription;
    // 动作返回值
    private Map<String, Object> actionReturnSchema;

    // 是异步响应
    private boolean isAsyncResponse;

    public String getActionName() {
        return actionName;
    }

    public FrontendActionDto setActionName(String actionName) {
        this.actionName = actionName;
        return this;
    }

    public String getActionAlias() {
        return actionAlias;
    }

    public FrontendActionDto setActionAlias(String actionAlias) {
        this.actionAlias = actionAlias;
        return this;
    }

    public Map<String, Object> getActionParameterSchema() {
        return actionParameterSchema;
    }

    public FrontendActionDto setActionParameterSchema(Map<String, Object> actionParameterSchema) {
        this.actionParameterSchema = actionParameterSchema;
        return this;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public FrontendActionDto setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
        return this;
    }

    public boolean isAsyncResponse() {
        return isAsyncResponse;
    }

    public FrontendActionDto setAsyncResponse(boolean asyncResponse) {
        isAsyncResponse = asyncResponse;
        return this;
    }

    public Map<String, Object> getActionReturnSchema() {
        return actionReturnSchema;
    }

    public FrontendActionDto setActionReturnSchema(Map<String, Object> actionReturnSchema) {
        this.actionReturnSchema = actionReturnSchema;
        return this;
    }
}
