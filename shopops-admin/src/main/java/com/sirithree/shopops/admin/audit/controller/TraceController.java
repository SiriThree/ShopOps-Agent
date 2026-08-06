package com.sirithree.shopops.admin.audit.controller;

import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.common.api.CommonResult;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TraceController {
    private final AgentTaskService agentTaskService;
    private final ToolCallLogService toolCallLogService;
    private final TraceService traceService;

    public TraceController(AgentTaskService agentTaskService, ToolCallLogService toolCallLogService, TraceService traceService) {
        this.agentTaskService = agentTaskService;
        this.toolCallLogService = toolCallLogService;
        this.traceService = traceService;
    }

    @GetMapping("/{taskId}/trace")
    public CommonResult<?> getTaskTrace(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return agentTaskService.getTask(context.getTenantId(), context.getShopId(), taskId)
                .<CommonResult<?>>map(task -> CommonResult.success(Map.of(
                        "traceId", task.getTraceId(),
                        "task", task,
                        "spans", traceService.listSpans(context.getTenantId(), task.getTraceId()),
                        "steps", agentTaskService.listSteps(context.getTenantId(), context.getShopId(), taskId),
                        "toolCalls", toolCallLogService.listByTaskId(context.getTenantId(), context.getShopId(), taskId)
                )))
                .orElseGet(() -> CommonResult.failed("任务不存在"));
    }
}
