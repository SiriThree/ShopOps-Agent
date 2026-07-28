package com.sirithree.shopops.admin.audit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineQueryParam;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class AdminAuditTimelineJdbcRepositoryTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldPushTimelineFiltersAndPaginationIntoUnionSql() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(Class.class)))
                .thenReturn(0L);
        AdminAuditTimelineJdbcRepository repository = new AdminAuditTimelineJdbcRepository(jdbcTemplate);

        AdminAuditTimelineQueryParam query = new AdminAuditTimelineQueryParam();
        query.setSource("TOOL");
        query.setEventStatus("SUCCESS");
        query.setTaskId(10001L);
        query.setTraceId("trace_daily_review_10001");
        query.setRiskLevel("low");
        query.setElevatedRisk(true);
        query.setCreatedStart(LocalDateTime.of(2026, 7, 18, 0, 0));
        query.setPageNum(2);
        query.setPageSize(25);

        repository.listTimeline(1L, 1L, query);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> parameterCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), parameterCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        MapSqlParameterSource parameters = parameterCaptor.getValue();
        assertThat(sql)
                .contains("UNION ALL")
                .contains("FROM auth_audit_event")
                .contains("event_type LIKE 'ORG!_%' ESCAPE '!'")
                .contains("FROM agent_task_event")
                .contains("WHEN JSON_VALID(event_data_json)")
                .contains("FROM tool_call_log")
                .contains("FROM approval_request")
                .contains("AND source = :source")
                .contains("AND event_status = :eventStatus")
                .contains("AND task_id = :taskId")
                .contains("AND trace_id = :traceId")
                .contains("AND UPPER(risk_level) = :riskLevel")
                .contains("AND UPPER(risk_level) NOT IN ('LOW', 'UNKNOWN')")
                .contains("AND created_at >= :createdStart")
                .contains("ORDER BY created_at DESC, sortable_id DESC")
                .contains("LIMIT :limit OFFSET :offset");
        assertThat(parameters.getValue("tenantId")).isEqualTo(1L);
        assertThat(parameters.getValue("shopId")).isEqualTo(1L);
        assertThat(parameters.getValue("source")).isEqualTo("TOOL");
        assertThat(parameters.getValue("eventStatus")).isEqualTo("SUCCESS");
        assertThat(parameters.getValue("taskId")).isEqualTo(10001L);
        assertThat(parameters.getValue("traceId")).isEqualTo("trace_daily_review_10001");
        assertThat(parameters.getValue("riskLevel")).isEqualTo("LOW");
        assertThat(parameters.getValue("offset")).isEqualTo(25);
        assertThat(parameters.getValue("limit")).isEqualTo(25);
    }
}
