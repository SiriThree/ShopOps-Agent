package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionMode;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.governance.WorkflowTemplate;
import com.sirithree.shopops.admin.agent.governance.WorkflowTemplateRegistry;
import com.sirithree.shopops.admin.agent.service.PlanValidator;
import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DefaultPlanValidator implements PlanValidator {
    private final McpToolService mcpToolService;
    private final AuthorizationService authorizationService;
    private final WorkflowTemplateRegistry templateRegistry;

    public DefaultPlanValidator(McpToolService mcpToolService,
                                AuthorizationService authorizationService,
                                WorkflowTemplateRegistry templateRegistry) {
        this.mcpToolService = mcpToolService;
        this.authorizationService = authorizationService;
        this.templateRegistry = templateRegistry;
    }

    @Override
    public void validate(AgentTaskContext context, AgentPlan plan) {
        if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
            throw new IllegalArgumentException("执行计划不能为空");
        }
        String workflowType = resolveWorkflowType(context);
        WorkflowTemplate template = templateRegistry.require(workflowType);
        AgentExecutionMode mode = AgentExecutionMode.from(context.getCreateParam().getExecutionMode());
        if (!template.allowedModes().contains(mode)) throw new IllegalArgumentException("工作流不允许执行模式: " + mode);
        if (plan.getSteps().size() > template.maxSteps()) throw new IllegalArgumentException("计划超过工作流最大步骤数: " + template.maxSteps());
        if (!"daily_review".equals(plan.getTaskType())) throw new IllegalArgumentException("计划任务类型与平台任务类型不兼容");

        Set<String> toolCodes = new HashSet<>();
        for (int index = 0; index < plan.getSteps().size(); index++) {
            var step = plan.getSteps().get(index);
            int expectedStepNo = index + 1;
            if (!Integer.valueOf(expectedStepNo).equals(step.getStepNo())) throw new IllegalArgumentException("计划步骤编号必须从 1 连续递增");
            if (step.getToolCode() == null || step.getToolCode().isBlank()) throw new IllegalArgumentException("计划步骤缺少工具编码: " + expectedStepNo);
            if (!toolCodes.add(step.getToolCode())) throw new IllegalArgumentException("计划包含重复工具: " + step.getToolCode());
            if (!template.allowedTools().contains(step.getToolCode())) throw new IllegalArgumentException("工具不在工作流模板允许范围: " + step.getToolCode());
            McpToolDto tool = mcpToolService.getTool(context.getTenantId(), step.getToolCode());
            if (tool == null) throw new IllegalArgumentException("计划包含未注册工具: " + step.getToolCode());
            if (!Boolean.TRUE.equals(tool.getEnabled())) throw new IllegalArgumentException("计划包含已停用工具: " + step.getToolCode());
            if (tool.getVersion() == null || tool.getVersion().isBlank()) throw new IllegalArgumentException("工具缺少版本元数据: " + step.getToolCode());
            if (tool.getPermissionCode() == null || !authorizationService.isAuthorized(context.getTenantId(), context.getShopId(), context.getUserId(), tool.getPermissionCode())) {
                throw new SecurityException("当前主体无权规划工具: " + step.getToolCode());
            }
            if (riskRank(tool.getRiskLevel()) > riskRank(template.maxRiskLevel())) throw new IllegalArgumentException("工具风险超过工作流模板上限: " + step.getToolCode());
            boolean highRisk = riskRank(tool.getRiskLevel()) >= riskRank("HIGH");
            if (mode == AgentExecutionMode.AUTOMATIC && (highRisk || Boolean.TRUE.equals(tool.getNeedApproval()))) {
                throw new IllegalArgumentException("AUTOMATIC 模式禁止规划需要审批的高风险工具: " + step.getToolCode());
            }
            boolean reportTool = "report.generate_daily_review".equals(step.getToolCode());
            boolean finalStep = index == plan.getSteps().size() - 1;
            if (reportTool != finalStep) throw new IllegalArgumentException("报告生成工具必须且只能作为最后一步");
        }
    }

    private String resolveWorkflowType(AgentTaskContext context) {
        if (context.getCreateParam().getTaskSpec() != null && context.getCreateParam().getTaskSpec().getIntent() != null) return context.getCreateParam().getTaskSpec().getIntent();
        return context.getCreateParam().getIntent() == null ? "daily_review" : context.getCreateParam().getIntent();
    }

    private int riskRank(String risk) {
        if (risk == null) return 0;
        return switch (risk.toUpperCase()) { case "CRITICAL" -> 4; case "HIGH" -> 3; case "MEDIUM" -> 2; case "LOW" -> 1; default -> 0; };
    }
}
