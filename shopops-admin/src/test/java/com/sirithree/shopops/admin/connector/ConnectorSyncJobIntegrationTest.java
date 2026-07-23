package com.sirithree.shopops.admin.connector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class ConnectorSyncJobIntegrationTest {
    private static final String UNCONFIGURED_CONNECTOR_CODE = "file.external-reports";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateListRetryAndAuditConnectorSyncJobs() {
        ResponseEntity<Map> forbidden = exchange(
                "/api/admin/connectors/sync-jobs",
                HttpMethod.POST,
                Map.of("connectorCode", UNCONFIGURED_CONNECTOR_CODE, "remark", "operator denied"),
                operatorHeaders()
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> created = dataOf(exchange(
                "/api/admin/connectors/sync-jobs",
                HttpMethod.POST,
                Map.of("connectorCode", UNCONFIGURED_CONNECTOR_CODE, "remark", "manual sync"),
                adminHeaders()
        ).getBody());
        assertThat(created)
                .containsEntry("connectorCode", UNCONFIGURED_CONNECTOR_CODE)
                .containsEntry("status", "FAILED")
                .containsEntry("attempt", 1)
                .containsEntry("maxAttempts", 3)
                .containsEntry("triggerType", "MANUAL");
        assertThat(created.get("message").toString()).contains("未配置");
        Long jobId = ((Number) created.get("jobId")).longValue();

        Map<String, Object> retried = dataOf(exchange(
                "/api/admin/connectors/sync-jobs/" + jobId + "/retry",
                HttpMethod.POST,
                null,
                adminHeaders()
        ).getBody());
        assertThat(retried)
                .containsEntry("jobId", jobId.intValue())
                .containsEntry("status", "FAILED")
                .containsEntry("attempt", 2)
                .containsEntry("triggerType", "RETRY");

        Map<String, Object> page = dataOf(exchange(
                "/api/admin/connectors/sync-jobs?status=FAILED&connectorCode=" + UNCONFIGURED_CONNECTOR_CODE,
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        assertThat(page).containsEntry("total", 1);
        List<Map<String, Object>> jobs = (List<Map<String, Object>>) page.get("list");
        assertThat(jobs.get(0))
                .containsEntry("jobId", jobId.intValue())
                .containsEntry("attempt", 2);

        Map<String, Object> apiCallPage = dataOf(exchange(
                "/api/admin/connectors/api-call-logs?jobId=" + jobId + "&status=FAILED&endpoint=connector.status.check",
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        assertThat(apiCallPage).containsEntry("total", 2);
        List<Map<String, Object>> apiCalls = (List<Map<String, Object>>) apiCallPage.get("list");
        assertThat(apiCalls)
                .extracting(item -> item.get("connectorCode"))
                .containsOnly(UNCONFIGURED_CONNECTOR_CODE);
        assertThat(apiCalls.get(0))
                .containsEntry("jobId", jobId.intValue())
                .containsEntry("requestMethod", "CHECK")
                .containsEntry("endpoint", "connector.status.check")
                .containsEntry("status", "FAILED")
                .containsEntry("statusCode", 503)
                .containsEntry("errorCode", "NOT_CONFIGURED");
        assertThat(((Number) apiCalls.get(0).get("latencyMs")).longValue()).isGreaterThanOrEqualTo(0L);

        Map<String, Object> auditPage = dataOf(exchange(
                "/api/admin/audit/timeline?source=CONNECTOR&eventType=CONNECTOR_SYNC_RETRIED&pageNum=1&pageSize=10",
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        assertThat(auditPage).containsEntry("total", 1);
        List<Map<String, Object>> events = (List<Map<String, Object>>) auditPage.get("list");
        assertThat(events.get(0))
                .containsEntry("eventStatus", "FAILURE")
                .containsEntry("toolCode", UNCONFIGURED_CONNECTOR_CODE);
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Map<String, Object> body, HttpHeaders headers) {
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                new HttpEntity<>(body, headers),
                Map.class
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(Map response) {
        assertThat(response).isNotNull();
        assertThat(response.get("code")).isEqualTo(200);
        return (Map<String, Object>) response.get("data");
    }

    private HttpHeaders adminHeaders() {
        return headers("1", "admin", "ADMIN");
    }

    private HttpHeaders operatorHeaders() {
        return headers("2", "operator", "OPERATOR");
    }

    private HttpHeaders headers(String userId, String username, String roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", userId);
        headers.set("X-User-Name", username);
        headers.set("X-User-Roles", roles);
        return headers;
    }
}
