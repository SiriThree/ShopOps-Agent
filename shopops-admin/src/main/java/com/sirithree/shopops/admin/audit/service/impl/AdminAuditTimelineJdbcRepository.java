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
                    event.setDetail(detail(rs.getString("source"), rs.getString("detail_a"), rs.getString("detail_b"), rs.getString("detail_c")));
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
                      'AUTH' AS source,
                      CONCAT('auth:', id) AS event_id,
                      id AS sortable_id,
                      event_type,
                      event_status,
                      user_id,
                      username,
                      CAST(NULL AS SIGNED) AS task_id,
                      CAST(NULL AS CHAR) AS trace_id,
                      CAST(NULL AS CHAR) AS tool_code,
                      request_id,
                      'auth_audit_event' AS resource_type,
                      CAST(id AS CHAR) AS resource_id,
                      CASE
                        WHEN event_status = 'FAILURE' THEN 'HIGH'
                        WHEN event_type IN ('ACCESS_DENIED', 'AUTHENTICATION') THEN 'MEDIUM'
                        ELSE 'LOW'
                      END AS risk_level,
                      CONCAT(event_type, ' ', event_status) AS summary,
                      created_at,
                      auth_type AS detail_a,
                      client_ip AS detail_b,
                      failure_reason AS detail_c
                    FROM auth_audit_event
                    WHERE tenant_id = :tenantId AND shop_id = :shopId
                  UNION ALL
                    SELECT
                      'TASK' AS source,
                      CONCAT('task:', id) AS event_id,
                      id AS sortable_id,
                      event_type,
                      CASE WHEN event_type = 'TASK_FAILED' THEN 'FAILURE' ELSE 'SUCCESS' END AS event_status,
                      operator_id AS user_id,
                      CAST(NULL AS CHAR) AS username,
                      task_id,
                      JSON_UNQUOTE(JSON_EXTRACT(event_data_json, '$.traceId')) AS trace_id,
                      CAST(NULL AS CHAR) AS tool_code,
                      CAST(NULL AS CHAR) AS request_id,
                      'agent_task' AS resource_type,
                      CAST(task_id AS CHAR) AS resource_id,
                      CASE
                        WHEN event_type = 'TASK_FAILED' THEN 'HIGH'
                        WHEN event_type IN ('TASK_RETRY_REQUESTED', 'TASK_REQUEUED') THEN 'MEDIUM'
                        ELSE 'LOW'
                      END AS risk_level,
                      event_type AS summary,
                      created_at,
                      from_status AS detail_a,
                      to_status AS detail_b,
                      event_data_json AS detail_c
                    FROM agent_task_event
                    WHERE tenant_id = :tenantId AND shop_id = :shopId
                  UNION ALL
                    SELECT
                      'TOOL' AS source,
                      CONCAT('tool:', tcl.id) AS event_id,
                      tcl.id AS sortable_id,
                      'TOOL_CALL' AS event_type,
                      tcl.status AS event_status,
                      tcl.user_id,
                      CAST(NULL AS CHAR) AS username,
                      tcl.task_id,
                      tcl.trace_id,
                      tcl.tool_code,
                      CAST(NULL AS CHAR) AS request_id,
                      'tool_call_log' AS resource_type,
                      CAST(tcl.id AS CHAR) AS resource_id,
                      COALESCE(
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
                      ) AS risk_level,
                      CONCAT('TOOL_CALL ', tcl.tool_code, ' ', tcl.status) AS summary,
                      tcl.created_at,
                      CAST(tcl.step_id AS CHAR) AS detail_a,
                      CAST(tcl.latency_ms AS CHAR) AS detail_b,
                      tcl.error_code AS detail_c
                    FROM tool_call_log tcl
                    WHERE tcl.tenant_id = :tenantId AND tcl.shop_id = :shopId
                  UNION ALL
                    SELECT
                      'APPROVAL' AS source,
                      CONCAT('approval:', ar.id, ':created') AS event_id,
                      ar.id * 10 AS sortable_id,
                      'APPROVAL_CREATED' AS event_type,
                      'PENDING' AS event_status,
                      ar.requester_id AS user_id,
                      ar.requester_name AS username,
                      ar.task_id,
                      ar.trace_id,
                      ar.tool_code,
                      CAST(NULL AS CHAR) AS request_id,
                      'approval_request' AS resource_type,
                      CAST(ar.id AS CHAR) AS resource_id,
                      UPPER(ar.risk_level) AS risk_level,
                      CONCAT('APPROVAL_CREATED ', ar.status) AS summary,
                      ar.created_at,
                      ar.approval_no AS detail_a,
                      ar.source_type AS detail_b,
                      ar.title AS detail_c
                    FROM approval_request ar
                    WHERE ar.tenant_id = :tenantId AND ar.shop_id = :shopId
                  UNION ALL
                    SELECT
                      'APPROVAL' AS source,
                      CONCAT('approval:', ar.id, ':decision') AS event_id,
                      ar.id * 10 + 1 AS sortable_id,
                      'APPROVAL_DECIDED' AS event_type,
                      CASE WHEN ar.status = 'REJECTED' THEN 'FAILURE' ELSE 'SUCCESS' END AS event_status,
                      ar.approver_id AS user_id,
                      ar.approver_name AS username,
                      ar.task_id,
                      ar.trace_id,
                      ar.tool_code,
                      CAST(NULL AS CHAR) AS request_id,
                      'approval_request' AS resource_type,
                      CAST(ar.id AS CHAR) AS resource_id,
                      UPPER(ar.risk_level) AS risk_level,
                      CONCAT('APPROVAL_DECIDED ', ar.status) AS summary,
                      ar.decided_at AS created_at,
                      ar.approval_no AS detail_a,
                      ar.status AS detail_b,
                      ar.decision_comment AS detail_c
                    FROM approval_request ar
                    WHERE ar.tenant_id = :tenantId AND ar.shop_id = :shopId
                      AND ar.status <> 'PENDING'
                      AND ar.decided_at IS NOT NULL
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

    private Map<String, Object> detail(String source, String detailA, String detailB, String detailC) {
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
        } else if ("APPROVAL".equals(source)) {
            putIfPresent(detail, "approvalNo", detailA);
            putIfPresent(detail, "sourceOrStatus", detailB);
            putIfPresent(detail, "titleOrComment", detailC);
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
