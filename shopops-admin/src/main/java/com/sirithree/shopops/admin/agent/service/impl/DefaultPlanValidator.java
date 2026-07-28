package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.PlanValidator;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DefaultPlanValidator implements PlanValidator {
    private final McpToolService mcpToolService;

    public DefaultPlanValidator(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    @Override
    public void validate(AgentTaskContext context, AgentPlan plan) {
        if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
            throw new IllegalArgumentException("执行计划不能为空");
        }
        Set<String> toolCodes = new HashSet<>();
        for (int index = 0; index < plan.getSteps().size(); index++) {
            var step = plan.getSteps().get(index);
            int expectedStepNo = index + 1;
            if (!Integer.valueOf(expectedStepNo).equals(step.getStepNo())) {
                throw new IllegalArgumentException("计划步骤编号必须从 1 连续递增");
            }
            if (step.getToolCode() == null || step.getToolCode().isBlank()) {
                throw new IllegalArgumentException("计划步骤缺少工具编码: " + expectedStepNo);
            }
            if (!toolCodes.add(step.getToolCode())) {
                throw new IllegalArgumentException("计划包含重复工具: " + step.getToolCode());
            }
            if (mcpToolService.getTool(context.getTenantId(), step.getToolCode()) == null) {
                throw new IllegalArgumentException("计划包含未注册工具: " + step.getToolCode());
            }
            boolean reportTool = "report.generate_daily_review".equals(step.getToolCode());
            boolean finalStep = index == plan.getSteps().size() - 1;
            if (reportTool != finalStep) {
                throw new IllegalArgumentException("报告生成工具必须且只能作为最后一步");
            }
        }
    }
}
