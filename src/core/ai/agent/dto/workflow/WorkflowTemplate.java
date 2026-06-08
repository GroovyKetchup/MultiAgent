package ai.agent.dto.workflow;

import ai.agent.dto.groupChat.plan.Plan;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("工作流模板")
@ClassDeclare(
        label = "WorkflowTemplate",
        what = "默认工作流模板，包含任务定义和执行计划",
        why = "提供确定性的任务执行流程",
        how = "通过模板ID匹配并应用预制的执行计划",
        developer = "裴硕",
        version = "1.0",
        createTime = "2025-12-14",
        updateTime = "2025-12-14"
)
public class WorkflowTemplate implements Serializable {

    private String templateId;
    private String taskName;
    private String taskDescription;
    private String assigneeAgentId;
    private Plan executionPlan;

    public WorkflowTemplate() {
    }

    public WorkflowTemplate(String templateId, String taskName, String taskDescription,
                            String assigneeAgentId, Plan executionPlan) {
        this.templateId = templateId;
        this.taskName = taskName;
        this.taskDescription = taskDescription;
        this.assigneeAgentId = assigneeAgentId;
        this.executionPlan = executionPlan;
    }

    public String getTemplateId() {
        return templateId;
    }

    public WorkflowTemplate setTemplateId(String templateId) {
        this.templateId = templateId;
        return this;
    }

    public String getTaskName() {
        return taskName;
    }

    public WorkflowTemplate setTaskName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public WorkflowTemplate setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
        return this;
    }

    public String getAssigneeAgentId() {
        return assigneeAgentId;
    }

    public WorkflowTemplate setAssigneeAgentId(String assigneeAgentId) {
        this.assigneeAgentId = assigneeAgentId;
        return this;
    }

    public Plan getExecutionPlan() {
        return executionPlan;
    }

    public WorkflowTemplate setExecutionPlan(Plan executionPlan) {
        this.executionPlan = executionPlan;
        return this;
    }
}
