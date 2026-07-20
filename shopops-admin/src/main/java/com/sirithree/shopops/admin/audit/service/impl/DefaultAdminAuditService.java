package com.sirithree.shopops.admin.audit.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventQueryParam;
import com.sirithree.shopops.admin.agent.service.AgentTaskAdminService;
import com.sirithree.shopops.admin.audit.domain.AdminAuditExportDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditOverviewDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditRiskSummaryDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineDetailDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineEventDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineQueryParam;
import com.sirithree.shopops.admin.audit.service.AdminAuditService;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.approval.domain.ApprovalRequestQueryParam;
import com.sirithree.shopops.admin.approval.domain.ApprovalStatus;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
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
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DefaultAdminAuditService implements AdminAuditService {
    private static final List<String> EXPORT_COLUMNS = List.of(
            "createdAt",
            "source",
            "eventType",
            "eventStatus",
            "riskLevel",
            "userId",
            "username",
            "taskId",
            "traceId",
            "toolCode",
            "requestId",
            "resourceType",
            "resourceId",
            "summary"
    );

    private final AuthAuditService authAuditService;
    private final AgentTaskAdminService agentTaskAdminService;
    private final ToolCallLogService toolCallLogService;
    private final McpToolService mcpToolService;
    private final ApprovalRequestService approvalRequestService;
    private final ObjectProvider<AdminAuditTimelineJdbcRepository> jdbcTimelineRepositoryProvider;

    public DefaultAdminAuditService(AuthAuditService authAuditService,
                                    AgentTaskAdminService agentTaskAdminService,
                                    ToolCallLogService toolCallLogService,
                                    McpToolService mcpToolService,
                                    ApprovalRequestService approvalRequestService,
                                    ObjectProvider<AdminAuditTimelineJdbcRepository> jdbcTimelineRepositoryProvider) {
        this.authAuditService = authAuditService;
        this.agentTaskAdminService = agentTaskAdminService;
        this.toolCallLogService = toolCallLogService;
        this.mcpToolService = mcpToolService;
        this.approvalRequestService = approvalRequestService;
        this.jdbcTimelineRepositoryProvider = jdbcTimelineRepositoryProvider;
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
        AdminAuditTimelineJdbcRepository jdbcTimelineRepository = jdbcTimelineRepositoryProvider.getIfAvailable();
        if (jdbcTimelineRepository != null) {
            return jdbcTimelineRepository.listTimeline(tenantId, shopId, param);
        }
        return memoryTimeline(tenantId, shopId, param);
    }

    private CommonPage<AdminAuditTimelineEventDto> memoryTimeline(Long tenantId, Long shopId, AdminAuditTimelineQueryParam param) {
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
        if (includesSource(query, "APPROVAL")) {
            events.addAll(approvalTimelineEvents(tenantId, shopId, query));
        }
        List<AdminAuditTimelineEventDto> filtered = events.stream()
                .filter(event -> matches(query.getTraceId(), event.getTraceId()))
                .filter(event -> matchesRiskLevel(query.getRiskLevel(), event.getRiskLevel()))
                .filter(event -> !Boolean.TRUE.equals(query.getElevatedRisk()) || isElevatedRisk(event.getRiskLevel()))
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

    @Override
    public Optional<AdminAuditTimelineDetailDto> getTimelineDetail(Long tenantId, Long shopId, String source, String resourceId) {
        if (source == null || resourceId == null || resourceId.isBlank()) {
            return Optional.empty();
        }
        return switch (source.toUpperCase()) {
            case "AUTH" -> authTimelineDetail(tenantId, shopId, resourceId);
            case "TASK" -> taskTimelineDetail(tenantId, shopId, resourceId);
            case "TOOL" -> toolTimelineDetail(tenantId, shopId, resourceId);
            case "APPROVAL" -> approvalTimelineDetail(tenantId, shopId, resourceId);
            default -> Optional.empty();
        };
    }

    @Override
    public AdminAuditRiskSummaryDto getRiskSummary(Long tenantId, Long shopId) {
        CommonPage<AdminAuditTimelineEventDto> totalPage = timelineCount(tenantId, shopId, new AdminAuditTimelineQueryParam());
        Map<String, Long> riskBreakdown = riskBreakdown(tenantId, shopId);
        AdminAuditTimelineQueryParam elevatedRiskQuery = new AdminAuditTimelineQueryParam();
        elevatedRiskQuery.setElevatedRisk(true);
        elevatedRiskQuery.setPageNum(1);
        elevatedRiskQuery.setPageSize(10);
        CommonPage<AdminAuditTimelineEventDto> elevatedRiskPage = listTimeline(tenantId, shopId, elevatedRiskQuery);
        AdminAuditRiskSummaryDto summary = new AdminAuditRiskSummaryDto();
        summary.setTotal(totalPage.getTotal());
        summary.setElevatedRiskTotal(elevatedRiskPage.getTotal());
        summary.setRiskBreakdown(riskBreakdown);
        summary.setRecentElevatedRiskEvents(elevatedRiskPage.getList());
        summary.setGeneratedAt(LocalDateTime.now());
        return summary;
    }

    private CommonPage<AdminAuditTimelineEventDto> timelineCount(Long tenantId, Long shopId, AdminAuditTimelineQueryParam query) {
        query.setPageNum(1);
        query.setPageSize(1);
        return listTimeline(tenantId, shopId, query);
    }

    private Map<String, Long> riskBreakdown(Long tenantId, Long shopId) {
        Map<String, Long> riskBreakdown = new LinkedHashMap<>();
        for (String riskLevel : List.of("HIGH", "MEDIUM", "LOW", "UNKNOWN")) {
            AdminAuditTimelineQueryParam riskQuery = new AdminAuditTimelineQueryParam();
            riskQuery.setRiskLevel(riskLevel);
            riskBreakdown.put(riskLevel, timelineCount(tenantId, shopId, riskQuery).getTotal());
        }
        return riskBreakdown;
    }

    @Override
    public AdminAuditExportDto exportTimeline(Long tenantId, Long shopId, AdminAuditTimelineQueryParam param) {
        AdminAuditTimelineQueryParam query = param == null ? new AdminAuditTimelineQueryParam() : param;
        query.setPageNum(1);
        query.setPageSize(100);
        List<Map<String, Object>> rows = listTimeline(tenantId, shopId, query).getList().stream()
                .map(this::exportRow)
                .toList();
        AdminAuditExportDto export = new AdminAuditExportDto();
        export.setFileName("shopops-audit-" + LocalDateTime.now().toLocalDate() + ".csv");
        export.setContentType("text/csv");
        export.setColumns(EXPORT_COLUMNS);
        export.setRows(rows);
        export.setRowCount(rows.size());
        export.setGeneratedAt(LocalDateTime.now());
        return export;
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

    private Optional<AdminAuditTimelineDetailDto> authTimelineDetail(Long tenantId, Long shopId, String resourceId) {
        Long eventId = parseLong(resourceId);
        if (eventId == null) {
            return Optional.empty();
        }
        AuthAuditEventQueryParam query = new AuthAuditEventQueryParam();
        query.setEventId(eventId);
        query.setPageNum(1);
        query.setPageSize(1);
        return authAuditService.listEvents(tenantId, shopId, query).getList().stream()
                .findFirst()
                .map(event -> detail(authTimelineEvent(event), Map.of("authAuditEvent", event), Map.of()));
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

    private Optional<AdminAuditTimelineDetailDto> taskTimelineDetail(Long tenantId, Long shopId, String resourceId) {
        Long taskId = parseLong(resourceId);
        if (taskId == null) {
            return Optional.empty();
        }
        return agentTaskAdminService.getTaskDetail(tenantId, shopId, taskId)
                .map(taskDetail -> {
                    AgentTaskEventDto latestEvent = taskDetail.getEvents().stream()
                            .max(Comparator.comparing(AgentTaskEventDto::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(null);
                    AdminAuditTimelineEventDto event = latestEvent == null ? taskResourceEvent(taskId) : taskTimelineEvent(latestEvent);
                    Map<String, Object> resource = new LinkedHashMap<>();
                    resource.put("taskDetail", taskDetail);
                    Map<String, Object> context = new LinkedHashMap<>();
                    context.put("traceId", taskDetail.getTask().getTraceId());
                    context.put("reportId", taskDetail.getTask().getReportId());
                    context.put("toolCallCount", taskDetail.getToolCalls().size());
                    context.put("spanCount", taskDetail.getSpans().size());
                    return detail(event, resource, context);
                });
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
                .filter(event -> matchesRiskLevel(query.getRiskLevel(), event.getRiskLevel()))
                .filter(event -> query.getCreatedStart() == null || event.getCreatedAt() == null || !event.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(event -> query.getCreatedEnd() == null || event.getCreatedAt() == null || !event.getCreatedAt().isAfter(query.getCreatedEnd()))
                .toList();
    }

    private Optional<AdminAuditTimelineDetailDto> toolTimelineDetail(Long tenantId, Long shopId, String resourceId) {
        Long logId = parseLong(resourceId);
        if (logId == null) {
            return Optional.empty();
        }
        ToolCallLogQueryParam query = new ToolCallLogQueryParam();
        query.setLogId(logId);
        query.setPageNum(1);
        query.setPageSize(1);
        return toolCallLogService.list(tenantId, shopId, query).getList().stream()
                .findFirst()
                .map(log -> {
                    AdminAuditTimelineEventDto event = toolTimelineEvent(tenantId, log);
                    Map<String, Object> resource = new LinkedHashMap<>();
                    resource.put("toolCallLog", log);
                    Map<String, Object> context = new LinkedHashMap<>();
                    putIfPresent(context, "tool", mcpToolService.getTool(tenantId, event.getToolCode()));
                    Long taskId = event.getTaskId();
                    if (taskId != null) {
                        agentTaskAdminService.getTaskDetail(tenantId, shopId, taskId)
                                .ifPresent(taskDetail -> context.put("taskDetail", taskDetail));
                    }
                    return detail(event, resource, context);
                });
    }

    private List<AdminAuditTimelineEventDto> approvalTimelineEvents(Long tenantId, Long shopId, AdminAuditTimelineQueryParam query) {
        ApprovalRequestQueryParam approvalQuery = new ApprovalRequestQueryParam();
        approvalQuery.setTaskId(query.getTaskId());
        approvalQuery.setTraceId(query.getTraceId());
        approvalQuery.setToolCode(query.getToolCode());
        approvalQuery.setRiskLevel(query.getRiskLevel());
        approvalQuery.setCreatedStart(query.getCreatedStart());
        approvalQuery.setCreatedEnd(query.getCreatedEnd());
        approvalQuery.setPageNum(1);
        approvalQuery.setPageSize(100);
        List<AdminAuditTimelineEventDto> events = new ArrayList<>();
        for (ApprovalRequestDto approval : approvalRequestService.list(tenantId, shopId, approvalQuery).getList()) {
            events.add(approvalTimelineEvent(approval, false));
            if (!ApprovalStatus.PENDING.equals(approval.getStatus()) && approval.getDecidedAt() != null) {
                events.add(approvalTimelineEvent(approval, true));
            }
        }
        return events.stream()
                .filter(event -> matches(query.getEventType(), event.getEventType()))
                .filter(event -> matches(query.getEventStatus(), event.getEventStatus()))
                .filter(event -> query.getUserId() == null || query.getUserId().equals(event.getUserId()))
                .filter(event -> query.getCreatedStart() == null || event.getCreatedAt() == null || !event.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(event -> query.getCreatedEnd() == null || event.getCreatedAt() == null || !event.getCreatedAt().isAfter(query.getCreatedEnd()))
                .toList();
    }

    private Optional<AdminAuditTimelineDetailDto> approvalTimelineDetail(Long tenantId, Long shopId, String resourceId) {
        Long approvalId = parseLong(resourceId);
        if (approvalId == null) {
            return Optional.empty();
        }
        return approvalRequestService.get(tenantId, shopId, approvalId)
                .map(approval -> {
                    AdminAuditTimelineEventDto event = approvalTimelineEvent(approval, !ApprovalStatus.PENDING.equals(approval.getStatus()));
                    Map<String, Object> resource = new LinkedHashMap<>();
                    resource.put("approvalRequest", approval);
                    Map<String, Object> context = new LinkedHashMap<>();
                    putIfPresent(context, "tool", mcpToolService.getTool(tenantId, approval.getToolCode()));
                    putIfPresent(context, "approvalStatus", approval.getStatus());
                    return detail(event, resource, context);
                });
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

    private AdminAuditTimelineEventDto approvalTimelineEvent(ApprovalRequestDto source, boolean decisionEvent) {
        AdminAuditTimelineEventDto event = new AdminAuditTimelineEventDto();
        event.setSource("APPROVAL");
        event.setEventId("approval:" + source.getApprovalId() + ":" + (decisionEvent ? "decision" : "created"));
        event.setEventType(approvalEventType(source, decisionEvent));
        event.setEventStatus(approvalEventStatus(source, decisionEvent));
        event.setUserId(decisionEvent ? source.getApproverId() : source.getRequesterId());
        event.setUsername(decisionEvent ? source.getApproverName() : source.getRequesterName());
        event.setTaskId(source.getTaskId());
        event.setTraceId(source.getTraceId());
        event.setToolCode(source.getToolCode());
        event.setResourceType("approval_request");
        event.setResourceId(String.valueOf(source.getApprovalId()));
        event.setRiskLevel(normalizedRiskLevel(source.getRiskLevel()));
        event.setSummary(event.getEventType() + " " + source.getStatus());
        event.setCreatedAt(decisionEvent ? source.getDecidedAt() : source.getCreatedAt());
        Map<String, Object> detail = new LinkedHashMap<>();
        putIfPresent(detail, "approvalNo", source.getApprovalNo());
        putIfPresent(detail, "sourceType", source.getSourceType());
        putIfPresent(detail, "sourceId", source.getSourceId());
        putIfPresent(detail, "title", source.getTitle());
        putIfPresent(detail, "status", source.getStatus());
        putIfPresent(detail, "decisionComment", source.getDecisionComment());
        event.setDetail(detail);
        return event;
    }

    private AdminAuditTimelineEventDto taskResourceEvent(Long taskId) {
        AdminAuditTimelineEventDto event = new AdminAuditTimelineEventDto();
        event.setSource("TASK");
        event.setEventId("task:" + taskId);
        event.setEventType("TASK_DETAIL");
        event.setEventStatus("SUCCESS");
        event.setTaskId(taskId);
        event.setResourceType("agent_task");
        event.setResourceId(String.valueOf(taskId));
        event.setRiskLevel("LOW");
        event.setSummary("TASK_DETAIL");
        return event;
    }

    private AdminAuditTimelineDetailDto detail(AdminAuditTimelineEventDto event,
                                               Map<String, Object> resource,
                                               Map<String, Object> context) {
        AdminAuditTimelineDetailDto detail = new AdminAuditTimelineDetailDto();
        detail.setEvent(event);
        detail.setResource(resource);
        detail.setContext(context);
        return detail;
    }

    private Map<String, Object> exportRow(AdminAuditTimelineEventDto event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("createdAt", event.getCreatedAt());
        row.put("source", event.getSource());
        row.put("eventType", event.getEventType());
        row.put("eventStatus", event.getEventStatus());
        row.put("riskLevel", event.getRiskLevel());
        row.put("userId", event.getUserId());
        row.put("username", event.getUsername());
        row.put("taskId", event.getTaskId());
        row.put("traceId", event.getTraceId());
        row.put("toolCode", event.getToolCode());
        row.put("requestId", event.getRequestId());
        row.put("resourceType", event.getResourceType());
        row.put("resourceId", event.getResourceId());
        row.put("summary", event.getSummary());
        return row;
    }

    private boolean includesSource(AdminAuditTimelineQueryParam query, String source) {
        return query.getSource() == null || query.getSource().isBlank() || source.equalsIgnoreCase(query.getSource());
    }

    private String normalizedRiskLevel(String riskLevel) {
        return riskLevel == null || riskLevel.isBlank() ? "UNKNOWN" : riskLevel.toUpperCase();
    }

    private boolean isElevatedRisk(String riskLevel) {
        String normalized = normalizedRiskLevel(riskLevel);
        return !"LOW".equals(normalized) && !"UNKNOWN".equals(normalized);
    }

    private boolean matchesRiskLevel(String expected, String actual) {
        return expected == null || expected.isBlank() || normalizedRiskLevel(expected).equals(normalizedRiskLevel(actual));
    }

    private String taskEventStatus(AgentTaskEventDto event) {
        if ("TASK_FAILED".equals(event.getEventType())) {
            return "FAILURE";
        }
        return "SUCCESS";
    }

    private String approvalEventStatus(ApprovalRequestDto event, boolean decisionEvent) {
        if (!decisionEvent) {
            return "PENDING";
        }
        if (ApprovalStatus.REJECTED.equals(event.getStatus())) {
            return "FAILURE";
        }
        if (ApprovalStatus.WITHDRAWN.equals(event.getStatus())) {
            return "CANCELED";
        }
        return "SUCCESS";
    }

    private String approvalEventType(ApprovalRequestDto event, boolean decisionEvent) {
        if (!decisionEvent) {
            return "APPROVAL_CREATED";
        }
        if (ApprovalStatus.WITHDRAWN.equals(event.getStatus())) {
            return "APPROVAL_WITHDRAWN";
        }
        return "APPROVAL_DECIDED";
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

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
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
