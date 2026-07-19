package com.sirithree.shopops.admin.agent;

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
class AdminTaskStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminTaskStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/tasks.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .contains("text/html")
                .contains("charset=UTF-8");
        assertThat(response.getBody())
                .contains("ShopOps Agent Tasks")
                .contains("Agent Tasks")
                .contains("Task Detail")
                .contains("Recent Events")
                .contains("/api/admin/agent/tasks")
                .contains("/api/admin/agent/tasks/metrics")
                .contains("/api/admin/agent/tasks/events")
                .contains("/api/admin/agent/tasks/${encodeURIComponent(taskId)}/detail")
                .contains("data-quick-filter=\"failed\"")
                .contains("/admin/dashboard.html")
                .contains("/admin/audit.html");
    }
}
