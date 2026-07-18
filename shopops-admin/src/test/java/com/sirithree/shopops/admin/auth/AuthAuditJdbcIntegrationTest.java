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
class AuthAuditJdbcIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldRecordLoginAndAccessDeniedAuditEvents() {
        String adminToken = login("admin");
        assertThat(adminToken).isNotBlank();

        ResponseEntity<Map> invalidLogin = restTemplate.exchange(
                url("/api/admin/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "username", "ghost",
                        "password", "wrong",
                        "tenantId", 1,
                        "shopId", 1
                )),
                Map.class
        );
        assertThat(invalidLogin.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String viewerToken = login("viewer");
        HttpHeaders viewerHeaders = new HttpHeaders();
        viewerHeaders.setBearerAuth(viewerToken);
        ResponseEntity<Map> deniedCreate = restTemplate.exchange(
                url("/api/agent/tasks"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "taskType", "daily_review",
                        "userInput", "daily review",
                        "dateRange", Map.of("start", "2026-07-18", "end", "2026-07-18")
                ), viewerHeaders),
                Map.class
        );
        assertThat(deniedCreate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> loginSuccessPage = getWithBearer(
                "/api/admin/auth/audit-events?eventType=LOGIN&eventStatus=SUCCESS&username=admin",
                adminToken
        );
        assertThat(((Number) loginSuccessPage.get("total")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat((List<Map<String, Object>>) loginSuccessPage.get("list"))
                .extracting(event -> event.get("username"))
                .contains("admin");

        Map<String, Object> loginFailurePage = getWithBearer(
                "/api/admin/auth/audit-events?eventType=LOGIN&eventStatus=FAILURE&username=ghost",
                adminToken
        );
        assertThat(((Number) loginFailurePage.get("total")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat((List<Map<String, Object>>) loginFailurePage.get("list"))
                .extracting(event -> event.get("failureReason"))
                .contains("Invalid username or password");

        Map<String, Object> accessDeniedPage = getWithBearer(
                "/api/admin/auth/audit-events?eventType=ACCESS_DENIED&eventStatus=FAILURE&userId=3",
                adminToken
        );
        assertThat(((Number) accessDeniedPage.get("total")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat((List<Map<String, Object>>) accessDeniedPage.get("list"))
                .extracting(event -> event.get("username"))
                .contains("viewer");
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> getWithBearer(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> response = restTemplate.exchange(
                url(path),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo(200);
        return (Map<String, Object>) body.get("data");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
