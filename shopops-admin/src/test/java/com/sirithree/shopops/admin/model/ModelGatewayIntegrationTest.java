package com.sirithree.shopops.admin.model;

import static org.assertj.core.api.Assertions.assertThat;

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
class ModelGatewayIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldInvokeEchoModelAndRecordCallLog() {
        Map<String, Object> result = dataOf(post(
                "/api/admin/model-gateway/invoke",
                Map.of(
                        "providerCode", "echo",
                        "modelName", "echo-001",
                        "prompt", "Generate a short operating summary",
                        "promptCode", "daily_review.summary",
                        "promptVersion", "v1",
                        "traceId", "tr_model_001",
                        "taskId", 10001
                ),
                operatorHeaders()
        ));

        assertThat(result)
                .containsEntry("providerCode", "echo")
                .containsEntry("modelName", "echo-001")
                .containsEntry("status", "SUCCESS");
        assertThat(result.get("callId")).isNotNull();
        assertThat(result.get("outputText").toString()).contains("Generate a short operating summary");
        assertThat(((Number) result.get("totalTokens")).intValue()).isGreaterThan(0);

        Map<String, Object> logs = dataOf(get(
                "/api/admin/model-gateway/call-logs?traceId=tr_model_001",
                adminHeaders()
        ));
        assertThat(logs.get("total")).isEqualTo(1);
        Map<String, Object> log = ((java.util.List<Map<String, Object>>) logs.get("list")).get(0);
        assertThat(log)
                .containsEntry("status", "SUCCESS")
                .containsEntry("providerCode", "echo")
                .containsEntry("promptCode", "daily_review.summary")
                .containsEntry("taskId", 10001);
    }

    @Test
    void shouldRestrictInvokeRoleAndLogUnknownProviderFailure() {
        ResponseEntity<Map> viewerInvoke = exchange(
                "/api/admin/model-gateway/invoke",
                HttpMethod.POST,
                Map.of("prompt", "viewer cannot invoke"),
                viewerHeaders()
        );
        assertThat(viewerInvoke.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> failed = dataOf(post(
                "/api/admin/model-gateway/invoke",
                Map.of(
                        "providerCode", "missing",
                        "prompt", "this should fail",
                        "traceId", "tr_model_missing"
                ),
                operatorHeaders()
        ));
        assertThat(failed)
                .containsEntry("status", "FAILURE")
                .containsEntry("errorCode", "PROVIDER_NOT_FOUND");

        Map<String, Object> logs = dataOf(get(
                "/api/admin/model-gateway/call-logs?status=FAILURE&traceId=tr_model_missing",
                adminHeaders()
        ));
        assertThat(logs.get("total")).isEqualTo(1);
    }

    private Map<String, Object> get(String path, HttpHeaders headers) {
        ResponseEntity<Map> response = exchange(path, HttpMethod.GET, null, headers);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private Map<String, Object> post(String path, Map<String, Object> body, HttpHeaders headers) {
        ResponseEntity<Map> response = exchange(path, HttpMethod.POST, body, headers);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
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
    private Map<String, Object> dataOf(Map<String, Object> response) {
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

    private HttpHeaders viewerHeaders() {
        return headers("3", "viewer", "VIEWER");
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
