package com.sirithree.shopops.admin.audit.service.impl;

import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineEventDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineQueryParam;
import com.sirithree.shopops.common.api.CommonPage;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class AdminAuditTimelineJdbcRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminAuditTimelineJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CommonPage<AdminAuditTimelineEventDto> listTimeline(Long tenantId, Long shopId, AdminAuditTimelineQueryParam param) {
        AdminAuditTimelineQueryParam query = param == null ? new AdminAuditTimelineQueryParam() : param;
        MapSqlParameterSource parameters = parameters(tenantId, shopId, query);
        List<AdminAuditTimelineEventDto> list = jdbcTemplate.query(
                timelineSql(query),
                parameters,
                (rs, rowNum) -> {
                    AdminAuditTimelineEventDto event = new AdminAuditTimelineEventDto();
                    event.setSource(rs.getString("source"));
                    event.setEventId(rs.getString("event_id"));
                    event.setEventType(rs.getString("event_type"));
                    event.setEventStatus(rs.getString("event_status"));
                    event.setUserId(longValue(rs.getObject("user_id")));
                    event.setUsername(rs.getString("username"));
                    event.setTaskId(longValue(rs.getObject("task_id")));
                    event.setTraceId(rs.getString("trace_id"));
                    event.setToolCode(rs.getString("tool_code"));
                    event.setRequestId(rs.getString("request_id"));
                    event.setResourceType(rs.getString("resource_type"));
                    event.setResourceId(rs.getString("resource_id"));
                    event.setRiskLevel(rs.getString("risk_level"));
                    event.setSummary(rs.getString("summary"));
                    event.setCreatedAt(localDateTime(rs.getObject("created_at")));
                    event.setDetail(detail(rs.getString("source"), rs.getString("detail_a"), rs.getString("detail_b"),
                            rs.getString("detail_c"), rs.getString("detail_d")));
                    return event;
                }
        );
        Long total = jdbcTemplate.queryForObject(countSql(query), parameters, Long.class);
        return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total == null ? 0L : total);
    }

    private String timelineSql(AdminAuditTimelineQueryParam query) {
        return """
                SELECT *
                FROM (
                """ + unionSql() + """
                ) audit_events
                """ + whereSql(query) + """
                ORDER BY created_at DESC, sortable_id DESC
                LIMIT :limit OFFSET :offset
                """;
    }

    private String countSql(AdminAuditTimelineQueryParam query) {
        return """
                SELECT COUNT(*)
                FROM (
                """ + unionSql() + """
                ) audit_events
                """ + whereSql(query);
    }

    private String unionSql() {
        return """
                    SELECT
                      CONVERT('AUTH' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS source,
                      CONVERT(CONCAT('auth:', id) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_id,
                      id AS sortable_id,
                      CONVERT(event_type USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_type,
                      CONVERT(event_status USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_status,
                      user_id,
                      CONVERT(username USING utf8mb4) COLLATE utf8mb4_unicode_ci AS username,
                      CAST(NULL AS SIGNED) AS task_id,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS trace_id,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS tool_code,
                      CONVERT(request_id USING utf8mb4) COLLATE utf8mb4_unicode_ci AS request_id,
                      CONVERT('auth_audit_event' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_type,
                      CONVERT(CAST(id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_id,
                      CASE
                        WHEN event_status = 'FAILURE' THEN 'HIGH'
                        WHEN event_type IN ('ACCESS_DENIED', 'AUTHENTICATION') THEN 'MEDIUM'
                        ELSE 'LOW'
                      END COLLATE utf8mb4_unicode_ci AS risk_level,
                      CONVERT(CASE
                        WHEN event_type LIKE 'ORG!_%' ESCAPE '!' AND failure_reason IS NOT NULL AND failure_reason <> '' THEN failure_reason
                        ELSE CONCAT(event_type, ' ', event_status)
                      END USING utf8mb4) COLLATE utf8mb4_unicode_ci AS summary,
                      created_at,
                      CONVERT(auth_type USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_a,
                      CONVERT(client_ip USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_b,
                      CONVERT(failure_reason USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_c,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS detail_d
                    FROM auth_audit_event
                    WHERE tenant_id = :tenantId AND shop_id = :shopId
                  UNION ALL
                    SELECT
                      CONVERT('TASK' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS source,
                      CONVERT(CONCAT('task:', id) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_id,
                      id AS sortable_id,
                      CONVERT(event_type USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_type,
                      CASE WHEN event_type = 'TASK_FAILED' THEN 'FAILURE' ELSE 'SUCCESS' END COLLATE utf8mb4_unicode_ci AS event_status,
                      operator_id AS user_id,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS username,
                      task_id,
                      CONVERT(CASE
                        WHEN JSON_VALID(event_data_json) THEN JSON_UNQUOTE(JSON_EXTRACT(event_data_json, '$.traceId'))
                        ELSE NULL
                      END USING utf8mb4) COLLATE utf8mb4_unicode_ci AS trace_id,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS tool_code,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS request_id,
                      CONVERT('agent_task' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_type,
                      CONVERT(CAST(task_id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_id,
                      CASE
                        WHEN event_type = 'TASK_FAILED' THEN 'HIGH'
                        WHEN event_type IN ('TASK_RETRY_REQUESTED', 'TASK_REQUEUED') THEN 'MEDIUM'
                        ELSE 'LOW'
                      END COLLATE utf8mb4_unicode_ci AS risk_level,
                      CONVERT(event_type USING utf8mb4) COLLATE utf8mb4_unicode_ci AS summary,
                      created_at,
                      CONVERT(from_status USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_a,
                      CONVERT(to_status USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_b,
                      CONVERT(event_data_json USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_c,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS detail_d
                    FROM agent_task_event
                    WHERE tenant_id = :tenantId AND shop_id = :shopId
                  UNION ALL
                    SELECT
                      CONVERT('TOOL' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS source,
                      CONVERT(CONCAT('tool:', tcl.id) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_id,
                      tcl.id AS sortable_id,
                      CONVERT('TOOL_CALL' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_type,
                      CONVERT(tcl.status USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_status,
                      tcl.user_id,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS username,
                      tcl.task_id,
                      CONVERT(tcl.trace_id USING utf8mb4) COLLATE utf8mb4_unicode_ci AS trace_id,
                      CONVERT(tcl.tool_code USING utf8mb4) COLLATE utf8mb4_unicode_ci AS tool_code,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS request_id,
                      CONVERT('tool_call_log' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_type,
                      CONVERT(CAST(tcl.id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_id,
                      CONVERT(COALESCE(
                        tcl.risk_level,
                        (
                          SELECT mt.risk_level
                          FROM mcp_tool mt
                          WHERE mt.tool_code = tcl.tool_code
                            AND mt.enabled = 1
                            AND (mt.tenant_id = tcl.tenant_id OR mt.tenant_id IS NULL)
                          ORDER BY mt.tenant_id DESC, mt.version DESC
                          LIMIT 1
                        ),
                        'MEDIUM'
                      ) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS risk_level,
                      CONVERT(CONCAT('TOOL_CALL ', tcl.tool_code, ' ', tcl.status) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS summary,
                      tcl.created_at,
                      CONVERT(CAST(tcl.step_id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_a,
                      CONVERT(CAST(tcl.latency_ms AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_b,
                      CONVERT(tcl.error_code USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_c,
                      CONVERT(tcl.error_message USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_d
                    FROM tool_call_log tcl
                    WHERE tcl.tenant_id = :tenantId AND tcl.shop_id = :shopId
                  UNION ALL
                    SELECT
                      CONVERT('APPROVAL' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS source,
                      CONVERT(CONCAT('approval:', ar.id, ':created') USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_id,
                      ar.id * 10 AS sortable_id,
                      CONVERT('APPROVAL_CREATED' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_type,
                      CONVERT('PENDING' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_status,
                      ar.requester_id AS user_id,
                      CONVERT(ar.requester_name USING utf8mb4) COLLATE utf8mb4_unicode_ci AS username,
                      ar.task_id,
                      CONVERT(ar.trace_id USING utf8mb4) COLLATE utf8mb4_unicode_ci AS trace_id,
                      CONVERT(ar.tool_code USING utf8mb4) COLLATE utf8mb4_unicode_ci AS tool_code,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS request_id,
                      CONVERT('approval_request' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_type,
                      CONVERT(CAST(ar.id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_id,
                      CONVERT(UPPER(ar.risk_level) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS risk_level,
                      CONVERT(CONCAT('APPROVAL_CREATED ', ar.status) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS summary,
                      ar.created_at,
                      CONVERT(ar.approval_no USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_a,
                      CONVERT(ar.source_type USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_b,
                      CONVERT(ar.title USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_c,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS detail_d
                    FROM approval_request ar
                    WHERE ar.tenant_id = :tenantId AND ar.shop_id = :shopId
                  UNION ALL
                    SELECT
                      CONVERT('APPROVAL' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS source,
                      CONVERT(CONCAT('approval:', ar.id, ':decision') USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_id,
                      ar.id * 10 + 1 AS sortable_id,
                      CASE
                        WHEN ar.status = 'WITHDRAWN' THEN 'APPROVAL_WITHDRAWN'
                        WHEN ar.status = 'EXPIRED' THEN 'APPROVAL_EXPIRED'
                        ELSE 'APPROVAL_DECIDED'
                      END COLLATE utf8mb4_unicode_ci AS event_type,
                      CASE
                        WHEN ar.status = 'REJECTED' THEN 'FAILURE'
                        WHEN ar.status = 'WITHDRAWN' THEN 'CANCELED'
                        WHEN ar.status = 'EXPIRED' THEN 'CANCELED'
                        ELSE 'SUCCESS'
                      END COLLATE utf8mb4_unicode_ci AS event_status,
                      ar.approver_id AS user_id,
                      CONVERT(ar.approver_name USING utf8mb4) COLLATE utf8mb4_unicode_ci AS username,
                      ar.task_id,
                      CONVERT(ar.trace_id USING utf8mb4) COLLATE utf8mb4_unicode_ci AS trace_id,
                      CONVERT(ar.tool_code USING utf8mb4) COLLATE utf8mb4_unicode_ci AS tool_code,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS request_id,
                      CONVERT('approval_request' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_type,
                      CONVERT(CAST(ar.id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_id,
                      CONVERT(UPPER(ar.risk_level) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS risk_level,
                      CONVERT(CONCAT(CASE
                        WHEN ar.status = 'WITHDRAWN' THEN 'APPROVAL_WITHDRAWN'
                        WHEN ar.status = 'EXPIRED' THEN 'APPROVAL_EXPIRED'
                        ELSE 'APPROVAL_DECIDED'
                      END, ' ', ar.status) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS summary,
                      ar.decided_at AS created_at,
                      CONVERT(ar.approval_no USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_a,
                      CONVERT(ar.status USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_b,
                      CONVERT(ar.decision_comment USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_c,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS detail_d
                    FROM approval_request ar
                    WHERE ar.tenant_id = :tenantId AND ar.shop_id = :shopId
                      AND ar.status <> 'PENDING'
                      AND ar.decided_at IS NOT NULL
                  UNION ALL
                    SELECT
                      CONVERT('CONNECTOR' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS source,
                      CONVERT(CONCAT('connector:', cae.id) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_id,
                      cae.id AS sortable_id,
                      CONVERT(cae.event_type USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_type,
                      CONVERT(cae.event_status USING utf8mb4) COLLATE utf8mb4_unicode_ci AS event_status,
                      cae.user_id,
                      CONVERT(cae.username USING utf8mb4) COLLATE utf8mb4_unicode_ci AS username,
                      CAST(NULL AS SIGNED) AS task_id,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS trace_id,
                      CONVERT(cae.connector_code USING utf8mb4) COLLATE utf8mb4_unicode_ci AS tool_code,
                      CONVERT(cae.request_id USING utf8mb4) COLLATE utf8mb4_unicode_ci AS request_id,
                      CONVERT('connector_audit_event' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_type,
                      CONVERT(CAST(cae.id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS resource_id,
                      CASE
                        WHEN cae.event_status = 'FAILURE' THEN 'MEDIUM'
                        WHEN cae.event_type = 'CONNECTOR_CREDENTIAL_DISABLED' THEN 'MEDIUM'
                        ELSE 'LOW'
                      END COLLATE utf8mb4_unicode_ci AS risk_level,
                      CONVERT(CONCAT(cae.event_type, ' ', cae.connector_code, ' ', cae.event_status) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS summary,
                      cae.created_at,
                      CONVERT(cae.connector_code USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_a,
                      CONVERT(cae.message USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_b,
                      CONVERT(cae.detail_json USING utf8mb4) COLLATE utf8mb4_unicode_ci AS detail_c,
                      CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS detail_d
                    FROM connector_audit_event cae
                    WHERE cae.tenant_id = :tenantId AND cae.shop_id = :shopId
                """;
    }

    private String whereSql(AdminAuditTimelineQueryParam query) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1\n");
        appendStringFilter(where, query.getSource(), "source");
        appendStringFilter(where, query.getEventType(), "event_type");
        appendStringFilter(where, query.getEventStatus(), "event_status");
        appendLongFilter(where, query.getUserId(), "user_id");
        appendStringFilter(where, query.getUsername(), "username");
        appendLongFilter(where, query.getTaskId(), "task_id");
        appendStringFilter(where, query.getTraceId(), "trace_id");
        appendStringFilter(where, query.getToolCode(), "tool_code");
        appendRiskLevelFilter(where, query.getRiskLevel());
        if (Boolean.TRUE.equals(query.getElevatedRisk())) {
            where.append("  AND UPPER(risk_level) NOT IN ('LOW', 'UNKNOWN')\n");
        }
        if (query.getCreatedStart() != null) {
            where.append("  AND created_at >= :createdStart\n");
        }
        if (query.getCreatedEnd() != null) {
            where.append("  AND created_at <= :createdEnd\n");
        }
        return where.toString();
    }

    private MapSqlParameterSource parameters(Long tenantId, Long shopId, AdminAuditTimelineQueryParam query) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("shopId", shopId)
                .addValue("source", blankToNull(query.getSource()))
                .addValue("eventType", blankToNull(query.getEventType()))
                .addValue("eventStatus", blankToNull(query.getEventStatus()))
                .addValue("userId", query.getUserId())
                .addValue("username", blankToNull(query.getUsername()))
                .addValue("taskId", query.getTaskId())
                .addValue("traceId", blankToNull(query.getTraceId()))
                .addValue("toolCode", blankToNull(query.getToolCode()))
                .addValue("riskLevel", normalizedRiskLevel(query.getRiskLevel()))
                .addValue("createdStart", query.getCreatedStart())
                .addValue("createdEnd", query.getCreatedEnd())
                .addValue("offset", query.offset())
                .addValue("limit", query.safePageSize());
    }

    private void appendStringFilter(StringBuilder where, String value, String column) {
        if (value != null && !value.isBlank()) {
            where.append("  AND ").append(column).append(" = :").append(toParameterName(column)).append("\n");
        }
    }

    private void appendRiskLevelFilter(StringBuilder where, String riskLevel) {
        if (riskLevel != null && !riskLevel.isBlank()) {
            where.append("  AND UPPER(risk_level) = :riskLevel\n");
        }
    }

    private void appendLongFilter(StringBuilder where, Long value, String column) {
        if (value != null) {
            where.append("  AND ").append(column).append(" = :").append(toParameterName(column)).append("\n");
        }
    }

    private String toParameterName(String column) {
        StringBuilder name = new StringBuilder();
        boolean upperNext = false;
        for (char current : column.toCharArray()) {
            if (current == '_') {
                upperNext = true;
            } else if (upperNext) {
                name.append(Character.toUpperCase(current));
                upperNext = false;
            } else {
                name.append(current);
            }
        }
        return name.toString();
    }

    private Map<String, Object> detail(String source, String detailA, String detailB, String detailC, String detailD) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if ("AUTH".equals(source)) {
            putIfPresent(detail, "authType", detailA);
            putIfPresent(detail, "clientIp", detailB);
            putIfPresent(detail, "failureReason", detailC);
        } else if ("TASK".equals(source)) {
            putIfPresent(detail, "fromStatus", detailA);
            putIfPresent(detail, "toStatus", detailB);
            putIfPresent(detail, "eventDataJson", detailC);
        } else if ("TOOL".equals(source)) {
            putIfPresent(detail, "stepId", detailA);
            putIfPresent(detail, "latencyMs", detailB);
            putIfPresent(detail, "errorCode", detailC);
            putIfPresent(detail, "errorMessage", detailD);
        } else if ("APPROVAL".equals(source)) {
            putIfPresent(detail, "approvalNo", detailA);
            putIfPresent(detail, "sourceOrStatus", detailB);
            putIfPresent(detail, "titleOrComment", detailC);
        } else if ("CONNECTOR".equals(source)) {
            putIfPresent(detail, "connectorCode", detailA);
            putIfPresent(detail, "message", detailB);
            putIfPresent(detail, "detailJson", detailC);
        }
        return detail;
    }

    private void putIfPresent(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizedRiskLevel(String riskLevel) {
        return riskLevel == null || riskLevel.isBlank() ? null : riskLevel.toUpperCase();
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

    private LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.toString());
    }
}
