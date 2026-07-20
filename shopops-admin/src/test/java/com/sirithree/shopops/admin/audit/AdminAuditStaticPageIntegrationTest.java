package com.sirithree.shopops.admin.audit;

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
class AdminAuditStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAuditCenterStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/audit.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .contains("text/html")
                .contains("charset=UTF-8");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .contains("no-store")
                .contains("no-cache");
        assertThat(response.getBody())
                .contains("ShopOps 审计中心")
                .contains("审计中心")
                .contains("CSV 下载")
                .contains("快捷筛选")
                .contains("<option value=\"ELEVATED\">中高风险</option>")
                .contains("elevatedRisk")
                .contains("applyInitialQuery")
                .contains("new URLSearchParams(window.location.search)")
                .contains("syncUrl")
                .contains("updateContextLinks")
                .contains("summaryItems")
                .contains("id=\"eventSummary\"")
                .contains("工具调用")
                .contains("id=\"openTask\"")
                .contains("id=\"openReport\"")
                .contains("id=\"openToolLogs\"")
                .contains("/admin/tasks.html?taskId=")
                .contains("/admin/reports.html?reportId=")
                .contains("window.history.replaceState")
                .contains("positiveInt(params.get(\"pageNum\"), 1)")
                .contains("positiveInt(params.get(\"pageSize\"), 20)")
                .contains("empty-state")
                .contains("data-retry-list")
                .contains("errorRow")
                .contains("withBusy")
                .contains("#filterForm button.primary")
                .contains("查询中")
                .contains("重置中")
                .contains("加载中")
                .contains("刷新中")
                .contains("下载中")
                .contains("id=\"copyDetail\"")
                .contains("copyText")
                .contains("navigator.clipboard")
                .contains("fallbackCopy")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("applyStoredSession")
                .contains("id=\"sessionLine\"")
                .contains("data-quick-filter=\"failed\"")
                .contains("data-empty-reset")
                .contains(".timeline-column")
                .contains("max-width: 100%")
                .contains("/api/admin/audit/overview")
                .contains("/api/admin/audit/timeline")
                .contains("/api/admin/audit/high-risk")
                .contains("/api/admin/audit/export.csv");
    }
}
