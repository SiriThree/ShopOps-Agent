package com.sirithree.shopops.admin.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentVerificationResult;
import com.sirithree.shopops.admin.agent.service.AgentExecutorService;
import com.sirithree.shopops.admin.agent.service.PlanValidator;
import com.sirithree.shopops.admin.agent.service.PlannerService;
import com.sirithree.shopops.admin.agent.service.VerifierService;
import com.sirithree.shopops.admin.agent.governance.WorkflowTemplateRegistry;
import com.sirithree.shopops.admin.audit.domain.TraceSpanCreateCommand;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DefaultAgentEngineServiceTest {
    private final PlannerService plannerService = mock(PlannerService.class);
    private final PlanValidator planValidator = mock(PlanValidator.class);
    private final AgentExecutorService executorService = mock(AgentExecutorService.class);
    private final VerifierService verifierService = mock(VerifierService.class);
    private final TraceService traceService = mock(TraceService.class);
    private final DefaultAgentEngineService engine = new DefaultAgentEngineService(
            plannerService,
            planValidator,
            executorService,
            verifierService,
            traceService,
            new JacksonJsonSupport(new ObjectMapper()),
            new WorkflowTemplateRegistry()
    );

    @Test
    void shouldRepairMissingEvidenceOnceAndVerifyAgain() {
        AgentTaskContext context = context();
        AgentPlan initialPlan = plan(
                new AgentPlanStep(1, "Query orders", "order.query_summary"),
                new AgentPlanStep(2, "Generate report", "report.generate_daily_review")
        );
        AgentExecutionResult initialResult = result(101L, Map.of(
                "order.query_summary", ToolInvokeResult.success(Map.of("orderCount", 10), null),
                "report.generate_daily_review", ToolInvokeResult.success(Map.of("status", "generated"), null)
        ));
        AgentExecutionResult repairedResult = result(102L, Map.of(
                "order.query_summary", ToolInvokeResult.success(Map.of("orderCount", 10), null),
                "product.query_candidates", ToolInvokeResult.success(Map.of("candidateCount", 3), null),
                "report.generate_daily_review", ToolInvokeResult.success(Map.of("status", "regenerated"), null)
        ));

        when(plannerService.createPlan(context)).thenReturn(initialPlan);
        doNothing().when(planValidator).validate(context, initialPlan);
        when(executorService.execute(context, initialPlan)).thenReturn(initialResult);
        when(executorService.execute(eq(context), any(AgentPlan.class), eq(initialResult))).thenReturn(repairedResult);
        when(verifierService.verify(context, initialResult)).thenReturn(repairableVerification());
        when(verifierService.verify(context, repairedResult)).thenReturn(passedVerification());
        when(traceService.startSpan(any(TraceSpanCreateCommand.class))).thenReturn("span-1", "span-2", "span-3", "span-4", "span-5");

        AgentExecutionResult finalResult = engine.executeTask(context);

        assertThat(finalResult.getReportId()).isEqualTo(102L);
        assertThat(finalResult.getVerification().isPassed()).isTrue();

        ArgumentCaptor<AgentPlan> repairPlanCaptor = ArgumentCaptor.forClass(AgentPlan.class);
        verify(executorService).execute(eq(context), repairPlanCaptor.capture(), eq(initialResult));
        assertThat(repairPlanCaptor.getValue().getSteps())
                .extracting(AgentPlanStep::getToolCode)
                .containsExactly("product.query_candidates", "report.generate_daily_review");
    }

    private AgentTaskContext context() {
        AgentTaskCreateParam param = new AgentTaskCreateParam();
        param.setTaskType("daily_review");
        param.setUserInput("Find low click products and generate a report");

        AgentTaskContext context = new AgentTaskContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(1L);
        context.setTaskId(10001L);
        context.setTraceId("tr_test");
        context.setCreateParam(param);
        return context;
    }

    private AgentPlan plan(AgentPlanStep... steps) {
        AgentPlan plan = new AgentPlan();
        plan.setTaskType("daily_review");
        plan.setSteps(List.of(steps));
        return plan;
    }

    private AgentExecutionResult result(Long reportId, Map<String, ToolInvokeResult> stepResults) {
        AgentExecutionResult result = new AgentExecutionResult();
        result.setSuccess(true);
        result.setDegraded(false);
        result.setReportId(reportId);
        result.setStepResults(new LinkedHashMap<>(stepResults));
        return result;
    }

    private AgentVerificationResult repairableVerification() {
        AgentVerificationResult verification = new AgentVerificationResult();
        verification.setPassed(false);
        verification.setScore(0.75);
        verification.setRepairable(true);
        verification.setMissingEvidence(List.of("product_candidates"));
        verification.setRepairToolCodes(List.of("product.query_candidates"));
        return verification;
    }

    private AgentVerificationResult passedVerification() {
        AgentVerificationResult verification = new AgentVerificationResult();
        verification.setPassed(true);
        verification.setScore(1.0);
        return verification;
    }
}
