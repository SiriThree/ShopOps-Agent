package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.AgentExecutorService;
import com.sirithree.shopops.admin.agent.service.StepExecutionRecorder;
import com.sirithree.shopops.admin.auth.domain.PermissionCode;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SequentialAgentExecutorService implements AgentExecutorService {
    private final ToolGatewayService toolGatewayService;
    private final OperationReportService operationReportService;
    private final StepExecutionRecorder stepExecutionRecorder;

    public SequentialAgentExecutorService(ToolGatewayService toolGatewayService,
                                          OperationReportService operationReportService,
                                          StepExecutionRecorder stepExecutionRecorder) {
        this.toolGatewayService = toolGatewayService;
        this.operationReportService = operationReportService;
        this.stepExecutionRecorder = stepExecutionRecorder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentExecutionResult execute(AgentTaskContext context, AgentPlan plan) {
        return execute(context, plan, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentExecutionResult execute(AgentTaskContext context, AgentPlan plan, AgentExecutionResult baseResult) {
        AgentExecutionResult executionResult = new AgentExecutionResult();
        executionResult.setSuccess(false);
        executionResult.setDegraded(baseResult != null && Boolean.TRUE.equals(baseResult.getDegraded()));

        Map<String, ToolInvokeResult> results = new LinkedHashMap<>();
        Map<String, Object> dataByTool = new HashMap<>();
        if (baseResult != null && baseResult.getStepResults() != null) {
            results.putAll(baseResult.getStepResults());
            for (Map.Entry<String, ToolInvokeResult> entry : baseResult.getStepResults().entrySet()) {
                ToolInvokeResult previous = entry.getValue();
                if (previous != null && Boolean.TRUE.equals(previous.getSuccess()) && previous.getData() != null) {
                    dataByTool.put(entry.getKey(), previous.getData());
                }
            }
            executionResult.setReportId(baseResult.getReportId());
        }

        for (AgentPlanStep step : plan.getSteps()) {
            Object input = buildInput(context, step, dataByTool);
            Long stepId = stepExecutionRecorder.ensureStep(context, step);
            stepExecutionRecorder.running(context, stepId, input);
            ToolInvokeResult result = toolGatewayService.invoke(toToolContext(context, stepId), step.getToolCode(), input);
            results.put(step.getToolCode(), result);
            executionResult.setStepResults(results);

            if (!Boolean.TRUE.equals(result.getSuccess())) {
                stepExecutionRecorder.failed(context, stepId, result.getErrorCode(), result.getErrorMessage());
                if ("order.query_summary".equals(step.getToolCode()) || "report.generate_daily_review".equals(step.getToolCode())) {
                    executionResult.setErrorMessage(result.getErrorMessage());
                    return executionResult;
                }
                executionResult.setDegraded(true);
                continue;
            }

            dataByTool.put(step.getToolCode(), result.getData());
            stepExecutionRecorder.success(context, stepId, result.getData());
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
        toolContext.setParentSpanId(context.getExecutorSpanId());
        toolContext.setPermissions(Set.of(
                PermissionCode.ORDER_READ,
                PermissionCode.PRODUCT_READ,
                PermissionCode.REVIEW_READ,
                PermissionCode.REPORT_GENERATE,
                PermissionCode.TOOL_EXECUTE,
                "comment:read",
                "ad:read",
                "report:read",
                "report:export",
                "feishu:write"
        ));
        toolContext.setManualInvoke(false);
        return toolContext;
    }

    private Object buildInput(AgentTaskContext context, AgentPlanStep step, Map<String, Object> dataByTool) {
        if ("report.generate_daily_review".equals(step.getToolCode())) {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("orderSummary", dataByTool.getOrDefault("order.query_summary", Map.of()));
            input.put("negativeComments", dataByTool.getOrDefault("comment.query_negative", Map.of("negativeCount", 0, "riskComments", java.util.List.of(), "categoryStats", Map.of())));
            input.put("productCandidates", dataByTool.getOrDefault("product.query_candidates", Map.of("candidateCount", 0, "products", java.util.List.of())));
            input.put("adPerformance", dataByTool.getOrDefault("ad.query_performance", Map.of("campaigns", java.util.List.of())));
            input.put("externalReportMetrics", dataByTool.getOrDefault("report.query_external_metrics", Map.of("topChannels", java.util.List.of())));
            if (context.getCreateParam().getIntent() != null && !context.getCreateParam().getIntent().isBlank()) {
                input.put("intent", context.getCreateParam().getIntent());
            }
            input.put("executedToolCodes", new ArrayList<>(dataByTool.keySet()));
            input.put("dateRange", context.getCreateParam().getDateRange());
            return input;
        }
        return Map.of(
                "shopId", context.getShopId(),
                "startDate", context.getCreateParam().getDateRange().getStart(),
                "endDate", context.getCreateParam().getDateRange().getEnd()
        );
    }
}
