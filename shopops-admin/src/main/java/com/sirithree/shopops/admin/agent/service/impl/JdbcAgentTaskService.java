package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentDispatchResult;
import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.agent.domain.AgentStepStatus;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStatus;
import com.sirithree.shopops.admin.agent.service.AgentTaskDispatcher;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.agent.service.TaskStatusTransitionValidator;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskEventMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskStepMapper;
import com.sirithree.shopops.admin.persistence.model.AgentTask;
import com.sirithree.shopops.admin.persistence.model.AgentTaskEvent;
import com.sirithree.shopops.admin.persistence.model.AgentTaskStep;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final AgentTaskDispatcher agentTaskDispatcher;
    private final JacksonJsonSupport jsonSupport;

    public JdbcAgentTaskService(AgentTaskMapper agentTaskMapper,
                                AgentTaskStepMapper agentTaskStepMapper,
                                AgentTaskEventMapper agentTaskEventMapper,
                                AgentTaskDispatcher agentTaskDispatcher,
                                JacksonJsonSupport jsonSupport) {
        this.agentTaskMapper = agentTaskMapper;
        this.agentTaskStepMapper = agentTaskStepMapper;
        this.agentTaskEventMapper = agentTaskEventMapper;
        this.agentTaskDispatcher = agentTaskDispatcher;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public AgentTaskCreateResult createTask(Long tenantId, Long shopId, Long userId, AgentTaskCreateParam param) {
        AgentTask task = newTask(tenantId, shopId, userId, param);
        agentTaskMapper.insert(task);
        appendEvent(task, null, AgentTaskStatus.CREATED.name(), "TASK_CREATED", userId);

        try {
            Map<Integer, Long> stepIdByStepNo = seedSteps(task);

            AgentTaskContext context = new AgentTaskContext();
            context.setTenantId(tenantId);
            context.setShopId(shopId);
            context.setUserId(userId);
            context.setTaskId(task.getId());
            context.setTraceId(task.getTraceId());
            context.setCreateParam(param);
            context.setStepIdByStepNo(stepIdByStepNo);

            if (agentTaskDispatcher.isAsynchronous()) {
                queueTask(task, userId);
                agentTaskDispatcher.dispatch(context);
                return new AgentTaskCreateResult(task.getId(), task.getTaskNo(), task.getStatus(), task.getTraceId());
            }

            startTask(task, userId);
            AgentDispatchResult dispatchResult = agentTaskDispatcher.dispatch(context);
            AgentExecutionResult result = dispatchResult.getExecutionResult();

            task.setReportId(result.getReportId());
            transitionTask(task, Boolean.TRUE.equals(result.getDegraded()) ? AgentTaskStatus.DEGRADED : AgentTaskStatus.SUCCESS);
            task.setResultSummary(Boolean.TRUE.equals(result.getDegraded())
                    ? "Daily review report generated with degraded evidence"
                    : "Daily review report generated");
            task.setFinishedAt(LocalDateTime.now());
            agentTaskMapper.updateExecutionState(task);
            appendEvent(task, AgentTaskStatus.RUNNING.name(), task.getStatus(), "TASK_FINISHED", userId);
        } catch (RuntimeException ex) {
            String fromStatus = task.getStatus();
            transitionTask(task, AgentTaskStatus.FAILED);
            task.setErrorCode("TASK_EXECUTE_ERROR");
            task.setErrorMessage(ex.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            agentTaskMapper.updateExecutionState(task);
            appendEvent(task, fromStatus, AgentTaskStatus.FAILED.name(), "TASK_FAILED", userId);
        }

        return new AgentTaskCreateResult(task.getId(), task.getTaskNo(), task.getStatus(), task.getTraceId());
    }

    @Override
    public AgentTaskCreateResult retryTask(Long tenantId, Long shopId, Long userId, Long taskId) {
        AgentTask original = agentTaskMapper.selectById(tenantId, shopId, taskId);
        if (original == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        AgentTaskCreateParam param = jsonSupport.fromJson(original.getPlanJson(), AgentTaskCreateParam.class);
        if (param == null) {
            throw new IllegalArgumentException("原任务缺少请求快照，无法重试");
        }
        appendEvent(original, original.getStatus(), original.getStatus(), "TASK_RETRY_REQUESTED", userId);
        return createTask(tenantId, shopId, userId, param);
    }

    @Override
    public CommonPage<AgentTaskDto> listTasks(Long tenantId, Long shopId, AgentTaskQueryParam param) {
        AgentTaskQueryParam query = param == null ? new AgentTaskQueryParam() : param;
        List<AgentTaskDto> list = agentTaskMapper.listByPage(
                        tenantId,
                        shopId,
                        query.getStatus(),
                        query.getTaskType(),
                        query.offset(),
                        query.safePageSize()
                ).stream()
                .map(this::toDto)
                .toList();
        Long total = agentTaskMapper.countByPage(tenantId, shopId, query.getStatus(), query.getTaskType());
        return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total);
    }

    @Override
    public Optional<AgentTaskDto> getTask(Long tenantId, Long shopId, Long taskId) {
        return Optional.ofNullable(agentTaskMapper.selectById(tenantId, shopId, taskId)).map(this::toDto);
    }

    @Override
    public List<AgentTaskStepDto> listSteps(Long tenantId, Long shopId, Long taskId) {
        return agentTaskStepMapper.listByTaskId(tenantId, shopId, taskId).stream().map(this::toStepDto).toList();
    }

    @Override
    public List<AgentTaskEventDto> listEvents(Long tenantId, Long shopId, Long taskId) {
        return agentTaskEventMapper.listByTaskId(tenantId, shopId, taskId).stream().map(this::toEventDto).toList();
    }

    private AgentTask newTask(Long tenantId, Long shopId, Long userId, AgentTaskCreateParam param) {
        AgentTask task = new AgentTask();
        task.setTenantId(tenantId);
        task.setShopId(shopId);
        task.setUserId(userId);
        task.setTaskNo("TASK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        task.setTaskType(param.getTaskType());
        task.setUserInput(param.getUserInput());
        task.setStatus(AgentTaskStatus.CREATED.name());
        task.setPriority(5);
        task.setPlanJson(jsonSupport.toJson(param));
        task.setTraceId("tr_" + UUID.randomUUID().toString().replace("-", ""));
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    private void startTask(AgentTask task, Long userId) {
        transitionTask(task, AgentTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        agentTaskMapper.updateExecutionState(task);
        appendEvent(task, AgentTaskStatus.CREATED.name(), AgentTaskStatus.RUNNING.name(), "TASK_STARTED", userId);
    }

    private void queueTask(AgentTask task, Long userId) {
        transitionTask(task, AgentTaskStatus.QUEUED);
        agentTaskMapper.updateExecutionState(task);
        appendEvent(task, AgentTaskStatus.CREATED.name(), AgentTaskStatus.QUEUED.name(), "TASK_QUEUED", userId);
    }

    private Map<Integer, Long> seedSteps(AgentTask task) {
        Map<Integer, Long> stepIdByStepNo = new HashMap<>();
        stepIdByStepNo.put(1, insertStep(task, 1, "Query order summary", "order.query_summary"));
        stepIdByStepNo.put(2, insertStep(task, 2, "Query negative comments", "comment.query_negative"));
        stepIdByStepNo.put(3, insertStep(task, 3, "Query product candidates", "product.query_candidates"));
        stepIdByStepNo.put(4, insertStep(task, 4, "Generate daily review report", "report.generate_daily_review"));
        return stepIdByStepNo;
    }

    private Long insertStep(AgentTask task, int stepNo, String stepName, String toolCode) {
        AgentTaskStep step = new AgentTaskStep();
        step.setTenantId(task.getTenantId());
        step.setShopId(task.getShopId());
        step.setTaskId(task.getId());
        step.setStepNo(stepNo);
        step.setStepName(stepName);
        step.setToolCode(toolCode);
        step.setStatus(AgentStepStatus.PENDING.name());
        step.setRetryCount(0);
        agentTaskStepMapper.insert(step);
        return step.getId();
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

    private AgentTaskEventDto toEventDto(AgentTaskEvent event) {
        AgentTaskEventDto dto = new AgentTaskEventDto();
        dto.setEventId(event.getId());
        dto.setTaskId(event.getTaskId());
        dto.setEventType(event.getEventType());
        dto.setFromStatus(event.getFromStatus());
        dto.setToStatus(event.getToStatus());
        dto.setEventData(jsonSupport.toMap(event.getEventDataJson()));
        dto.setOperatorId(event.getOperatorId());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }
}
