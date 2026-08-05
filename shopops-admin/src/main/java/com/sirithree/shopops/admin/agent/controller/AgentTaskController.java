package com.sirithree.shopops.admin.agent.controller;

import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskInterpretation;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskRecoveryResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.agent.domain.NaturalLanguageTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.NaturalLanguageTaskRequest;
import com.sirithree.shopops.admin.agent.service.AgentTaskInterpreter;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.agent.service.PlannerService;
import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tasks")
public class AgentTaskController {
    private final AgentTaskService agentTaskService;
    private final AgentTaskInterpreter agentTaskInterpreter;
    private final PlannerService plannerService;

    public AgentTaskController(AgentTaskService agentTaskService,
                               AgentTaskInterpreter agentTaskInterpreter,
                               PlannerService plannerService) {
        this.agentTaskService = agentTaskService;
        this.agentTaskInterpreter = agentTaskInterpreter;
        this.plannerService = plannerService;
    }

    @PostMapping
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<AgentTaskCreateResult> createTask(@Valid @RequestBody AgentTaskCreateParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.createTask(context.getTenantId(), context.getShopId(), context.getUserId(), param));
    }

    @PostMapping("/natural-language")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<NaturalLanguageTaskCreateResult> createTaskFromNaturalLanguage(@Valid @RequestBody NaturalLanguageTaskRequest request) {
        RequestContext context = RequestContextHolder.current();
        AgentTaskCreateParam param = new AgentTaskCreateParam();
        DateRangeParam dateRange = defaultDateRange(request.getDateRange());
        AgentTaskInterpretation interpretation = agentTaskInterpreter.interpret(request.getUserInput(), dateRange);
        String intent = interpretation.getTaskSpec().getIntent();
        param.setTaskType("daily_review");
        param.setUserInput(request.getUserInput().trim());
        param.setIntent(intent);
        param.setTaskSpec(interpretation.getTaskSpec());
        param.setDateRange(dateRange);

        AgentTaskCreateResult task = agentTaskService.createTask(context.getTenantId(), context.getShopId(), context.getUserId(), param);

        NaturalLanguageTaskCreateResult result = new NaturalLanguageTaskCreateResult();
        result.setIntent(intent);
        result.setIntentLabel(interpretation.getIntentLabel());
        result.setConfidence(interpretation.getConfidence());
        result.setTaskType(param.getTaskType());
        result.setRoutedReason(interpretation.getRoutedReason());
        result.setFocusAreas(interpretation.getTaskSpec().getFocusAreas());
        result.setDataSources(interpretation.getDataSources());
        result.setRecommendedActions(interpretation.getRecommendedActions());
        result.setDateRange(param.getDateRange());
        result.setTaskSpec(interpretation.getTaskSpec());
        result.setPlan(plannerService.previewPlan(interpretation.getTaskSpec()));
        result.setTask(task);
        return CommonResult.success(result);
    }

    @PostMapping("/{taskId}/cancel")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<AgentTaskDto> cancelTask(@PathVariable Long taskId,
                                                  @RequestParam(required = false) String reason) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.cancelTask(context.getTenantId(), context.getShopId(),
                context.getUserId(), taskId, reason));
    }

    @PostMapping("/{taskId}/retry")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<AgentTaskCreateResult> retryTask(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.retryTask(context.getTenantId(), context.getShopId(), context.getUserId(), taskId));
    }

    @PostMapping("/stale/requeue")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<AgentTaskRecoveryResult> requeueStaleTasks(@RequestParam(defaultValue = "10") Integer queuedTimeoutMinutes,
                                                                   @RequestParam(defaultValue = "30") Integer runningTimeoutMinutes,
                                                                   @RequestParam(defaultValue = "20") Integer limit) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.requeueStaleTasks(
                context.getTenantId(),
                context.getShopId(),
                context.getUserId(),
                queuedTimeoutMinutes,
                runningTimeoutMinutes,
                limit
        ));
    }

    @GetMapping
    public CommonResult<CommonPage<AgentTaskDto>> listTasks(AgentTaskQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.listTasks(context.getTenantId(), context.getShopId(), param));
    }

    @GetMapping("/{taskId}")
    public CommonResult<AgentTaskDto> getTask(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return agentTaskService.getTask(context.getTenantId(), context.getShopId(), taskId)
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("任务不存在"));
    }

    @GetMapping("/{taskId}/steps")
    public CommonResult<List<AgentTaskStepDto>> listSteps(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.listSteps(context.getTenantId(), context.getShopId(), taskId));
    }

    @GetMapping("/{taskId}/events")
    public CommonResult<List<AgentTaskEventDto>> listEvents(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.listEvents(context.getTenantId(), context.getShopId(), taskId));
    }

    private DateRangeParam defaultDateRange(DateRangeParam requested) {
        if (requested != null && hasText(requested.getStart()) && hasText(requested.getEnd())) {
            return requested;
        }
        LocalDate today = LocalDate.now();
        DateRangeParam fallback = new DateRangeParam();
        fallback.setStart(today.toString());
        fallback.setEnd(today.toString());
        return fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
