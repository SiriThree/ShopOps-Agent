package com.sirithree.shopops.admin.dashboard;

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
class AdminDashboardStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminDashboardStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/dashboard.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .contains("text/html")
                .contains("charset=UTF-8");
        assertThat(response.getBody())
                .contains("ShopOps Admin Dashboard")
                .contains("ShopOps Dashboard")
                .contains("Admin modules")
                .contains("Task Queue")
                .contains("Tool Logs")
                .contains("System Health")
                .contains("Audit Risk")
                .contains("Recent Failures")
                .contains("/api/admin/dashboard/summary")
                .contains("/api/system/health")
                .contains("/api/admin/audit/high-risk")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("applyStoredSession")
                .contains("id=\"sessionLine\"")
                .contains("/admin/tasks.html?status=FAILED")
                .contains("/admin/tools.html?status=FAILED")
                .contains("taskModuleHint")
                .contains("auditModuleHint")
                .contains("/admin/audit.html");
    }
}
