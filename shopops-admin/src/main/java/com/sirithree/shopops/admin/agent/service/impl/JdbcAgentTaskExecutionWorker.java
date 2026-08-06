package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDispatchMessage;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStatus;
import com.sirithree.shopops.admin.agent.service.AgentEngineService;
import com.sirithree.shopops.admin.agent.service.TaskStatusTransitionValidator;
import com.sirithree.shopops.admin.agent.reliability.TaskErrorClassifier;
import com.sirithree.shopops.admin.agent.reliability.TaskErrorType;
import com.sirithree.shopops.admin.auth.domain.PermissionCode;
import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskEventMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskStepMapper;
import com.sirithree.shopops.admin.persistence.model.AgentTask;
import com.sirithree.shopops.admin.persistence.model.AgentTaskEvent;
import com.sirithree.shopops.admin.persistence.model.AgentTaskStep;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;
import java.util.LinkedHashMap;
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
    private final AuthorizationService authorizationService;
    private final JacksonJsonSupport jsonSupport;
    private final TaskErrorClassifier errorClassifier;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);

    public JdbcAgentTaskExecutionWorker(AgentTaskMapper agentTaskMapper,
                                        AgentTaskStepMapper agentTaskStepMapper,
                                        AgentTaskEventMapper agentTaskEventMapper,
                                        AgentEngineService agentEngineService,
                                        AuthorizationService authorizationService,
                                        JacksonJsonSupport jsonSupport,
                                        TaskErrorClassifier errorClassifier) {
        this.agentTaskMapper = agentTaskMapper;
        this.agentTaskStepMapper = agentTaskStepMapper;
        this.agentTaskEventMapper = agentTaskEventMapper;
        this.agentEngineService = agentEngineService;
        this.authorizationService = authorizationService;
        this.jsonSupport = jsonSupport;
        this.errorClassifier = errorClassifier;
    }

    public void execute(AgentTaskDispatchMessage message) {
        AgentTask task = agentTaskMapper.selectById(message.getTenantId(), message.getShopId(), message.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("Task does not exist: " + message.getTaskId());
        }
        if (!task.getTenantId().equals(message.getTenantId()) || !task.getShopId().equals(message.getShopId())
                || !task.getUserId().equals(message.getUserId())) {
            throw new SecurityException("Dispatch identity does not match persisted task");
        }
        if (!authorizationService.isAuthorized(task.getTenantId(), task.getShopId(), task.getUserId(),
                PermissionCode.AGENT_EXECUTE)) {
            failTask(task, task.getUserId(), new SecurityException("Agent execution permission has been revoked"));
            return;
        }
        AgentTaskStatus currentStatus = TaskStatusTransitionValidator.parse(task.getStatus());
        if (currentStatus.terminal() || currentStatus == AgentTaskStatus.CANCEL_REQUESTED) return;
        if (currentStatus != AgentTaskStatus.QUEUED && currentStatus != AgentTaskStatus.RETRYING) return;

        String workerId = "worker-" + UUID.randomUUID();
        try {
            if (!acquireLease(task, workerId, message.getUserId())) {
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

    private boolean acquireLease(AgentTask task, String workerId, Long userId) {
        LocalDateTime startedAt = LocalDateTime.now();
        int updated = agentTaskMapper.acquireLease(task.getTenantId(), task.getShopId(), task.getId(),
                workerId, startedAt, startedAt.plus(LEASE_DURATION));
        if (updated == 0) {
            return false;
        }
        task.setStatus(AgentTaskStatus.RUNNING.name());
        task.setStartedAt(startedAt);
        task.setWorkerId(workerId);
        task.setLockedAt(startedAt);
        task.setHeartbeatAt(startedAt);
        task.setLeaseExpireAt(startedAt.plus(LEASE_DURATION));
        appendEvent(task, AgentTaskStatus.QUEUED.name(), AgentTaskStatus.RUNNING.name(), "TASK_STARTED", userId);
        return true;
    }

    private void finishTask(AgentTask task, AgentExecutionResult result, Long userId) {
        AgentTaskCreateParam param = jsonSupport.fromJson(task.getPlanJson(), AgentTaskCreateParam.class);
        task.setReportId(result.getReportId());
        transitionTask(task, Boolean.TRUE.equals(result.getDegraded()) ? AgentTaskStatus.NEEDS_MANUAL_ACTION : AgentTaskStatus.SUCCEEDED);
        task.setResultSummary(RulePlannerService.taskResultSummary(
                param == null ? null : param.getIntent(),
                Boolean.TRUE.equals(result.getDegraded())
        ));
        task.setFinishedAt(LocalDateTime.now());
        agentTaskMapper.updateExecutionState(task);
        appendEvent(task, AgentTaskStatus.RUNNING.name(), task.getStatus(), "TASK_FINISHED", userId);
    }

    private void failTask(AgentTask task, Long userId, RuntimeException ex) {
        String fromStatus = task.getStatus();
        TaskErrorType errorType = errorClassifier.classify(ex);
        AgentTaskStatus target = errorType.requiresLookup() || errorType.manualAfterFailure()
                ? AgentTaskStatus.NEEDS_MANUAL_ACTION : AgentTaskStatus.FAILED;
        transitionTask(task, target);
        task.setErrorType(errorType.name());
        task.setStatusReason(errorType.requiresLookup() ? "External result must be checked before retry" : ex.getMessage());
        task.setErrorCode("TASK_" + errorType.name());
        task.setErrorMessage(ex.getMessage());
        task.setFinishedAt(LocalDateTime.now());
        agentTaskMapper.updateExecutionState(task);
        appendEvent(task, fromStatus, target.name(), "TASK_FAILED", userId);
    }

    private void transitionTask(AgentTask task, AgentTaskStatus toStatus) {
        TaskStatusTransitionValidator.requireTransition(task.getStatus(), toStatus);
        task.setStatus(statusValue(toStatus));
    }

    private String statusValue(AgentTaskStatus status) {
        if (status == AgentTaskStatus.SUCCEEDED) return "SUCCESS";
        if (status == AgentTaskStatus.NEEDS_MANUAL_ACTION) return "DEGRADED";
        return status.name();
    }

    private void appendEvent(AgentTask task, String fromStatus, String toStatus, String eventType, Long userId) {
        AgentTaskEvent event = new AgentTaskEvent();
        event.setTenantId(task.getTenantId());
        event.setShopId(task.getShopId());
        event.setTaskId(task.getId());
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setEventDataJson(jsonSupport.toJson(eventData(task, eventType)));
        event.setOperatorId(userId);
        event.setCreatedAt(LocalDateTime.now());
        agentTaskEventMapper.insert(event);
    }

    private Map<String, Object> eventData(AgentTask task, String eventType) {
        Map<String, Object> data = new LinkedHashMap<>();
        putIfPresent(data, "taskNo", task.getTaskNo());
        putIfPresent(data, "taskType", task.getTaskType());
        putIfPresent(data, "traceId", task.getTraceId());
        putIfPresent(data, "reportId", task.getReportId());
        putIfPresent(data, "errorCode", task.getErrorCode());
        putIfPresent(data, "errorMessage", task.getErrorMessage());
        putIfPresent(data, "eventType", eventType);
        return data;
    }

    private void putIfPresent(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }
}
