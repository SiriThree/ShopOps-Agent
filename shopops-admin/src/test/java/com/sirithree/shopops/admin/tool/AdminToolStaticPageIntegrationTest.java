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
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .contains("no-store")
                .contains("no-cache");
        assertThat(response.getBody())
                .contains("ShopOps 工具日志")
                .contains("工具注册表")
                .contains("手动调用")
                .contains("调用日志")
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
                .contains("empty-state")
                .contains("data-retry-list")
                .contains("errorRow")
                .contains("id=\"invokeSubmit\"")
                .contains("id=\"toolFilterSubmit\"")
                .contains("withBusy")
                .contains("查询中")
                .contains("重置中")
                .contains("加载中")
                .contains("刷新中")
                .contains("调用中")
                .contains("id=\"copyToolDetail\"")
                .contains("id=\"copyInvokeResult\"")
                .contains("id=\"copyLogPayload\"")
                .contains("copyText")
                .contains("navigator.clipboard")
                .contains("fallbackCopy")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("applyStoredSession")
                .contains("id=\"sessionLine\"")
                .contains("data-quick-filter=\"failed\"")
                .contains("aria-label=\"管理导航\"")
                .contains("任务队列")
                .contains("审计中心")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/audit.html");
    }
}
