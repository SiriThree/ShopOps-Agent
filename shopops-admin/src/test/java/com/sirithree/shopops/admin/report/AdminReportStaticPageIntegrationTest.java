package com.sirithree.shopops.admin.report;

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
class AdminReportStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminReportStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/reports.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .contains("text/html")
                .contains("charset=UTF-8");
        assertThat(response.getBody())
                .contains("ShopOps Reports")
                .contains("Report Preview")
                .contains("Evidence")
                .contains("/api/reports")
                .contains("/api/reports/${encodeURIComponent(reportId)}")
                .contains("applyInitialQuery")
                .contains("new URLSearchParams(window.location.search)")
                .contains("syncUrl")
                .contains("updateContextLinks")
                .contains("summaryItems")
                .contains("evidenceSummary")
                .contains("id=\"evidenceSummary\"")
                .contains("id=\"openTask\"")
                .contains("id=\"openAudit\"")
                .contains("id=\"openToolLogs\"")
                .contains("/admin/tasks.html?taskId=")
                .contains("/admin/tools.html?taskId=")
                .contains("window.history.replaceState")
                .contains("positiveInt(params.get(\"pageNum\"), 1)")
                .contains("positiveInt(params.get(\"pageSize\"), 20)")
                .contains("empty-state")
                .contains("data-retry-list")
                .contains("errorRow")
                .contains("data-quick-filter=\"daily\"")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/audit.html");
    }
}
