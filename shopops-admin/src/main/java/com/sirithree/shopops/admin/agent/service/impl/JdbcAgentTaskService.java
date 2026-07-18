package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.agent.service.AgentEngineService;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskEventMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskStepMapper;
import com.sirithree.shopops.admin.persistence.model.AgentTask;
import com.sirithree.shopops.admin.persistence.model.AgentTaskEvent;
import com.sirithree.shopops.admin.persistence.model.AgentTaskStep;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcAgentTaskService implements AgentTaskService {
    private final AgentTaskMapper agentTaskMapper;
    private final AgentTaskStepMapper agentTaskStepMapper;
    private final AgentTaskEventMapper agentTaskEventMapper;
    private final AgentEngineService agentEngineService;

    public JdbcAgentTaskService(AgentTaskMapper agentTaskMapper,
                                AgentTaskStepMapper agentTaskStepMapper,
                                AgentTaskEventMapper agentTaskEventMapper,
                                AgentEngineService agentEngineService) {
        this.agentTaskMapper = agentTaskMapper;
        this.agentTaskStepMapper = agentTaskStepMapper;
        this.agentTaskEventMapper = agentTaskEventMapper;
        this.agentEngineService = agentEngineService;
    }

    @Override
    public AgentTaskCreateResult createTask(Long tenantId, Long shopId, Long userId, AgentTaskCreateParam param) {
        AgentTask task = new AgentTask();
        task.setTenantId(tenantId);
        task.setShopId(shopId);
        task.setUserId(userId);
        task.setTaskNo("TASK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        task.setTaskType(param.getTaskType());
        task.setUserInput(param.getUserInput());
        task.setStatus("CREATED");
        task.setPriority(5);
        task.setTraceId("tr_" + UUID.randomUUID().toString().replace("-", ""));
        task.setCreatedAt(LocalDateTime.now());
        agentTaskMapper.insert(task);
        appendEvent(task, null, "CREATED", "TASK_CREATED", userId);

        try {
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            agentTaskMapper.updateExecutionState(task);
            appendEvent(task, "CREATED", "RUNNING", "TASK_STARTED", userId);
            seedSteps(task);

            AgentTaskContext context = new AgentTaskContext();
            context.setTenantId(tenantId);
            context.setShopId(shopId);
            context.setUserId(userId);
            context.setTaskId(task.getId());
            context.setTraceId(task.getTraceId());
            context.setCreateParam(param);
            AgentExecutionResult result = agentEngineService.executeTask(context);

            task.setReportId(result.getReportId());
            task.setStatus(Boolean.TRUE.equals(result.getDegraded()) ? "DEGRADED" : "SUCCESS");
            task.setResultSummary(Boolean.TRUE.equals(result.getDegraded()) ? "报告已降级生成" : "每日经营复盘已生成");
            task.setFinishedAt(LocalDateTime.now());
            agentTaskMapper.updateExecutionState(task);
            appendEvent(task, "RUNNING", task.getStatus(), "TASK_FINISHED", userId);
        } catch (RuntimeException ex) {
            task.setStatus("FAILED");
            task.setErrorCode("TASK_EXECUTE_ERROR");
            task.setErrorMessage(ex.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            agentTaskMapper.updateExecutionState(task);
            appendEvent(task, "RUNNING", "FAILED", "TASK_FAILED", userId);
        }

        return new AgentTaskCreateResult(task.getId(), task.getTaskNo(), task.getStatus(), task.getTraceId());
    }

    @Override
    public Optional<AgentTaskDto> getTask(Long tenantId, Long shopId, Long taskId) {
        return Optional.ofNullable(agentTaskMapper.selectById(tenantId, shopId, taskId)).map(this::toDto);
    }

    @Override
    public List<AgentTaskStepDto> listSteps(Long tenantId, Long shopId, Long taskId) {
        return agentTaskStepMapper.listByTaskId(tenantId, shopId, taskId).stream().map(this::toStepDto).toList();
    }

    private void seedSteps(AgentTask task) {
        insertStep(task, 1, "查询订单核心指标", "order.query_summary");
        insertStep(task, 2, "查询差评风险", "comment.query_negative");
        insertStep(task, 3, "查询待优化商品", "product.query_candidates");
        insertStep(task, 4, "生成经营复盘报告", "report.generate_daily_review");
    }

    private void insertStep(AgentTask task, int stepNo, String stepName, String toolCode) {
        AgentTaskStep step = new AgentTaskStep();
        step.setTenantId(task.getTenantId());
        step.setShopId(task.getShopId());
        step.setTaskId(task.getId());
        step.setStepNo(stepNo);
        step.setStepName(stepName);
        step.setToolCode(toolCode);
        step.setStatus("PENDING");
        step.setRetryCount(0);
        agentTaskStepMapper.insert(step);
    }

    private void appendEvent(AgentTask task, String fromStatus, String toStatus, String eventType, Long userId) {
        AgentTaskEvent event = new AgentTaskEvent();
        event.setTenantId(task.getTenantId());
        event.setShopId(task.getShopId());
        event.setTaskId(task.getId());
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setOperatorId(userId);
        event.setCreatedAt(LocalDateTime.now());
        agentTaskEventMapper.insert(event);
    }

    private AgentTaskDto toDto(AgentTask task) {
        AgentTaskDto dto = new AgentTaskDto();
        dto.setTaskId(task.getId());
        dto.setTenantId(task.getTenantId());
        dto.setShopId(task.getShopId());
        dto.setUserId(task.getUserId());
        dto.setTaskNo(task.getTaskNo());
        dto.setTaskType(task.getTaskType());
        dto.setUserInput(task.getUserInput());
        dto.setStatus(task.getStatus());
        dto.setTraceId(task.getTraceId());
        dto.setReportId(task.getReportId());
        dto.setResultSummary(task.getResultSummary());
        dto.setErrorMessage(task.getErrorMessage());
        return dto;
    }

    private AgentTaskStepDto toStepDto(AgentTaskStep step) {
        AgentTaskStepDto dto = new AgentTaskStepDto();
        dto.setStepId(step.getId());
        dto.setTaskId(step.getTaskId());
        dto.setStepNo(step.getStepNo());
        dto.setStepName(step.getStepName());
        dto.setToolCode(step.getToolCode());
        dto.setStatus(step.getStatus());
        dto.setInput(step.getInputJson());
        dto.setOutput(step.getOutputJson());
        dto.setErrorMessage(step.getErrorMessage());
        return dto;
    }
}
