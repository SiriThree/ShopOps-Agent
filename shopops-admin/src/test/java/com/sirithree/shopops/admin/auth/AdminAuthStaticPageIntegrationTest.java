package com.sirithree.shopops.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AdminAuthStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminAuthStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/auth.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .contains("text/html")
                .contains("charset=UTF-8");
        assertThat(response.getBody())
                .contains("ShopOps Auth")
                .contains("Current User")
                .contains("Auth Audit Events")
                .contains("/api/admin/auth/login")
                .contains("/api/admin/auth/me")
                .contains("/api/admin/auth/logout")
                .contains("/api/admin/auth/audit-events")
                .contains("applyInitialQuery")
                .contains("new URLSearchParams(window.location.search)")
                .contains("syncUrl")
                .contains("window.history.replaceState")
                .contains("positiveInt(params.get(\"pageNum\"), 1)")
                .contains("positiveInt(params.get(\"pageSize\"), 10)")
                .contains("empty-state")
                .contains("data-retry-list")
                .contains("errorRow")
                .contains("id=\"loginSubmit\"")
                .contains("withBusy")
                .contains("Applying")
                .contains("id=\"copyUser\"")
                .contains("id=\"copyEventDetail\"")
                .contains("copyText")
                .contains("navigator.clipboard")
                .contains("fallbackCopy")
                .contains("data-quick-filter=\"failure\"")
                .contains("aria-label=\"Admin navigation\"")
                .contains("Agent Tasks")
                .contains("Audit Center")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/audit.html")
                .contains("/admin/tools.html");
    }
}
