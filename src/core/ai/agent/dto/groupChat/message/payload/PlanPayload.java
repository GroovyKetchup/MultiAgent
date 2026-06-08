package ai.agent.dto.groupChat.message.payload;

import ai.agent.dto.groupChat.plan.Plan;
import com.alibaba.fastjson2.annotation.JSONType;

/**
 * 计划消息载荷
 * 用于智能体制定的结构化计划
 */
@JSONType(typeName = "PLAN")
public class PlanPayload extends MessagePayload {
    private Plan plan;
    private String description; // 计划的文字描述

    public PlanPayload() {
    }

    public PlanPayload(Plan plan, String description) {
        this.plan = plan;
        this.description = description;
    }

    public Plan getPlan() {
        return plan;
    }

    public String getDescription() {
        return description;
    }

    public PlanPayload setPlan(Plan plan) {
        this.plan = plan;
        return this;
    }

    public PlanPayload setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getPayloadType() {
        return "PLAN";
    }

    @Override
    public boolean isValid() {
        return plan != null && plan.getSteps() != null && !plan.getSteps().isEmpty();
    }

    @Override
    public String toString() {
        return "PlanPayload{plan=" + plan + ", description='" + description + "'}";
    }
}
