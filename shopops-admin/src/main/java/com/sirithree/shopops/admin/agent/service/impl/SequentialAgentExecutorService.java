package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.AgentExecutorService;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SequentialAgentExecutorService implements AgentExecutorService {
    private final ToolGatewayService toolGatewayService;
    private final OperationReportService operationReportService;

    public SequentialAgentExecutorService(ToolGatewayService toolGatewayService,
                                          OperationReportService operationReportService) {
        this.toolGatewayService = toolGatewayService;
        this.operationReportService = operationReportService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentExecutionResult execute(AgentTaskContext context, AgentPlan plan) {
        AgentExecutionResult executionResult = new AgentExecutionResult();
        executionResult.setSuccess(false);
        executionResult.setDegraded(false);

        Map<String, ToolInvokeResult> results = new LinkedHashMap<>();
        Map<String, Object> dataByTool = new HashMap<>();

        for (AgentPlanStep step : plan.getSteps()) {
            Object input = buildInput(context, step, dataByTool);
            ToolInvokeResult result = toolGatewayService.invoke(toToolContext(context, step.getStepNo().longValue()), step.getToolCode(), input);
            results.put(step.getToolCode(), result);
            executionResult.setStepResults(results);

            if (!Boolean.TRUE.equals(result.getSuccess())) {
                if ("order.query_summary".equals(step.getToolCode()) || "report.generate_daily_review".equals(step.getToolCode())) {
                    executionResult.setErrorMessage(result.getErrorMessage());
                    return executionResult;
                }
                executionResult.setDegraded(true);
                continue;
            }

            dataByTool.put(step.getToolCode(), result.getData());
            if ("report.generate_daily_review".equals(step.getToolCode())) {
                OperationReportDto report = operationReportService.createDailyReviewReport(
                        context.getTenantId(),
                        context.getShopId(),
                        context.getTaskId(),
                        context.getUserId(),
                        context.getTraceId(),
                        (Map<String, Object>) result.getData()
                );
                executionResult.setReportId(report.getReportId());
            }
        }

        executionResult.setSuccess(true);
        return executionResult;
    }

    private ToolInvokeContext toToolContext(AgentTaskContext context, Long stepId) {
        ToolInvokeContext toolContext = new ToolInvokeContext();
        toolContext.setTenantId(context.getTenantId());
        toolContext.setShopId(context.getShopId());
        toolContext.setUserId(context.getUserId());
        toolContext.setTaskId(context.getTaskId());
        toolContext.setStepId(stepId);
        toolContext.setTraceId(context.getTraceId());
        toolContext.setManualInvoke(false);
        return toolContext;
    }

    private Object buildInput(AgentTaskContext context, AgentPlanStep step, Map<String, Object> dataByTool) {
        if ("report.generate_daily_review".equals(step.getToolCode())) {
            return Map.of(
                    "orderSummary", dataByTool.getOrDefault("order.query_summary", Map.of()),
                    "negativeComments", dataByTool.getOrDefault("comment.query_negative", Map.of("negativeCount", 0, "riskComments", java.util.List.of(), "categoryStats", Map.of())),
                    "productCandidates", dataByTool.getOrDefault("product.query_candidates", Map.of("candidateCount", 0, "products", java.util.List.of())),
                    "dateRange", context.getCreateParam().getDateRange()
            );
        }
        return Map.of(
                "shopId", context.getShopId(),
                "startDate", context.getCreateParam().getDateRange().getStart(),
                "endDate", context.getCreateParam().getDateRange().getEnd()
        );
    }
}
