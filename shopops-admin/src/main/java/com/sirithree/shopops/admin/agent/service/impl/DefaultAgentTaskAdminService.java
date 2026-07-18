package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDetailDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventQueryParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskMetricsDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.admin.agent.service.AgentTaskAdminService;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskEventMapper;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskMapper;
import com.sirithree.shopops.admin.persistence.model.AgentTaskEvent;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.common.api.CommonPage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultAgentTaskAdminService implements AgentTaskAdminService {
    private final AgentTaskService agentTaskService;
    private final OperationReportService operationReportService;
    private final TraceService traceService;
    private final ToolCallLogService toolCallLogService;
    private final AgentTaskMapper agentTaskMapper;
    private final AgentTaskEventMapper agentTaskEventMapper;
    private final JacksonJsonSupport jsonSupport;
    private final String persistenceMode;

    public DefaultAgentTaskAdminService(AgentTaskService agentTaskService,
                                        OperationReportService operationReportService,
                                        TraceService traceService,
                                        ToolCallLogService toolCallLogService,
                                        AgentTaskMapper agentTaskMapper,
                                        AgentTaskEventMapper agentTaskEventMapper,
                                        JacksonJsonSupport jsonSupport,
                                        @Value("${shopops.persistence:memory}") String persistenceMode) {
        this.agentTaskService = agentTaskService;
        this.operationReportService = operationReportService;
        this.traceService = traceService;
        this.toolCallLogService = toolCallLogService;
        this.agentTaskMapper = agentTaskMapper;
        this.agentTaskEventMapper = agentTaskEventMapper;
        this.jsonSupport = jsonSupport;
        this.persistenceMode = persistenceMode;
    }

    @Override
    public CommonPage<AgentTaskDto> listTasks(Long tenantId, Long shopId, AgentTaskQueryParam param) {
        return agentTaskService.listTasks(tenantId, shopId, param);
    }

    @Override
    public CommonPage<AgentTaskEventDto> listEvents(Long tenantId, Long shopId, AgentTaskEventQueryParam param) {
        AgentTaskEventQueryParam query = param == null ? new AgentTaskEventQueryParam() : param;
        if ("jdbc".equalsIgnoreCase(persistenceMode)) {
            List<AgentTaskEventDto> list = agentTaskEventMapper.listByPage(
                            tenantId,
                            shopId,
                            query,
                            query.offset(),
                            query.safePageSize()
                    ).stream()
                    .map(this::toEventDto)
                    .toList();
            Long total = agentTaskEventMapper.countByPage(tenantId, shopId, query);
            return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total);
        }
        return memoryEvents(tenantId, shopId, query);
    }

    @Override
    public Optional<AgentTaskDetailDto> getTaskDetail(Long tenantId, Long shopId, Long taskId) {
        return agentTaskService.getTask(tenantId, shopId, taskId).map(task -> {
            AgentTaskDetailDto detail = new AgentTaskDetailDto();
            detail.setTask(task);
            detail.setSteps(agentTaskService.listSteps(tenantId, shopId, taskId));
            detail.setEvents(agentTaskService.listEvents(tenantId, shopId, taskId));
            if (task.getReportId() != null) {
                operationReportService.getReport(tenantId, shopId, task.getReportId()).ifPresent(detail::setReport);
            }
            detail.setSpans(traceService.listSpans(tenantId, task.getTraceId()));
            detail.setToolCalls(toolCallLogService.listByTaskId(tenantId, shopId, taskId));
            return detail;
        });
    }

    @Override
    public AgentTaskMetricsDto getTaskMetrics(Long tenantId, Long shopId) {
        if ("jdbc".equalsIgnoreCase(persistenceMode)) {
            return jdbcMetrics(tenantId, shopId);
        }
        return memoryMetrics(tenantId, shopId);
    }

    private AgentTaskMetricsDto jdbcMetrics(Long tenantId, Long shopId) {
        List<Map<String, Object>> rows = agentTaskMapper.countGroupByStatus(tenantId, shopId);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        long total = 0L;
        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("taskStatus"));
            long count = number(row.get("taskCount")).longValue();
            statusCounts.put(status, count);
            total += count;
        }
        Long avgLatency = agentTaskMapper.selectAverageLatencyMs(tenantId, shopId);
        return toMetrics(statusCounts, total, avgLatency == null ? 0L : avgLatency);
    }

    private AgentTaskMetricsDto memoryMetrics(Long tenantId, Long shopId) {
        AgentTaskQueryParam query = new AgentTaskQueryParam();
        query.setPageNum(1);
        query.setPageSize(100);
        CommonPage<AgentTaskDto> page = agentTaskService.listTasks(tenantId, shopId, query);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (AgentTaskDto task : page.getList()) {
            statusCounts.merge(task.getStatus(), 1L, Long::sum);
        }
        return toMetrics(statusCounts, page.getTotal(), 0L);
    }

    private CommonPage<AgentTaskEventDto> memoryEvents(Long tenantId, Long shopId, AgentTaskEventQueryParam query) {
        List<AgentTaskEventDto> events = new ArrayList<>();
        if (query.getTaskId() != null) {
            events.addAll(agentTaskService.listEvents(tenantId, shopId, query.getTaskId()));
        } else {
            AgentTaskQueryParam taskQuery = new AgentTaskQueryParam();
            taskQuery.setPageNum(1);
            taskQuery.setPageSize(100);
            for (AgentTaskDto task : agentTaskService.listTasks(tenantId, shopId, taskQuery).getList()) {
                events.addAll(agentTaskService.listEvents(tenantId, shopId, task.getTaskId()));
            }
        }
        List<AgentTaskEventDto> filtered = events.stream()
                .filter(event -> matches(query.getEventType(), event.getEventType()))
                .filter(event -> matches(query.getFromStatus(), event.getFromStatus()))
                .filter(event -> matches(query.getToStatus(), event.getToStatus()))
                .filter(event -> query.getOperatorId() == null || query.getOperatorId().equals(event.getOperatorId()))
                .filter(event -> query.getCreatedStart() == null || (event.getCreatedAt() != null && !event.getCreatedAt().isBefore(query.getCreatedStart())))
                .filter(event -> query.getCreatedEnd() == null || (event.getCreatedAt() != null && !event.getCreatedAt().isAfter(query.getCreatedEnd())))
                .sorted(Comparator.comparing(AgentTaskEventDto::getEventId).reversed())
                .toList();
        List<AgentTaskEventDto> pageList = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(pageList, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    private AgentTaskMetricsDto toMetrics(Map<String, Long> statusCounts, long total, long avgLatencyMs) {
        AgentTaskMetricsDto metrics = new AgentTaskMetricsDto();
        metrics.setStatusBreakdown(statusCounts);
        metrics.setTotal(total);
        metrics.setCreated(statusCounts.getOrDefault("CREATED", 0L));
        metrics.setQueued(statusCounts.getOrDefault("QUEUED", 0L));
        metrics.setRunning(statusCounts.getOrDefault("RUNNING", 0L));
        metrics.setSuccess(statusCounts.getOrDefault("SUCCESS", 0L));
        metrics.setFailed(statusCounts.getOrDefault("FAILED", 0L));
        metrics.setDegraded(statusCounts.getOrDefault("DEGRADED", 0L));
        metrics.setAvgLatencyMs(avgLatencyMs);
        metrics.setSuccessRate(successRate(metrics.getSuccess(), total));
        return metrics;
    }

    private double successRate(long success, long total) {
        if (total <= 0) {
            return 0.0d;
        }
        return BigDecimal.valueOf(success)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : 0L;
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
