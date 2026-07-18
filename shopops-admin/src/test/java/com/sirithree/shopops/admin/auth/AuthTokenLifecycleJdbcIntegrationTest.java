package com.sirithree.shopops.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@EnabledIfSystemProperty(named = "shopops.jdbc.it", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=jdbc",
                "shopops.auth.header-dev-mode=false",
                "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                "spring.datasource.username=root",
                "spring.datasource.password=root"
        }
)
class AuthTokenLifecycleJdbcIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistSessionAndRejectRevokedToken() {
        String adminToken = login("admin");
        String operatorToken = login("operator");
        assertThat(getMe(operatorToken).getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<Map> logoutResponse = restTemplate.exchange(
                url("/api/admin/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(operatorToken)),
                Map.class
        );
        assertThat(logoutResponse.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<Map> meAfterLogout = getMe(operatorToken);
        assertThat(meAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Map<String, Object> logoutPage = getWithBearer(
                "/api/admin/auth/audit-events?eventType=LOGOUT&eventStatus=SUCCESS&username=operator",
                adminToken
        );
        assertThat(((Number) logoutPage.get("total")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat((List<Map<String, Object>>) logoutPage.get("list"))
                .extracting(event -> event.get("username"))
                .contains("operator");
    }

    @SuppressWarnings("unchecked")
    private String login(String username) {
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/admin/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "username", username,
                        "password", "shopops123",
                        "tenantId", 1,
                        "shopId", 1
                )),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return data.get("accessToken").toString();
    }

    private ResponseEntity<Map> getMe(String token) {
        return restTemplate.exchange(
                url("/api/admin/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                Map.class
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getWithBearer(String path, String token) {
        ResponseEntity<Map> response = restTemplate.exchange(
                url(path),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return (Map<String, Object>) response.getBody().get("data");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
