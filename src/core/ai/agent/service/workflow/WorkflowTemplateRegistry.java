package ai.agent.service.workflow;

import ai.agent.dto.groupChat.message.Message;
import ai.agent.dto.groupChat.plan.Plan;
import ai.agent.dto.groupChat.plan.PlanStep;
import ai.agent.dto.workflow.WorkflowTemplate;
import ai.agent.engine.graph.GraphContext;
import ai.agent.util.ConsolePrintUtil;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Comment("工作流模板注册中心")
@ClassDeclare(
        label = "WorkflowTemplateRegistry",
        what = "管理所有工作流模板，提供查询和应用逻辑",
        why = "统一管理默认工作流，支持动态注册和查询",
        how = "通过Map存储模板，提供非侵入式的应用接口",
        developer = "裴硕",
        version = "1.0",
        createTime = "2025-12-14",
        updateTime = "2025-12-14"
)
public class WorkflowTemplateRegistry {

    private static final WorkflowTemplateRegistry INSTANCE = new WorkflowTemplateRegistry();

    private final Map<String, WorkflowTemplate> templateRegistry = new LinkedHashMap<>();

    private WorkflowTemplateRegistry() {
    }

    public static WorkflowTemplateRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册工作流模板
     */
    public void register(WorkflowTemplate template) {
        if (template == null || StrUtil.isBlank(template.getTaskName())) {
            return;
        }
        templateRegistry.put(template.getTaskName(), template);
        ConsolePrintUtil.printGreenLn("[工作流注册] 注册模板: " + template.getTaskName());
    }

    /**
     * 根据任务名称获取工作流模板
     */
    public WorkflowTemplate getByTaskName(String taskName) {
        return templateRegistry.get(taskName);
    }

    /**
     * 获取所有已注册的模板
     */
    public Map<String, WorkflowTemplate> getAllTemplates() {
        return new LinkedHashMap<>(templateRegistry);
    }

    /**
     * 清空所有模板（用于测试）
     */
    public void clear() {
        templateRegistry.clear();
    }

    // ========================= 非侵入式应用逻辑 =========================

    /**
     * 尝试应用工作流模板（非侵入式）
     * 检测内部触发消息格式：[[INTERNAL_PLAN_TRIGGER:agentId|taskName]]
     * 
     * @param ctx GraphContext
     * @param triggerMessage 触发消息
     * @return true=已应用工作流模板，false=无匹配模板
     */
    public boolean tryApplyWorkflowTemplate(GraphContext ctx, Message triggerMessage) {
        String text = triggerMessage.getTextPayload().getText();
        
        ConsolePrintUtil.printYellowLn("[工作流检测] 触发消息: " + text);
        
        // 检测是否为内部触发消息
        if (!text.contains("[[START_PREBUILT_PLAN:")) {
            ConsolePrintUtil.printYellowLn("[工作流检测] 非内部触发消息，跳过");
            return false;
        }

        ConsolePrintUtil.printGreenLn("[工作流检测] 检测到内部触发消息");
        
        // 提取任务名称
        String taskName = extractTaskName(text);
        ConsolePrintUtil.printYellowLn("[工作流检测] 提取的任务名称: " + taskName);
        
        if (StrUtil.isBlank(taskName)) {
            ConsolePrintUtil.printRedLn("[工作流检测] 任务名称为空");
            return false;
        }

        // 查找工作流模板
        WorkflowTemplate template = getByTaskName(taskName);
        ConsolePrintUtil.printYellowLn("[工作流检测] 查找模板结果: " + (template != null ? "找到" : "未找到"));
        
        if (template != null && template.getExecutionPlan() != null) {
            ConsolePrintUtil.printGreenLn("[工作流应用] 成功应用工作流模板: " + taskName);
            String planText = convertPlanToText(template.getExecutionPlan());
            setPlanToContext(ctx, planText);
            return true;
        }
        
        ConsolePrintUtil.printRedLn("[工作流应用] 未找到工作流模板: " + taskName);
        ConsolePrintUtil.printYellowLn("[工作流应用] 当前已注册的模板: " + templateRegistry.keySet());
        return false;
    }

    /**
     * 从内部触发消息中提取任务名称
     */
    private String extractTaskName(String text) {
        if (text.contains("[[START_PREBUILT_PLAN:")) {
            int start = text.indexOf("|") + 1;
            int end = text.indexOf("]]");
            if (start > 0 && end > start) {
                return text.substring(start, end);
            }
        }
        return null;
    }

    /**
     * 将Plan转换为文本格式
     */
    private String convertPlanToText(Plan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("执行计划：").append(plan.getGoal()).append("\n");
        List<PlanStep> steps = plan.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            sb.append(i + 1).append(". ").append(steps.get(i).getGoal()).append("\n");
            sb.append(StrUtil.format("{}\n", steps.get(i).getDescription()));
        }
        return sb.toString();
    }

    /**
     * 将计划设置到GraphContext
     */
    private void setPlanToContext(GraphContext ctx, String planContent) {
        ctx.putProcessData("$PLAN_CONTENT", planContent);
    }
}
