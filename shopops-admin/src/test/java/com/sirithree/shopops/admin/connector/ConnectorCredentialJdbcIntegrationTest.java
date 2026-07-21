package com.sirithree.shopops.admin.connector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@EnabledIfSystemProperty(named = "shopops.jdbc.it", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=jdbc",
                "shopops.connector.credential-secret=jdbc-it-credential-secret",
                "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                "spring.datasource.username=root",
                "spring.datasource.password=root",
                "spring.datasource.hikari.initialization-fail-timeout=1",
                "spring.datasource.hikari.connection-timeout=3000"
        }
)
class ConnectorCredentialJdbcIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistEncryptedConnectorCredential() {
        jdbcTemplate.update("""
                DELETE FROM connector_credential
                WHERE tenant_id = 1 AND shop_id = 1 AND connector_code = 'file.order-summary'
                """);

        String plainSecret = "sk-jdbc-secret-001";
        ResponseEntity<Map> saveResponse = restTemplate.exchange(
                url("/api/admin/connectors/credentials"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "connectorCode", "file.order-summary",
                        "credentialType", "api_key",
                        "secretValue", plainSecret
                ), adminHeaders()),
                Map.class
        );

        assertThat(saveResponse.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> saved = (Map<String, Object>) saveResponse.getBody().get("data");
        assertThat(saved)
                .containsEntry("maskedSecret", "sk-****001")
                .containsEntry("status", "ENABLED");
        assertThat(saved.toString()).doesNotContain(plainSecret);

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT encrypted_secret, secret_preview, status
                FROM connector_credential
                WHERE tenant_id = 1 AND shop_id = 1 AND connector_code = 'file.order-summary'
                """);
        assertThat(row.get("encrypted_secret").toString())
                .startsWith("v1:")
                .doesNotContain(plainSecret);
        assertThat(row)
                .containsEntry("secret_preview", "sk-****001")
                .containsEntry("status", "ENABLED");

        ResponseEntity<Map> testResponse = restTemplate.exchange(
                url("/api/admin/connectors/credentials/file.order-summary/test"),
                HttpMethod.POST,
                new HttpEntity<>(adminHeaders()),
                Map.class
        );
        Map<String, Object> testResult = (Map<String, Object>) testResponse.getBody().get("data");
        assertThat(testResult)
                .containsEntry("success", true)
                .containsEntry("status", "PASS");
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
