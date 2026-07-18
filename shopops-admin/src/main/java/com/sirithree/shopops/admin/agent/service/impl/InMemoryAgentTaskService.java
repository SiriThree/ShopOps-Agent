package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.agent.service.AgentEngineService;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryAgentTaskService implements AgentTaskService {
    private final AtomicLong taskIdGenerator = new AtomicLong(10001);
    private final Map<Long, AgentTaskDto> tasks = new ConcurrentHashMap<>();
    private final Map<Long, List<AgentTaskStepDto>> steps = new ConcurrentHashMap<>();
    private final AgentEngineService agentEngineService;

    public InMemoryAgentTaskService(AgentEngineService agentEngineService) {
        this.agentEngineService = agentEngineService;
    }

    @Override
    public AgentTaskCreateResult createTask(Long tenantId, Long shopId, Long userId, AgentTaskCreateParam param) {
        Long taskId = taskIdGenerator.getAndIncrement();
        String taskNo = "TASK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + taskId;
        String traceId = "tr_" + UUID.randomUUID().toString().replace("-", "");

        AgentTaskDto task = new AgentTaskDto();
        task.setTaskId(taskId);
        task.setTenantId(tenantId);
        task.setShopId(shopId);
        task.setUserId(userId);
        task.setTaskNo(taskNo);
        task.setTaskType(param.getTaskType());
        task.setUserInput(param.getUserInput());
        task.setStatus("CREATED");
        task.setTraceId(traceId);
        tasks.put(taskId, task);

        try {
            task.setStatus("RUNNING");
            AgentTaskContext context = new AgentTaskContext();
            context.setTenantId(tenantId);
            context.setShopId(shopId);
            context.setUserId(userId);
            context.setTaskId(taskId);
            context.setTraceId(traceId);
            context.setCreateParam(param);
            seedSteps(taskId);
            AgentExecutionResult result = agentEngineService.executeTask(context);
            task.setReportId(result.getReportId());
            task.setStatus(Boolean.TRUE.equals(result.getDegraded()) ? "DEGRADED" : "SUCCESS");
            task.setResultSummary(Boolean.TRUE.equals(result.getDegraded()) ? "报告已降级生成" : "每日经营复盘已生成");
        } catch (RuntimeException ex) {
            task.setStatus("FAILED");
            task.setErrorMessage(ex.getMessage());
        }

        return new AgentTaskCreateResult(taskId, taskNo, task.getStatus(), traceId);
    }

    @Override
    public Optional<AgentTaskDto> getTask(Long tenantId, Long shopId, Long taskId) {
        return Optional.ofNullable(tasks.get(taskId))
                .filter(task -> tenantId.equals(task.getTenantId()) && shopId.equals(task.getShopId()));
    }

    @Override
    public List<AgentTaskStepDto> listSteps(Long tenantId, Long shopId, Long taskId) {
        return steps.getOrDefault(taskId, List.of());
    }

    private void seedSteps(Long taskId) {
        List<AgentTaskStepDto> taskSteps = new ArrayList<>();
        taskSteps.add(step(taskId, 1, "查询订单核心指标", "order.query_summary"));
        taskSteps.add(step(taskId, 2, "查询差评风险", "comment.query_negative"));
        taskSteps.add(step(taskId, 3, "查询待优化商品", "product.query_candidates"));
        taskSteps.add(step(taskId, 4, "生成经营复盘报告", "report.generate_daily_review"));
        steps.put(taskId, taskSteps);
    }

    private AgentTaskStepDto step(Long taskId, int stepNo, String stepName, String toolCode) {
        AgentTaskStepDto step = new AgentTaskStepDto();
        step.setStepId((long) stepNo);
        step.setTaskId(taskId);
        step.setStepNo(stepNo);
        step.setStepName(stepName);
        step.setToolCode(toolCode);
        step.setStatus("SUCCESS");
        return step;
    }
}
