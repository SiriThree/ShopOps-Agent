package com.sirithree.shopops.admin.agent.controller;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDetailDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskMetricsDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.admin.agent.service.AgentTaskAdminService;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/agent/tasks")
public class AdminAgentTaskController {
    private final AgentTaskAdminService agentTaskAdminService;

    public AdminAgentTaskController(AgentTaskAdminService agentTaskAdminService) {
        this.agentTaskAdminService = agentTaskAdminService;
    }

    @GetMapping
    public CommonResult<CommonPage<AgentTaskDto>> listTasks(AgentTaskQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskAdminService.listTasks(context.getTenantId(), context.getShopId(), param));
    }

    @GetMapping("/{taskId}/detail")
    public CommonResult<AgentTaskDetailDto> getTaskDetail(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return agentTaskAdminService.getTaskDetail(context.getTenantId(), context.getShopId(), taskId)
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("Task not found"));
    }

    @GetMapping("/metrics")
    public CommonResult<AgentTaskMetricsDto> getTaskMetrics() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskAdminService.getTaskMetrics(context.getTenantId(), context.getShopId()));
    }
}
