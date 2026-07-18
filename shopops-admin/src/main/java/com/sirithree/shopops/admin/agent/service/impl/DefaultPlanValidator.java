package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.PlanValidator;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import org.springframework.stereotype.Service;

@Service
public class DefaultPlanValidator implements PlanValidator {
    private final McpToolService mcpToolService;

    public DefaultPlanValidator(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    @Override
    public void validate(AgentTaskContext context, AgentPlan plan) {
        if (plan.getSteps().isEmpty()) {
            throw new IllegalArgumentException("执行计划不能为空");
        }
        plan.getSteps().forEach(step -> {
            if (mcpToolService.getTool(context.getTenantId(), step.getToolCode()) == null) {
                throw new IllegalArgumentException("计划包含未注册工具: " + step.getToolCode());
            }
        });
    }
}
