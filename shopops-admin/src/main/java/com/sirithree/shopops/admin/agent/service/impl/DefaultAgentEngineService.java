package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.audit.domain.TraceSpanCreateCommand;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.AgentEngineService;
import com.sirithree.shopops.admin.agent.service.AgentExecutorService;
import com.sirithree.shopops.admin.agent.service.PlanValidator;
import com.sirithree.shopops.admin.agent.service.PlannerService;
import com.sirithree.shopops.admin.agent.service.VerifierService;
import org.springframework.stereotype.Service;

@Service
public class DefaultAgentEngineService implements AgentEngineService {
    private final PlannerService plannerService;
    private final PlanValidator planValidator;
    private final AgentExecutorService executorService;
    private final VerifierService verifierService;
    private final TraceService traceService;

    public DefaultAgentEngineService(PlannerService plannerService,
                                     PlanValidator planValidator,
                                     AgentExecutorService executorService,
                                     VerifierService verifierService,
                                     TraceService traceService) {
        this.plannerService = plannerService;
        this.planValidator = planValidator;
        this.executorService = executorService;
        this.verifierService = verifierService;
        this.traceService = traceService;
    }

    @Override
    public AgentExecutionResult executeTask(AgentTaskContext context) {
        String rootSpan = startSpan(context, null, "agent", "agent.task", "task", context.getTaskId(), context.getCreateParam().getUserInput());
        String activeChildSpan = null;
        try {
            String plannerSpan = startSpan(context, rootSpan, "planner", "agent.planner", "task", context.getTaskId(), context.getCreateParam().getTaskType());
            activeChildSpan = plannerSpan;
            AgentPlan plan = plannerService.createPlan(context);
            planValidator.validate(context, plan);
            traceService.finishSpan(context.getTraceId(), plannerSpan, "SUCCESS", "steps=" + plan.getSteps().size(), null);
            activeChildSpan = null;

            String executorSpan = startSpan(context, rootSpan, "executor", "agent.executor", "task", context.getTaskId(), "execute plan");
            activeChildSpan = executorSpan;
            context.setExecutorSpanId(executorSpan);
            AgentExecutionResult result = executorService.execute(context, plan);
            context.setExecutorSpanId(null);
            traceService.finishSpan(context.getTraceId(), executorSpan, "SUCCESS", "reportId=" + result.getReportId(), null);
            activeChildSpan = null;

            String verifierSpan = startSpan(context, rootSpan, "verifier", "agent.verifier", "task", context.getTaskId(), "verify result");
            activeChildSpan = verifierSpan;
            verifierService.verify(context, result);
            traceService.finishSpan(context.getTraceId(), verifierSpan, "SUCCESS", "verification passed", null);
            activeChildSpan = null;

            traceService.finishSpan(context.getTraceId(), rootSpan, "SUCCESS", "task finished", null);
            return result;
        } catch (RuntimeException ex) {
            if (activeChildSpan != null) {
                traceService.finishSpan(context.getTraceId(), activeChildSpan, "FAILED", null, ex.getMessage());
            }
            traceService.finishSpan(context.getTraceId(), rootSpan, "FAILED", null, ex.getMessage());
            throw ex;
        }
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
