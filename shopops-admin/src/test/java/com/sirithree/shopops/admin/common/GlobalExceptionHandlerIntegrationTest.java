package com.sirithree.shopops.admin.common;

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
class GlobalExceptionHandlerIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnUnifiedValidationResultWhenRequestBodyIsInvalid() {
        Map<String, Object> request = Map.of(
                "taskType", "daily_review",
                "userInput", "帮我生成今天店铺运营复盘"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/agent/tasks"),
                HttpMethod.POST,
                new HttpEntity<>(request, defaultHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
        assertThat(response.getBody().get("message").toString()).contains("dateRange");
        assertThat(response.getBody().get("data")).isNull();
    }

    @Test
    void shouldReturnUnifiedValidationResultWhenHeaderTypeIsInvalid() {
        HttpHeaders headers = defaultHeaders();
        headers.set("X-Tenant-Id", "abc");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/tools"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(400);
        assertThat(response.getBody().get("message").toString()).contains("X-Tenant-Id");
    }

    @Test
    void shouldReturnRequestIdHeaderFromRequestContext() {
        HttpHeaders headers = defaultHeaders();
        headers.set("X-Request-Id", "req_test_001");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/tools"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo("req_test_001");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() {
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/tools"),
                HttpMethod.GET,
                new HttpEntity<>(defaultHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst("X-Request-Id")).startsWith("req_");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnCurrentUserContext() {
        HttpHeaders headers = defaultHeaders();
        headers.set("X-User-Name", "ops-admin");
        headers.set("X-User-Roles", "ADMIN,OPERATOR");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/admin/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("tenantId")).isEqualTo(1);
        assertThat(data.get("shopId")).isEqualTo(1);
        assertThat(data.get("userId")).isEqualTo(1);
        assertThat(data.get("username")).isEqualTo("ops-admin");
        assertThat((List<String>) data.get("roles")).contains("ADMIN", "OPERATOR");
        assertThat(data.get("authType")).isEqualTo("HEADER");
        assertThat(data.get("authenticated")).isEqualTo(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLoginAndResolveCurrentUserFromBearerToken() {
        Map<String, Object> loginRequest = Map.of(
                "username", "operator",
                "password", "shopops123",
                "tenantId", 1,
                "shopId", 1
        );

        ResponseEntity<Map> loginResponse = restTemplate.exchange(
                url("/api/admin/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                Map.class
        );

        assertThat(loginResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(loginResponse.getBody()).isNotNull();
        Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().get("data");
        String accessToken = loginData.get("accessToken").toString();
        assertThat(accessToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> meResponse = restTemplate.exchange(
                url("/api/admin/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(meResponse.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> meData = (Map<String, Object>) meResponse.getBody().get("data");
        assertThat(meData.get("userId")).isEqualTo(2);
        assertThat(meData.get("username")).isEqualTo("operator");
        assertThat((List<String>) meData.get("roles")).contains("OPERATOR");
        assertThat(meData.get("authType")).isEqualTo("BEARER");
    }

    @Test
    void shouldRejectInvalidBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/admin/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(401);
    }

    @Test
    void shouldAllowViewerToReadTools() {
        HttpHeaders headers = defaultHeaders();
        headers.set("X-User-Roles", "VIEWER");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/tools"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
    }

    @Test
    void shouldDenyViewerWhenCreatingTask() {
        HttpHeaders headers = defaultHeaders();
        headers.set("X-User-Roles", "VIEWER");
        Map<String, Object> request = Map.of(
                "taskType", "daily_review",
                "userInput", "daily review",
                "dateRange", Map.of("start", "2026-07-18", "end", "2026-07-18")
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/agent/tasks"),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(403);
    }

    @Test
    void shouldDenyViewerWhenInvokingToolManually() {
        HttpHeaders headers = defaultHeaders();
        headers.set("X-User-Roles", "VIEWER");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/tools/order.query_summary/invoke"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(403);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnSystemHealthInMemoryMode() {
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/system/health"),
                HttpMethod.GET,
                new HttpEntity<>(defaultHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        Map<String, Object> checks = (Map<String, Object>) data.get("checks");
        Map<String, Object> database = (Map<String, Object>) checks.get("database");
        Map<String, Object> flyway = (Map<String, Object>) checks.get("flyway");
        Map<String, Object> redis = (Map<String, Object>) checks.get("redis");
        Map<String, Object> rabbitmq = (Map<String, Object>) checks.get("rabbitmq");
        Map<String, Object> toolRegistry = (Map<String, Object>) checks.get("toolRegistry");

        assertThat(data.get("status")).isEqualTo("UP");
        assertThat(data.get("persistence")).isEqualTo("memory");
        assertThat(database.get("mode")).isEqualTo("SKIPPED");
        assertThat(flyway.get("mode")).isEqualTo("SKIPPED");
        assertThat(redis.get("mode")).isEqualTo("SKIPPED");
        assertThat(rabbitmq.get("mode")).isEqualTo("SKIPPED");
        assertThat(toolRegistry.get("status")).isEqualTo("UP");
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
