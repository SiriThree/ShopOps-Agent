package com.sirithree.shopops.admin.audit.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventQueryParam;
import com.sirithree.shopops.admin.agent.service.AgentTaskAdminService;
import com.sirithree.shopops.admin.audit.domain.AdminAuditOverviewDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineEventDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineQueryParam;
import com.sirithree.shopops.admin.audit.service.AdminAuditService;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventDto;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventQueryParam;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolCallLogQueryParam;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DefaultAdminAuditService implements AdminAuditService {
    private final AuthAuditService authAuditService;
    private final AgentTaskAdminService agentTaskAdminService;
    private final ToolCallLogService toolCallLogService;
    private final McpToolService mcpToolService;

    public DefaultAdminAuditService(AuthAuditService authAuditService,
                                    AgentTaskAdminService agentTaskAdminService,
                                    ToolCallLogService toolCallLogService,
                                    McpToolService mcpToolService) {
        this.authAuditService = authAuditService;
        this.agentTaskAdminService = agentTaskAdminService;
        this.toolCallLogService = toolCallLogService;
        this.mcpToolService = mcpToolService;
    }

    @Override
    public AdminAuditOverviewDto getOverview(Long tenantId, Long shopId) {
        AdminAuditOverviewDto overview = new AdminAuditOverviewDto();
        overview.setAuthEventTotal(authEventTotal(tenantId, shopId, null));
        overview.setAuthFailureTotal(authEventTotal(tenantId, shopId, "FAILURE"));
        overview.setTaskEventTotal(taskEventTotal(tenantId, shopId, null));
        overview.setTaskFailureTotal(taskEventTotal(tenantId, shopId, "TASK_FAILED"));
        overview.setToolCallTotal(toolCallTotal(tenantId, shopId, null));
        overview.setToolCallFailed(toolCallTotal(tenantId, shopId, "FAILED"));
        overview.setRecentAuthEvents(recentAuthEvents(tenantId, shopId).getList());
        overview.setRecentTaskEvents(recentTaskEvents(tenantId, shopId).getList());
        overview.setRecentToolCalls(recentToolCalls(tenantId, shopId).getList());
        overview.setGeneratedAt(LocalDateTime.now());
        return overview;
    }

    @Override
    public CommonPage<AdminAuditTimelineEventDto> listTimeline(Long tenantId, Long shopId, AdminAuditTimelineQueryParam param) {
        AdminAuditTimelineQueryParam query = param == null ? new AdminAuditTimelineQueryParam() : param;
        List<AdminAuditTimelineEventDto> events = new ArrayList<>();
        if (includesSource(query, "AUTH")) {
            events.addAll(authTimelineEvents(tenantId, shopId, query));
        }
        if (includesSource(query, "TASK")) {
            events.addAll(taskTimelineEvents(tenantId, shopId, query));
        }
        if (includesSource(query, "TOOL")) {
            events.addAll(toolTimelineEvents(tenantId, shopId, query));
        }
        List<AdminAuditTimelineEventDto> filtered = events.stream()
                .filter(event -> matches(query.getTraceId(), event.getTraceId()))
                .filter(event -> matches(query.getRiskLevel(), event.getRiskLevel()))
                .filter(event -> query.getCreatedStart() == null || event.getCreatedAt() == null || !event.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(event -> query.getCreatedEnd() == null || event.getCreatedAt() == null || !event.getCreatedAt().isAfter(query.getCreatedEnd()))
                .sorted(Comparator.comparing(AdminAuditTimelineEventDto::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(AdminAuditTimelineEventDto::getEventId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<AdminAuditTimelineEventDto> pageList = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(pageList, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    private long authEventTotal(Long tenantId, Long shopId, String eventStatus) {
        AuthAuditEventQueryParam query = new AuthAuditEventQueryParam();
        query.setEventStatus(eventStatus);
        query.setPageNum(1);
        query.setPageSize(1);
        return authAuditService.listEvents(tenantId, shopId, query).getTotal();
    }

    private long taskEventTotal(Long tenantId, Long shopId, String eventType) {
        AgentTaskEventQueryParam query = new AgentTaskEventQueryParam();
        query.setEventType(eventType);
        query.setPageNum(1);
        query.setPageSize(1);
        return agentTaskAdminService.listEvents(tenantId, shopId, query).getTotal();
    }

    private long toolCallTotal(Long tenantId, Long shopId, String status) {
        ToolCallLogQueryParam query = new ToolCallLogQueryParam();
        query.setStatus(status);
        query.setPageNum(1);
        query.setPageSize(1);
        return toolCallLogService.list(tenantId, shopId, query).getTotal();
    }

    private CommonPage<AuthAuditEventDto> recentAuthEvents(Long tenantId, Long shopId) {
        AuthAuditEventQueryParam query = new AuthAuditEventQueryParam();
        query.setPageNum(1);
        query.setPageSize(10);
        return authAuditService.listEvents(tenantId, shopId, query);
    }

    private CommonPage<AgentTaskEventDto> recentTaskEvents(Long tenantId, Long shopId) {
        AgentTaskEventQueryParam query = new AgentTaskEventQueryParam();
        query.setPageNum(1);
        query.setPageSize(10);
        return agentTaskAdminService.listEvents(tenantId, shopId, query);
    }

    private CommonPage<Map<String, Object>> recentToolCalls(Long tenantId, Long shopId) {
        ToolCallLogQueryParam query = new ToolCallLogQueryParam();
        query.setPageNum(1);
        query.setPageSize(10);
        return toolCallLogService.list(tenantId, shopId, query);
    }

    private List<AdminAuditTimelineEventDto> authTimelineEvents(Long tenantId, Long shopId, AdminAuditTimelineQueryParam query) {
        AuthAuditEventQueryParam authQuery = new AuthAuditEventQueryParam();
        authQuery.setEventType(query.getEventType());
        authQuery.setEventStatus(query.getEventStatus());
        authQuery.setUserId(query.getUserId());
        authQuery.setUsername(query.getUsername());
        authQuery.setCreatedStart(query.getCreatedStart());
        authQuery.setCreatedEnd(query.getCreatedEnd());
        authQuery.setPageNum(1);
        authQuery.setPageSize(100);
        return authAuditService.listEvents(tenantId, shopId, authQuery).getList().stream()
                .map(this::authTimelineEvent)
                .toList();
    }

    private List<AdminAuditTimelineEventDto> taskTimelineEvents(Long tenantId, Long shopId, AdminAuditTimelineQueryParam query) {
        AgentTaskEventQueryParam taskQuery = new AgentTaskEventQueryParam();
        taskQuery.setTaskId(query.getTaskId());
        taskQuery.setEventType(query.getEventType());
        taskQuery.setOperatorId(query.getUserId());
        taskQuery.setCreatedStart(query.getCreatedStart());
        taskQuery.setCreatedEnd(query.getCreatedEnd());
        taskQuery.setPageNum(1);
        taskQuery.setPageSize(100);
        return agentTaskAdminService.listEvents(tenantId, shopId, taskQuery).getList().stream()
                .map(this::taskTimelineEvent)
                .filter(event -> matches(query.getEventStatus(), event.getEventStatus()))
                .toList();
    }

    private List<AdminAuditTimelineEventDto> toolTimelineEvents(Long tenantId, Long shopId, AdminAuditTimelineQueryParam query) {
        ToolCallLogQueryParam toolQuery = new ToolCallLogQueryParam();
        toolQuery.setTaskId(query.getTaskId());
        toolQuery.setStatus(query.getEventStatus());
        toolQuery.setToolCode(query.getToolCode());
        toolQuery.setPageNum(1);
        toolQuery.setPageSize(100);
        return toolCallLogService.list(tenantId, shopId, toolQuery).getList().stream()
                .map(source -> toolTimelineEvent(tenantId, source))
                .filter(event -> matches(query.getEventType(), event.getEventType()))
                .filter(event -> query.getUserId() == null || query.getUserId().equals(event.getUserId()))
                .filter(event -> matches(query.getTraceId(), event.getTraceId()))
                .filter(event -> matches(query.getRiskLevel(), event.getRiskLevel()))
                .filter(event -> query.getCreatedStart() == null || event.getCreatedAt() == null || !event.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(event -> query.getCreatedEnd() == null || event.getCreatedAt() == null || !event.getCreatedAt().isAfter(query.getCreatedEnd()))
                .toList();
    }

    private AdminAuditTimelineEventDto authTimelineEvent(AuthAuditEventDto source) {
        AdminAuditTimelineEventDto event = new AdminAuditTimelineEventDto();
        event.setSource("AUTH");
        event.setEventId("auth:" + source.getEventId());
        event.setEventType(source.getEventType());
        event.setEventStatus(source.getEventStatus());
        event.setUserId(source.getUserId());
        event.setUsername(source.getUsername());
        event.setRequestId(source.getRequestId());
        event.setResourceType("auth_audit_event");
        event.setResourceId(String.valueOf(source.getEventId()));
        event.setRiskLevel(authRiskLevel(source));
        event.setSummary(source.getEventType() + " " + source.getEventStatus());
        event.setCreatedAt(source.getCreatedAt());
        Map<String, Object> detail = new LinkedHashMap<>();
        putIfPresent(detail, "authType", source.getAuthType());
        putIfPresent(detail, "clientIp", source.getClientIp());
        putIfPresent(detail, "userAgent", source.getUserAgent());
        putIfPresent(detail, "failureReason", source.getFailureReason());
        event.setDetail(detail);
        return event;
    }

    private AdminAuditTimelineEventDto taskTimelineEvent(AgentTaskEventDto source) {
        AdminAuditTimelineEventDto event = new AdminAuditTimelineEventDto();
        event.setSource("TASK");
        event.setEventId("task:" + source.getEventId());
        event.setEventType(source.getEventType());
        event.setEventStatus(taskEventStatus(source));
        event.setUserId(source.getOperatorId());
        event.setTaskId(source.getTaskId());
        event.setTraceId(taskEventTraceId(source));
        event.setResourceType("agent_task");
        event.setResourceId(String.valueOf(source.getTaskId()));
        event.setRiskLevel(taskRiskLevel(source));
        event.setSummary(source.getEventType());
        event.setCreatedAt(source.getCreatedAt());
        Map<String, Object> detail = new LinkedHashMap<>();
        putIfPresent(detail, "fromStatus", source.getFromStatus());
        putIfPresent(detail, "toStatus", source.getToStatus());
        putIfPresent(detail, "eventData", source.getEventData());
        event.setDetail(detail);
        return event;
    }

    private AdminAuditTimelineEventDto toolTimelineEvent(Long tenantId, Map<String, Object> source) {
        AdminAuditTimelineEventDto event = new AdminAuditTimelineEventDto();
        event.setSource("TOOL");
        event.setEventId("tool:" + source.get("id"));
        event.setEventType("TOOL_CALL");
        event.setEventStatus(stringValue(source.get("status")));
        event.setUserId(longValue(source.get("userId")));
        event.setTaskId(longValue(source.get("taskId")));
        event.setTraceId(stringValue(source.get("traceId")));
        event.setToolCode(stringValue(source.get("toolCode")));
        event.setResourceType("tool_call_log");
        event.setResourceId(stringValue(source.get("id")));
        event.setRiskLevel(toolRiskLevel(tenantId, event.getToolCode(), stringValue(source.get("riskLevel"))));
        event.setSummary("TOOL_CALL " + stringValue(source.get("toolCode")) + " " + stringValue(source.get("status")));
        event.setCreatedAt(localDateTime(source.get("createdAt")));
        Map<String, Object> detail = new LinkedHashMap<>();
        putIfPresent(detail, "stepId", source.get("stepId"));
        putIfPresent(detail, "spanId", source.get("spanId"));
        putIfPresent(detail, "latencyMs", source.get("latencyMs"));
        putIfPresent(detail, "retryCount", source.get("retryCount"));
        putIfPresent(detail, "riskLevel", event.getRiskLevel());
        putIfPresent(detail, "errorCode", source.get("errorCode"));
        putIfPresent(detail, "errorMessage", source.get("errorMessage"));
        event.setDetail(detail);
        return event;
    }

    private boolean includesSource(AdminAuditTimelineQueryParam query, String source) {
        return query.getSource() == null || query.getSource().isBlank() || source.equalsIgnoreCase(query.getSource());
    }

    private String taskEventStatus(AgentTaskEventDto event) {
        if ("TASK_FAILED".equals(event.getEventType())) {
            return "FAILURE";
        }
        return "SUCCESS";
    }

    private String authRiskLevel(AuthAuditEventDto event) {
        if ("FAILURE".equals(event.getEventStatus())) {
            return "HIGH";
        }
        if ("ACCESS_DENIED".equals(event.getEventType()) || "AUTHENTICATION".equals(event.getEventType())) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String taskRiskLevel(AgentTaskEventDto event) {
        if ("TASK_FAILED".equals(event.getEventType())) {
            return "HIGH";
        }
        if ("TASK_RETRY_REQUESTED".equals(event.getEventType()) || "TASK_REQUEUED".equals(event.getEventType())) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String toolRiskLevel(Long tenantId, String toolCode, String logRiskLevel) {
        if (logRiskLevel != null && !logRiskLevel.isBlank()) {
            return logRiskLevel;
        }
        if (toolCode == null || toolCode.isBlank()) {
            return "MEDIUM";
        }
        McpToolDto tool = mcpToolService.getTool(tenantId, toolCode);
        return tool == null || tool.getRiskLevel() == null || tool.getRiskLevel().isBlank() ? "MEDIUM" : tool.getRiskLevel();
    }

    @SuppressWarnings("unchecked")
    private String taskEventTraceId(AgentTaskEventDto event) {
        if (event.getEventData() instanceof Map<?, ?> map) {
            Object value = ((Map<String, Object>) map).get("traceId");
            return stringValue(value);
        }
        return null;
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.valueOf(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.toString());
    }

    private void putIfPresent(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }
}
