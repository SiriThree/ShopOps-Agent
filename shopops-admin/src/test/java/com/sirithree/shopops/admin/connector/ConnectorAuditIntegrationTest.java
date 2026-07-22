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
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class ConnectorAuditIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldRecordConnectorCredentialActionsInAuditCenter() {
        post("/api/admin/connectors/credentials", Map.of(
                "connectorCode", "file.order-summary",
                "credentialType", "api_key",
                "secretValue", "sk-audit-secret-001"
        ));
        post("/api/admin/connectors/credentials/file.order-summary/test", null);
        post("/api/admin/connectors/credentials/file.order-summary/disable", null);
        post("/api/admin/connectors/credentials/file.order-summary/test", null);

        Map<String, Object> timeline = dataOf(get("/api/admin/audit/timeline?source=CONNECTOR&pageNum=1&pageSize=10"));
        assertThat(((Number) timeline.get("total")).longValue()).isEqualTo(4L);
        List<Map<String, Object>> events = (List<Map<String, Object>>) timeline.get("list");
        assertThat(events)
                .extracting(event -> event.get("eventType"))
                .contains("CONNECTOR_CREDENTIAL_SAVED", "CONNECTOR_CREDENTIAL_TESTED", "CONNECTOR_CREDENTIAL_DISABLED");
        assertThat(events)
                .extracting(event -> event.get("resourceType"))
                .containsOnly("connector_audit_event");
        assertThat(events.toString()).doesNotContain("sk-audit-secret-001");
        assertThat(events)
                .extracting(event -> event.get("eventStatus"))
                .contains("SUCCESS", "FAILURE");

        Map<String, Object> detail = dataOf(get("/api/admin/audit/timeline/CONNECTOR/" + events.get(0).get("resourceId")));
        assertThat((Map<String, Object>) detail.get("event"))
                .containsEntry("source", "CONNECTOR")
                .containsEntry("resourceType", "connector_audit_event");
        assertThat((Map<String, Object>) detail.get("resource")).containsKey("connectorAuditEvent");

        Map<String, Object> elevated = dataOf(get("/api/admin/audit/timeline?source=CONNECTOR&elevatedRisk=true&pageNum=1&pageSize=10"));
        assertThat(((Number) elevated.get("total")).longValue()).isGreaterThanOrEqualTo(2L);
        assertThat((List<Map<String, Object>>) elevated.get("list"))
                .extracting(event -> event.get("riskLevel"))
                .containsOnly("MEDIUM");
    }

    private ResponseEntity<Map> get(String path) {
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(adminHeaders()), Map.class);
    }

    private ResponseEntity<Map> post(String path, Map<String, Object> body) {
        return restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, adminHeaders()), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        return (Map<String, Object>) response.getBody().get("data");
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        headers.set("X-User-Name", "admin");
        headers.set("X-User-Roles", "ADMIN");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
