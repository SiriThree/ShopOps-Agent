package com.sirithree.shopops.admin.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.agent.service.StepExecutionRecorder;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SequentialAgentExecutorServiceTest {
    private final ToolGatewayService toolGatewayService = mock(ToolGatewayService.class);
    private final OperationReportService operationReportService = mock(OperationReportService.class);
    private final StepExecutionRecorder stepExecutionRecorder = mock(StepExecutionRecorder.class);
    private final SequentialAgentExecutorService executor = new SequentialAgentExecutorService(
            toolGatewayService,
            operationReportService,
            stepExecutionRecorder
    );

    @Test
    @SuppressWarnings("unchecked")
    void shouldCarryPreviousSuccessfulEvidenceIntoRepairReport() {
        AgentTaskContext context = context();
        AgentExecutionResult base = baseResult();
        AgentPlan repairPlan = plan(
                new AgentPlanStep(3, "Repair product evidence", "product.query_candidates"),
                new AgentPlanStep(4, "Regenerate report", "report.generate_daily_review")
        );
        OperationReportDto report = new OperationReportDto();
        report.setReportId(90200L);

        when(stepExecutionRecorder.ensureStep(eq(context), any(AgentPlanStep.class))).thenReturn(3L, 4L);
        when(toolGatewayService.invoke(any(), eq("product.query_candidates"), any()))
                .thenReturn(ToolInvokeResult.success(Map.of("candidateCount", 3), null));
        when(toolGatewayService.invoke(any(), eq("report.generate_daily_review"), any()))
                .thenReturn(ToolInvokeResult.success(Map.of("status", "regenerated"), null));
        when(operationReportService.createDailyReviewReport(eq(1L), eq(1L), eq(10001L), eq(1L), eq("tr_test"), any()))
                .thenReturn(report);

        AgentExecutionResult result = executor.execute(context, repairPlan, base);

        assertThat(result.getReportId()).isEqualTo(90200L);
        assertThat(result.getStepResults()).containsKeys("order.query_summary", "product.query_candidates", "report.generate_daily_review");

        ArgumentCaptor<Object> reportInputCaptor = ArgumentCaptor.forClass(Object.class);
        org.mockito.Mockito.verify(toolGatewayService)
                .invoke(any(), eq("report.generate_daily_review"), reportInputCaptor.capture());
        Map<String, Object> reportInput = (Map<String, Object>) reportInputCaptor.getValue();
        assertThat(reportInput.get("orderSummary")).isEqualTo(Map.of("orderCount", 10));
        assertThat(reportInput.get("productCandidates")).isEqualTo(Map.of("candidateCount", 3));

        ArgumentCaptor<Map<String, Object>> reportDataCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(operationReportService)
                .createDailyReviewReport(eq(1L), eq(1L), eq(10001L), eq(1L), eq("tr_test"), reportDataCaptor.capture());
        assertThat(reportDataCaptor.getValue()).containsEntry("status", "regenerated");
    }

    private AgentTaskContext context() {
        AgentTaskCreateParam param = new AgentTaskCreateParam();
        param.setTaskType("daily_review");
        DateRangeParam dateRange = new DateRangeParam();
        dateRange.setStart("2026-07-22");
        dateRange.setEnd("2026-07-22");
        param.setDateRange(dateRange);

        AgentTaskContext context = new AgentTaskContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(1L);
        context.setTaskId(10001L);
        context.setTraceId("tr_test");
        context.setCreateParam(param);
        return context;
    }

    private AgentExecutionResult baseResult() {
        Map<String, ToolInvokeResult> steps = new LinkedHashMap<>();
        steps.put("order.query_summary", ToolInvokeResult.success(Map.of("orderCount", 10), null));
        steps.put("report.generate_daily_review", ToolInvokeResult.success(Map.of("status", "generated"), null));

        AgentExecutionResult result = new AgentExecutionResult();
        result.setSuccess(true);
        result.setReportId(90100L);
        result.setStepResults(steps);
        return result;
    }

    private AgentPlan plan(AgentPlanStep... steps) {
        AgentPlan plan = new AgentPlan();
        plan.setTaskType("daily_review");
        plan.setSteps(List.of(steps));
        return plan;
    }
}
