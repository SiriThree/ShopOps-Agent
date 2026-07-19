package com.sirithree.shopops.admin.tool;

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
class AdminToolStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminToolStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/tools.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .contains("text/html")
                .contains("charset=UTF-8");
        assertThat(response.getBody())
                .contains("ShopOps Tools")
                .contains("Tool Registry")
                .contains("Manual Invoke")
                .contains("Call Logs")
                .contains("/api/tools")
                .contains("/api/tools/call-logs")
                .contains("/api/tools/${encodeURIComponent(toolCode)}")
                .contains("/invoke")
                .contains("applyInitialQuery")
                .contains("new URLSearchParams(window.location.search)")
                .contains("syncUrl")
                .contains("updateContextLinks")
                .contains("summaryItems")
                .contains("id=\"toolSummary\"")
                .contains("id=\"logSummary\"")
                .contains("id=\"openTask\"")
                .contains("id=\"openAudit\"")
                .contains("id=\"openReports\"")
                .contains("/admin/tasks.html?taskId=")
                .contains("/admin/reports.html?taskId=")
                .contains("window.history.replaceState")
                .contains("positiveInt(params.get(\"pageNum\"), 1)")
                .contains("positiveInt(params.get(\"pageSize\"), 20)")
                .contains("data-quick-filter=\"failed\"")
                .contains("aria-label=\"Admin navigation\"")
                .contains("Agent Tasks")
                .contains("Audit Center")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/audit.html");
    }
}
