package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.PlannerService;
import org.springframework.stereotype.Service;

@Service
public class RulePlannerService implements PlannerService {
    @Override
    public AgentPlan createPlan(AgentTaskContext context) {
        if (!"daily_review".equals(context.getCreateParam().getTaskType())) {
            throw new IllegalArgumentException("P0 仅支持 daily_review 任务");
        }
        AgentPlan plan = new AgentPlan();
        plan.setTaskType("daily_review");
        plan.getSteps().add(new AgentPlanStep(1, "查询订单核心指标", "order.query_summary"));
        plan.getSteps().add(new AgentPlanStep(2, "查询差评风险", "comment.query_negative"));
        plan.getSteps().add(new AgentPlanStep(3, "查询待优化商品", "product.query_candidates"));
        plan.getSteps().add(new AgentPlanStep(4, "生成经营复盘报告", "report.generate_daily_review"));
        return plan;
    }
}
