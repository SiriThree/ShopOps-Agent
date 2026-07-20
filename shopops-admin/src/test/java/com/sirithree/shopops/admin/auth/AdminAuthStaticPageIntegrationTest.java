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
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .contains("no-store")
                .contains("no-cache");
        assertThat(response.getBody())
                .contains("ShopOps 认证")
                .contains("当前用户")
                .contains("认证审计事件")
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
                .contains("id=\"authFilterSubmit\"")
                .contains("withBusy")
                .contains("查询中")
                .contains("重置中")
                .contains("加载中")
                .contains("刷新中")
                .contains("应用中")
                .contains("id=\"copyUser\"")
                .contains("id=\"copyEventDetail\"")
                .contains("copyText")
                .contains("navigator.clipboard")
                .contains("fallbackCopy")
                .contains("data-quick-filter=\"failure\"")
                .contains("aria-label=\"管理导航\"")
                .contains("任务队列")
                .contains("审计中心")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/audit.html")
                .contains("/admin/tools.html");
    }
}
