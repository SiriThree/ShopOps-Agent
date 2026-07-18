package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDispatchMessage;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStatus;
import com.sirithree.shopops.admin.agent.service.AgentEngineService;
import com.sirithree.shopops.admin.agent.service.TaskStatusTransitionValidator;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskEventMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskStepMapper;
import com.sirithree.shopops.admin.persistence.model.AgentTask;
import com.sirithree.shopops.admin.persistence.model.AgentTaskEvent;
import com.sirithree.shopops.admin.persistence.model.AgentTaskStep;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcAgentTaskExecutionWorker {
    private final AgentTaskMapper agentTaskMapper;
    private final AgentTaskStepMapper agentTaskStepMapper;
    private final AgentTaskEventMapper agentTaskEventMapper;
    private final AgentEngineService agentEngineService;
    private final JacksonJsonSupport jsonSupport;

    public JdbcAgentTaskExecutionWorker(AgentTaskMapper agentTaskMapper,
                                        AgentTaskStepMapper agentTaskStepMapper,
                                        AgentTaskEventMapper agentTaskEventMapper,
                                        AgentEngineService agentEngineService,
                                        JacksonJsonSupport jsonSupport) {
        this.agentTaskMapper = agentTaskMapper;
        this.agentTaskStepMapper = agentTaskStepMapper;
        this.agentTaskEventMapper = agentTaskEventMapper;
        this.agentEngineService = agentEngineService;
        this.jsonSupport = jsonSupport;
    }

    public void execute(AgentTaskDispatchMessage message) {
        AgentTask task = agentTaskMapper.selectById(message.getTenantId(), message.getShopId(), message.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("Task does not exist: " + message.getTaskId());
        }
        AgentTaskStatus currentStatus = TaskStatusTransitionValidator.parse(task.getStatus());
        if (currentStatus == AgentTaskStatus.SUCCESS || currentStatus == AgentTaskStatus.FAILED || currentStatus == AgentTaskStatus.DEGRADED) {
            return;
        }
        if (currentStatus != AgentTaskStatus.QUEUED) {
            return;
        }

        try {
            if (!startQueuedTask(task, message.getUserId())) {
                return;
            }
            AgentExecutionResult result = agentEngineService.executeTask(buildContext(task, message));
            finishTask(task, result, message.getUserId());
        } catch (RuntimeException ex) {
            failTask(task, message.getUserId(), ex);
        }
    }

    private AgentTaskContext buildContext(AgentTask task, AgentTaskDispatchMessage message) {
        AgentTaskCreateParam param = jsonSupport.fromJson(task.getPlanJson(), AgentTaskCreateParam.class);
        if (param == null) {
            throw new IllegalArgumentException("Task request snapshot is missing: " + task.getId());
        }
        Map<Integer, Long> stepIdByStepNo = agentTaskStepMapper
                .listByTaskId(task.getTenantId(), task.getShopId(), task.getId())
                .stream()
                .collect(Collectors.toMap(AgentTaskStep::getStepNo, AgentTaskStep::getId));

        AgentTaskContext context = new AgentTaskContext();
        context.setTenantId(task.getTenantId());
        context.setShopId(task.getShopId());
        context.setUserId(task.getUserId());
        context.setTaskId(task.getId());
        context.setTraceId(message.getTraceId() == null ? task.getTraceId() : message.getTraceId());
        context.setCreateParam(param);
        context.setStepIdByStepNo(stepIdByStepNo);
        return context;
    }

    private boolean startQueuedTask(AgentTask task, Long userId) {
        LocalDateTime startedAt = LocalDateTime.now();
        int updated = agentTaskMapper.updateStatusIfCurrent(
                task.getTenantId(),
                task.getShopId(),
                task.getId(),
                AgentTaskStatus.QUEUED.name(),
                AgentTaskStatus.RUNNING.name(),
                startedAt
        );
        if (updated == 0) {
            return false;
        }
        task.setStatus(AgentTaskStatus.RUNNING.name());
        task.setStartedAt(startedAt);
        appendEvent(task, AgentTaskStatus.QUEUED.name(), AgentTaskStatus.RUNNING.name(), "TASK_STARTED", userId);
        return true;
    }

    private void finishTask(AgentTask task, AgentExecutionResult result, Long userId) {
        task.setReportId(result.getReportId());
        transitionTask(task, Boolean.TRUE.equals(result.getDegraded()) ? AgentTaskStatus.DEGRADED : AgentTaskStatus.SUCCESS);
        task.setResultSummary(Boolean.TRUE.equals(result.getDegraded())
                ? "Daily review report generated with degraded evidence"
                : "Daily review report generated");
        task.setFinishedAt(LocalDateTime.now());
        agentTaskMapper.updateExecutionState(task);
        appendEvent(task, AgentTaskStatus.RUNNING.name(), task.getStatus(), "TASK_FINISHED", userId);
    }

    private void failTask(AgentTask task, Long userId, RuntimeException ex) {
        String fromStatus = task.getStatus();
        transitionTask(task, AgentTaskStatus.FAILED);
        task.setErrorCode("TASK_EXECUTE_ERROR");
        task.setErrorMessage(ex.getMessage());
        task.setFinishedAt(LocalDateTime.now());
        agentTaskMapper.updateExecutionState(task);
        appendEvent(task, fromStatus, AgentTaskStatus.FAILED.name(), "TASK_FAILED", userId);
    }

    private void transitionTask(AgentTask task, AgentTaskStatus toStatus) {
        TaskStatusTransitionValidator.requireTransition(task.getStatus(), toStatus);
        task.setStatus(toStatus.name());
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
}
