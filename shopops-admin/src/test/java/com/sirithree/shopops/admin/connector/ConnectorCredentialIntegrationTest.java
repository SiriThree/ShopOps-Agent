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
class ConnectorCredentialIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldManageConnectorCredentialsWithoutReturningSecret() {
        ResponseEntity<Map> forbidden = exchange(
                "/api/admin/connectors/credentials",
                HttpMethod.POST,
                Map.of(
                        "connectorCode", "file.order-summary",
                        "credentialType", "api_key",
                        "secretValue", "sk-test-secret-001"
                ),
                operatorHeaders()
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> saved = dataOf(exchange(
                "/api/admin/connectors/credentials",
                HttpMethod.POST,
                Map.of(
                        "connectorCode", "file.order-summary",
                        "credentialType", "api_key",
                        "secretValue", "sk-test-secret-001"
                ),
                adminHeaders()
        ).getBody());
        assertThat(saved)
                .containsEntry("connectorCode", "file.order-summary")
                .containsEntry("credentialType", "API_KEY")
                .containsEntry("configured", true)
                .containsEntry("enabled", true)
                .containsEntry("status", "ENABLED");
        assertThat(saved.get("maskedSecret")).isEqualTo("sk-****001");
        assertThat(saved.toString()).doesNotContain("sk-test-secret-001");

        Map<String, Object> credentials = responseData(exchange(
                "/api/admin/connectors/credentials",
                HttpMethod.GET,
                null,
                adminHeaders()
        ).getBody());
        List<Map<String, Object>> list = (List<Map<String, Object>>) credentials.get("data");
        assertThat(list)
                .extracting(item -> item.get("connectorCode"))
                .contains("file.order-summary", "file.negative-comments", "file.product-candidates");
        assertThat(list.toString()).doesNotContain("sk-test-secret-001");

        Map<String, Object> testResult = dataOf(exchange(
                "/api/admin/connectors/credentials/file.order-summary/test",
                HttpMethod.POST,
                null,
                adminHeaders()
        ).getBody());
        assertThat(testResult)
                .containsEntry("connectorCode", "file.order-summary")
                .containsEntry("success", true)
                .containsEntry("status", "PASS");

        Map<String, Object> disabled = dataOf(exchange(
                "/api/admin/connectors/credentials/file.order-summary/disable",
                HttpMethod.POST,
                null,
                adminHeaders()
        ).getBody());
        assertThat(disabled)
                .containsEntry("connectorCode", "file.order-summary")
                .containsEntry("enabled", false)
                .containsEntry("status", "DISABLED");

        Map<String, Object> disabledTest = dataOf(exchange(
                "/api/admin/connectors/credentials/file.order-summary/test",
                HttpMethod.POST,
                null,
                adminHeaders()
        ).getBody());
        assertThat(disabledTest)
                .containsEntry("success", false)
                .containsEntry("status", "DISABLED");
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
        Map<String, Object> result = responseData(response);
        assertThat(result.get("code")).isEqualTo(200);
        return (Map<String, Object>) result.get("data");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> responseData(Map response) {
        assertThat(response).isNotNull();
        return (Map<String, Object>) response;
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
