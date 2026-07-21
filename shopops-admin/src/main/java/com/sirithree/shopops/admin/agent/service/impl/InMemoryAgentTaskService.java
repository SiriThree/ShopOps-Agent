package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentDispatchResult;
import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskRecoveryResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.agent.domain.AgentStepStatus;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStatus;
import com.sirithree.shopops.admin.agent.service.AgentTaskDispatcher;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.agent.service.TaskStatusTransitionValidator;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private final AtomicLong eventIdGenerator = new AtomicLong(1);
    private final Map<Long, AgentTaskDto> tasks = new ConcurrentHashMap<>();
    private final Map<Long, List<AgentTaskStepDto>> steps = new ConcurrentHashMap<>();
    private final Map<Long, List<AgentTaskEventDto>> events = new ConcurrentHashMap<>();
    private final Map<Long, AgentTaskCreateParam> originalParams = new ConcurrentHashMap<>();
    private final AgentTaskDispatcher agentTaskDispatcher;

    public InMemoryAgentTaskService(AgentTaskDispatcher agentTaskDispatcher) {
        this.agentTaskDispatcher = agentTaskDispatcher;
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
        task.setStatus(AgentTaskStatus.CREATED.name());
        task.setTraceId(traceId);
        task.setCreatedAt(LocalDateTime.now());
        tasks.put(taskId, task);
        originalParams.put(taskId, param);
        appendEvent(task, null, AgentTaskStatus.CREATED.name(), "TASK_CREATED", userId);

        try {
            AgentTaskContext context = new AgentTaskContext();
            context.setTenantId(tenantId);
            context.setShopId(shopId);
            context.setUserId(userId);
            context.setTaskId(taskId);
            context.setTraceId(traceId);
            context.setCreateParam(param);
            seedSteps(taskId);

            if (agentTaskDispatcher.isAsynchronous()) {
                transitionTask(task, AgentTaskStatus.QUEUED);
                appendEvent(task, AgentTaskStatus.CREATED.name(), AgentTaskStatus.QUEUED.name(), "TASK_QUEUED", userId);
                agentTaskDispatcher.dispatch(context);
                return new AgentTaskCreateResult(taskId, taskNo, task.getStatus(), traceId);
            }

            transitionTask(task, AgentTaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            appendEvent(task, AgentTaskStatus.CREATED.name(), AgentTaskStatus.RUNNING.name(), "TASK_STARTED", userId);
            AgentDispatchResult dispatchResult = agentTaskDispatcher.dispatch(context);
            AgentExecutionResult result = dispatchResult.getExecutionResult();
            task.setReportId(result.getReportId());
            transitionTask(task, Boolean.TRUE.equals(result.getDegraded()) ? AgentTaskStatus.DEGRADED : AgentTaskStatus.SUCCESS);
            task.setResultSummary(Boolean.TRUE.equals(result.getDegraded())
                    ? "Daily review report generated with degraded evidence"
                    : "Daily review report generated");
            task.setFinishedAt(LocalDateTime.now());
            appendEvent(task, AgentTaskStatus.RUNNING.name(), task.getStatus(), "TASK_FINISHED", userId);
        } catch (RuntimeException ex) {
            String fromStatus = task.getStatus();
            transitionTask(task, AgentTaskStatus.FAILED);
            task.setErrorCode("TASK_EXECUTE_ERROR");
            task.setErrorMessage(ex.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            appendEvent(task, fromStatus, AgentTaskStatus.FAILED.name(), "TASK_FAILED", userId);
        }

        return new AgentTaskCreateResult(taskId, taskNo, task.getStatus(), traceId);
    }

    @Override
    public AgentTaskCreateResult retryTask(Long tenantId, Long shopId, Long userId, Long taskId) {
        AgentTaskDto original = tasks.get(taskId);
        if (original == null || !tenantId.equals(original.getTenantId()) || !shopId.equals(original.getShopId())) {
            throw new IllegalArgumentException("任务不存在");
        }
        AgentTaskCreateParam param = originalParams.get(taskId);
        if (param == null) {
            throw new IllegalArgumentException("原任务缺少请求快照，无法重试");
        }
        appendEvent(original, original.getStatus(), original.getStatus(), "TASK_RETRY_REQUESTED", userId);
        return createTask(tenantId, shopId, userId, param);
    }

    @Override
    public AgentTaskRecoveryResult requeueStaleTasks(Long tenantId, Long shopId, Long userId, Integer queuedTimeoutMinutes, Integer runningTimeoutMinutes, Integer limit) {
        return new AgentTaskRecoveryResult();
    }

    @Override
    public CommonPage<AgentTaskDto> listTasks(Long tenantId, Long shopId, AgentTaskQueryParam param) {
        AgentTaskQueryParam query = param == null ? new AgentTaskQueryParam() : param;
        List<AgentTaskDto> filtered = tasks.values().stream()
                .filter(task -> tenantId.equals(task.getTenantId()) && shopId.equals(task.getShopId()))
                .filter(task -> query.getStatus() == null || query.getStatus().isBlank() || query.getStatus().equals(task.getStatus()))
                .filter(task -> query.getTaskType() == null || query.getTaskType().isBlank() || query.getTaskType().equals(task.getTaskType()))
                .filter(task -> query.getTaskNo() == null || query.getTaskNo().isBlank() || query.getTaskNo().equals(task.getTaskNo()))
                .filter(task -> query.getUserId() == null || query.getUserId().equals(task.getUserId()))
                .filter(task -> query.getTraceId() == null || query.getTraceId().isBlank() || query.getTraceId().equals(task.getTraceId()))
                .filter(task -> query.getReportId() == null || query.getReportId().equals(task.getReportId()))
                .filter(task -> query.getCreatedStart() == null || (task.getCreatedAt() != null && !task.getCreatedAt().isBefore(query.getCreatedStart())))
                .filter(task -> query.getCreatedEnd() == null || (task.getCreatedAt() != null && !task.getCreatedAt().isAfter(query.getCreatedEnd())))
                .filter(task -> query.getFinishedStart() == null || (task.getFinishedAt() != null && !task.getFinishedAt().isBefore(query.getFinishedStart())))
                .filter(task -> query.getFinishedEnd() == null || (task.getFinishedAt() != null && !task.getFinishedAt().isAfter(query.getFinishedEnd())))
                .sorted(Comparator.comparing(AgentTaskDto::getTaskId).reversed())
                .toList();
        List<AgentTaskDto> pageList = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(pageList, query.safePageNum(), query.safePageSize(), (long) filtered.size());
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

    @Override
    public List<AgentTaskEventDto> listEvents(Long tenantId, Long shopId, Long taskId) {
        return getTask(tenantId, shopId, taskId)
                .map(task -> events.getOrDefault(taskId, List.of()))
                .orElseGet(List::of);
    }

    private void seedSteps(Long taskId) {
        List<AgentTaskStepDto> taskSteps = new ArrayList<>();
        taskSteps.add(step(taskId, 1, "查询订单核心指标", "order.query_summary"));
        taskSteps.add(step(taskId, 2, "查询差评风险", "comment.query_negative"));
        taskSteps.add(step(taskId, 3, "查询待优化商品", "product.query_candidates"));
        taskSteps.add(step(taskId, 4, "查询广告投放指标", "ad.query_performance"));
        taskSteps.add(step(taskId, 5, "查询外部报表指标", "report.query_external_metrics"));
        taskSteps.add(step(taskId, 6, "生成经营复盘报告", "report.generate_daily_review"));
        steps.put(taskId, taskSteps);
    }

    private AgentTaskStepDto step(Long taskId, int stepNo, String stepName, String toolCode) {
        AgentTaskStepDto step = new AgentTaskStepDto();
        step.setStepId((long) stepNo);
        step.setTaskId(taskId);
        step.setStepNo(stepNo);
        step.setStepName(stepName);
        step.setToolCode(toolCode);
        step.setStatus(AgentStepStatus.SUCCESS.name());
        return step;
    }

    private void transitionTask(AgentTaskDto task, AgentTaskStatus toStatus) {
        TaskStatusTransitionValidator.requireTransition(task.getStatus(), toStatus);
        task.setStatus(toStatus.name());
    }

    private void appendEvent(AgentTaskDto task, String fromStatus, String toStatus, String eventType, Long operatorId) {
        AgentTaskEventDto event = new AgentTaskEventDto();
        event.setEventId(eventIdGenerator.getAndIncrement());
        event.setTaskId(task.getTaskId());
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setEventData(eventData(task, eventType));
        event.setOperatorId(operatorId);
        event.setCreatedAt(LocalDateTime.now());
        events.computeIfAbsent(task.getTaskId(), ignored -> new ArrayList<>()).add(event);
    }

    private Map<String, Object> eventData(AgentTaskDto task, String eventType) {
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
