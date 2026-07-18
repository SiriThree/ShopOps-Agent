package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDetailDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskMetricsDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.admin.agent.service.AgentTaskAdminService;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskMapper;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.common.api.CommonPage;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final String persistenceMode;

    public DefaultAgentTaskAdminService(AgentTaskService agentTaskService,
                                        OperationReportService operationReportService,
                                        TraceService traceService,
                                        ToolCallLogService toolCallLogService,
                                        AgentTaskMapper agentTaskMapper,
                                        @Value("${shopops.persistence:memory}") String persistenceMode) {
        this.agentTaskService = agentTaskService;
        this.operationReportService = operationReportService;
        this.traceService = traceService;
        this.toolCallLogService = toolCallLogService;
        this.agentTaskMapper = agentTaskMapper;
        this.persistenceMode = persistenceMode;
    }

    @Override
    public CommonPage<AgentTaskDto> listTasks(Long tenantId, Long shopId, AgentTaskQueryParam param) {
        return agentTaskService.listTasks(tenantId, shopId, param);
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
}
