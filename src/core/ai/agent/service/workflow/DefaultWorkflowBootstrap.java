package ai.agent.service.workflow;

import ai.agent.dto.groupChat.plan.Plan;
import ai.agent.dto.groupChat.plan.PlanStep;
import ai.agent.dto.groupChat.taskboard.TaskBoardItem;
import ai.agent.dto.workflow.WorkflowTemplate;
import ai.agent.engine.groupChat.GroupChatEngine;
import ai.agent.util.ConsolePrintUtil;
import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ai.agent.constant.AppConstants.*;

@Comment("默认工作流启动器")
@ClassDeclare(
        label = "DefaultWorkflowBootstrap",
        what = "系统启动时加载和注册所有默认工作流模板",
        why = "提供开箱即用的默认工作流",
        how = "在GroupChatEngine初始化时调用bootstrap方法",
        developer = "裴硕",
        version = "1.0",
        createTime = "2025-12-14",
        updateTime = "2025-12-14"
)
public class DefaultWorkflowBootstrap {

    private static final String WORKFLOW_REQUIREMENT_STRUCTURING = "需求结构化任务";
    private static final String WORKFLOW_PROTOTYPE_DELIVERY = "原型交付任务";

    /**
     * 启动：注册所有默认工作流模板
     */
    public static void bootstrap() {
        ConsolePrintUtil.printGreenLn("[工作流启动] 开始注册默认工作流模板...");
        
        WorkflowTemplateRegistry registry = WorkflowTemplateRegistry.getInstance();
        
        // 注册需求结构化工作流
        registry.register(buildRequirementStructuringWorkflow());
        
        // 注册原型交付工作流
        registry.register(buildPrototypeDeliveryWorkflow());
        
        ConsolePrintUtil.printGreenLn("[工作流启动] 默认工作流模板注册完成");
    }

    /**
     * 加载默认工作流配置
     */
    public static Map<String, Boolean> loadDefaultWorkflowConfig() {
        Map<String, Boolean> config = new LinkedHashMap<>();
        config.put(WORKFLOW_REQUIREMENT_STRUCTURING, true);
        config.put(WORKFLOW_PROTOTYPE_DELIVERY, true);
        return config;
    }

    /**
     * 根据配置生成初始任务
     */
    public static List<TaskBoardItem> generateInitialTasks(GroupChatEngine engine) {
        List<TaskBoardItem> tasks = new ArrayList<>();
        
        Map<String, Boolean> config = engine.getEngineConfig().getAutoScheduleTaskConfig();
        if (config == null) {
            return tasks;
        }

        WorkflowTemplateRegistry registry = WorkflowTemplateRegistry.getInstance();
        
        // 遍历所有已注册的工作流模板
        for (Map.Entry<String, WorkflowTemplate> entry : registry.getAllTemplates().entrySet()) {
            String taskName = entry.getKey();
            WorkflowTemplate template = entry.getValue();
            
            // 检查是否启用该工作流
            if (config.getOrDefault(taskName, false)) {
                TaskBoardItem task = engine.createTask(
                        template.getTaskName(),
                        template.getTaskDescription(),
                        AGENT_ID_SYSTEM_SCHEDULER,
                        template.getAssigneeAgentId()
                );
                task.setCreateTime(Instant.now());
                tasks.add(task);
                ConsolePrintUtil.printGreenLn("[工作流启动] 创建初始任务: " + taskName);
            }
        }
        
        return tasks;
    }

    // ========================= 工作流模板定义 =========================

    /**
     * 需求结构化工作流
     */
    private static WorkflowTemplate buildRequirementStructuringWorkflow() {
        Plan plan = new Plan("对用户所上传的文档进行解析");
        plan.addStep(new PlanStep("1", "查看我的任务", "调用MyTasksTool查看我的任务列表"));
        plan.addStep(new PlanStep("2", "将任务标记为进行中", "调用UpdateTaskStatusTool将任务状态更新为IN_PROGRESS"));
        plan.addStep(new PlanStep("3", "解析上传的需求文档", "调用VotaForge_需求文档解析解析上传的需求文档"));
        plan.addStep(new PlanStep("4", "将任务标记为已完成", "调用UpdateTaskStatusTool将任务状态更新为COMPLETED"));

        String taskDescription = "先根据聊天记录看下用户有没有上传附件。在附件收到后，使用工具[VotaForge_需求文档解析]下发文档解析任务，你不需要管解析的结果，只要工具没有错误就是成功。然后最后你也不需要进行汇报，标记任务已完成就好。";

        return new WorkflowTemplate(
                "WORKFLOW_REQ_STRUCT",
                WORKFLOW_REQUIREMENT_STRUCTURING,
                taskDescription,
                AGENT_ID_REQUIREMENT_STRUCTURING,
                plan
        );
    }

    /**
     * 原型交付工作流
     */
    private static WorkflowTemplate buildPrototypeDeliveryWorkflow() {
        Plan plan = new Plan("对已解析的文档执行原型交付");
        plan.addStep(new PlanStep("1", "查看我的任务", "调用MyTasksTool查看我的任务列表"));
        plan.addStep(new PlanStep("2", "将任务标记为进行中", "调用UpdateTaskStatusTool将任务状态更新为IN_PROGRESS"));
        plan.addStep(new PlanStep("3", "执行原型交付", "调用VotaForge_原型交付执行交付"));
        plan.addStep(new PlanStep("4", "将任务标记为已完成", "调用UpdateTaskStatusTool将任务状态更新为COMPLETED"));

        String taskDescription = "调用工具[VotaForge_原型交付]；随后调用工具[系统_通知用户]告知用户可以预览";

        return new WorkflowTemplate(
                "WORKFLOW_PROTO_DELIVERY",
                WORKFLOW_PROTOTYPE_DELIVERY,
                taskDescription,
                AGENT_ID_ROUGH_DELIVERY,
                plan
        );
    }
}
