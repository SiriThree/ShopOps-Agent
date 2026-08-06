package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentVerificationResult;
import com.sirithree.shopops.admin.agent.service.AgentEngineService;
import com.sirithree.shopops.admin.agent.service.AgentExecutorService;
import com.sirithree.shopops.admin.agent.service.PlanValidator;
import com.sirithree.shopops.admin.agent.service.PlannerService;
import com.sirithree.shopops.admin.agent.service.VerifierService;
import com.sirithree.shopops.admin.agent.governance.WorkflowTemplateRegistry;
import com.sirithree.shopops.admin.audit.domain.TraceSpanCreateCommand;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultAgentEngineService implements AgentEngineService {
    private final PlannerService plannerService;
    private final PlanValidator planValidator;
    private final AgentExecutorService executorService;
    private final VerifierService verifierService;
    private final TraceService traceService;
    private final JacksonJsonSupport jsonSupport;
    private final WorkflowTemplateRegistry templateRegistry;

    public DefaultAgentEngineService(PlannerService plannerService,
                                     PlanValidator planValidator,
                                     AgentExecutorService executorService,
                                     VerifierService verifierService,
                                     TraceService traceService,
                                     JacksonJsonSupport jsonSupport,
                                     WorkflowTemplateRegistry templateRegistry) {
        this.plannerService = plannerService;
        this.planValidator = planValidator;
        this.executorService = executorService;
        this.verifierService = verifierService;
        this.traceService = traceService;
        this.jsonSupport = jsonSupport;
        this.templateRegistry = templateRegistry;
    }

    @Override
    public AgentExecutionResult executeTask(AgentTaskContext context) {
        context.setStartedAtMillis(System.currentTimeMillis());
        context.setRepairAttempts(0);
        String rootSpan = startSpan(context, null, "agent", "agent.task", "task", context.getTaskId(), context.getCreateParam().getUserInput());
        String activeChildSpan = null;
        try {
            String plannerSpan = startSpan(context, rootSpan, "planner", "agent.planner", "task", context.getTaskId(), context.getCreateParam().getTaskType());
            activeChildSpan = plannerSpan;
            AgentPlan plan = plannerService.createPlan(context);
            planValidator.validate(context, plan);
            traceService.finishSpan(context.getTraceId(), plannerSpan, "SUCCESS", jsonSupport.toJson(plan), null);
            activeChildSpan = null;

            String executorSpan = startSpan(context, rootSpan, "executor", "agent.executor", "task", context.getTaskId(), "execute plan");
            activeChildSpan = executorSpan;
            context.setExecutorSpanId(executorSpan);
            AgentExecutionResult result = executorService.execute(context, plan);
            context.setExecutorSpanId(null);
            if (!Boolean.TRUE.equals(result.getSuccess())) {
                String errorMessage = result.getErrorMessage() == null ? "Task execution failed" : result.getErrorMessage();
                traceService.finishSpan(context.getTraceId(), executorSpan, "FAILED", null, errorMessage);
                activeChildSpan = null;
                throw new IllegalStateException(errorMessage);
            }
            traceService.finishSpan(context.getTraceId(), executorSpan, "SUCCESS", "reportId=" + result.getReportId(), null);
            activeChildSpan = null;

            String verifierSpan = startSpan(context, rootSpan, "verifier", "agent.verifier", "task", context.getTaskId(), "verify result");
            activeChildSpan = verifierSpan;
            AgentVerificationResult verification = verifierService.verify(context, result);
            result.setVerification(verification);
            if (!verification.isPassed()) {
                if (verification.isRepairable() && context.getRepairAttempts() < maxRepairAttempts(context)) {
                    traceService.finishSpan(context.getTraceId(), verifierSpan, "FAILED", jsonSupport.toJson(verification), "verification failed, repair required");
                    activeChildSpan = null;
                    result = repairAndVerify(context, rootSpan, plan, result);
                } else {
                    traceService.finishSpan(context.getTraceId(), verifierSpan, "FAILED", jsonSupport.toJson(verification), "verification failed");
                    activeChildSpan = null;
                    throw new IllegalStateException("Agent result verification failed");
                }
            } else {
                traceService.finishSpan(context.getTraceId(), verifierSpan, "SUCCESS", jsonSupport.toJson(verification), null);
                activeChildSpan = null;
            }

            traceService.finishSpan(context.getTraceId(), rootSpan, "SUCCESS", "task finished", null);
            return result;
        } catch (RuntimeException ex) {
            if (activeChildSpan != null) {
                traceService.finishSpan(context.getTraceId(), activeChildSpan, "FAILED", null, ex.getMessage());
            }
            traceService.finishSpan(context.getTraceId(), rootSpan, "FAILED", null, ex.getMessage());
            throw ex;
        } finally {
            context.setExecutorSpanId(null);
        }
    }

    private AgentExecutionResult repairAndVerify(AgentTaskContext context,
                                                 String rootSpan,
                                                 AgentPlan originalPlan,
                                                 AgentExecutionResult baseResult) {
        context.setRepairAttempts(context.getRepairAttempts() + 1);
        AgentPlan repairPlan = repairPlan(originalPlan, baseResult.getVerification());
        planValidator.validate(context, repairPlan);
        String repairSpan = startSpan(context, rootSpan, "executor", "agent.repair", "task", context.getTaskId(), "repair missing evidence");
        context.setExecutorSpanId(repairSpan);
        AgentExecutionResult repairedResult = executorService.execute(context, repairPlan, baseResult);
        context.setExecutorSpanId(null);
        if (!Boolean.TRUE.equals(repairedResult.getSuccess())) {
            String errorMessage = repairedResult.getErrorMessage() == null ? "Repair execution failed" : repairedResult.getErrorMessage();
            traceService.finishSpan(context.getTraceId(), repairSpan, "FAILED", jsonSupport.toJson(repairPlan), errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        traceService.finishSpan(context.getTraceId(), repairSpan, "SUCCESS", jsonSupport.toJson(repairPlan), null);

        String reverifySpan = startSpan(context, rootSpan, "verifier", "agent.verifier.retry", "task", context.getTaskId(), "verify repaired result");
        AgentVerificationResult verification = verifierService.verify(context, repairedResult);
        repairedResult.setVerification(verification);
        if (!verification.isPassed()) {
            traceService.finishSpan(context.getTraceId(), reverifySpan, "FAILED", jsonSupport.toJson(verification), "verification failed after repair");
            throw new IllegalStateException("Agent result verification failed");
        }
        traceService.finishSpan(context.getTraceId(), reverifySpan, "SUCCESS", jsonSupport.toJson(verification), null);
        return repairedResult;
    }

    private int maxRepairAttempts(AgentTaskContext context) {
        String workflowType = context.getCreateParam().getTaskSpec() != null && context.getCreateParam().getTaskSpec().getIntent() != null
                ? context.getCreateParam().getTaskSpec().getIntent()
                : (context.getCreateParam().getIntent() == null ? "daily_review" : context.getCreateParam().getIntent());
        return templateRegistry.require(workflowType).maxRepairAttempts();
    }

    private AgentPlan repairPlan(AgentPlan originalPlan, AgentVerificationResult verification) {
        int stepNo = originalPlan.getSteps().stream()
                .map(AgentPlanStep::getStepNo)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        List<AgentPlanStep> steps = new ArrayList<>();
        for (String toolCode : verification.getRepairToolCodes()) {
            steps.add(new AgentPlanStep(++stepNo, repairStepName(toolCode), toolCode, "Verifier detected missing evidence and requested a supplemental tool call"));
        }
        steps.add(new AgentPlanStep(++stepNo, "重新生成报告", "report.generate_daily_review", "Use repaired evidence to refresh the final report"));

        AgentPlan repairPlan = new AgentPlan();
        repairPlan.setTaskType(originalPlan.getTaskType());
        repairPlan.setRationale("Verifier requested a bounded repair run for missing evidence: " + verification.getMissingEvidence());
        repairPlan.setSteps(steps);
        return repairPlan;
    }

    private String repairStepName(String toolCode) {
        return switch (toolCode) {
            case "order.query_summary" -> "补充订单证据";
            case "comment.query_negative" -> "补充评价证据";
            case "product.query_candidates" -> "补充商品证据";
            case "ad.query_performance" -> "补充投放证据";
            case "report.query_external_metrics" -> "补充外部指标";
            default -> "补充证据";
        };
    }

    private String startSpan(AgentTaskContext context, String parentSpanId, String spanType, String spanName, String refType, Long refId, String inputSummary) {
        TraceSpanCreateCommand command = new TraceSpanCreateCommand();
        command.setTenantId(context.getTenantId());
        command.setShopId(context.getShopId());
        command.setTraceId(context.getTraceId());
        command.setParentSpanId(parentSpanId);
        command.setSpanType(spanType);
        command.setSpanName(spanName);
        command.setRefType(refType);
        command.setRefId(refId);
        command.setInputSummary(inputSummary);
        return traceService.startSpan(command);
    }
}
