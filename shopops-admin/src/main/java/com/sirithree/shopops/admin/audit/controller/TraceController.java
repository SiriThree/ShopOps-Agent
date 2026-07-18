package com.sirithree.shopops.admin.audit.controller;

import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.common.api.CommonResult;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public CommonResult<?> getTaskTrace(@RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
                                        @RequestHeader(value = "X-Shop-Id", defaultValue = "1") Long shopId,
                                        @PathVariable Long taskId) {
        return agentTaskService.getTask(tenantId, shopId, taskId)
                .<CommonResult<?>>map(task -> CommonResult.success(Map.of(
                        "traceId", task.getTraceId(),
                        "task", task,
                        "spans", traceService.listSpans(tenantId, task.getTraceId()),
                        "steps", agentTaskService.listSteps(tenantId, shopId, taskId),
                        "toolCalls", toolCallLogService.listByTaskId(taskId)
                )))
                .orElseGet(() -> CommonResult.failed("任务不存在"));
    }
}
