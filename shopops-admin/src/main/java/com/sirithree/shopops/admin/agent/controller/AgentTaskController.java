package com.sirithree.shopops.admin.agent.controller;

import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tasks")
public class AgentTaskController {
    private final AgentTaskService agentTaskService;

    public AgentTaskController(AgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    @PostMapping
    public CommonResult<AgentTaskCreateResult> createTask(@RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
                                                          @RequestHeader(value = "X-Shop-Id", defaultValue = "1") Long shopId,
                                                          @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                          @Valid @RequestBody AgentTaskCreateParam param) {
        return CommonResult.success(agentTaskService.createTask(tenantId, shopId, userId, param));
    }

    @GetMapping("/{taskId}")
    public CommonResult<AgentTaskDto> getTask(@RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
                                              @RequestHeader(value = "X-Shop-Id", defaultValue = "1") Long shopId,
                                              @PathVariable Long taskId) {
        return agentTaskService.getTask(tenantId, shopId, taskId)
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("任务不存在"));
    }

    @GetMapping("/{taskId}/steps")
    public CommonResult<List<AgentTaskStepDto>> listSteps(@RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
                                                          @RequestHeader(value = "X-Shop-Id", defaultValue = "1") Long shopId,
                                                          @PathVariable Long taskId) {
        return CommonResult.success(agentTaskService.listSteps(tenantId, shopId, taskId));
    }
}
