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
                .contains("ShopOps 管理总览")
                .contains("后台模块")
                .contains("任务队列")
                .contains("工具日志")
                .contains("系统健康")
                .contains("审计风险")
                .contains("最近失败")
                .contains("/api/admin/dashboard/summary")
                .contains("/api/system/health")
                .contains("/api/admin/audit/high-risk")
                .contains("Promise.allSettled")
                .contains("renderSummaryError")
                .contains("renderHealthError")
                .contains("renderRiskError")
                .contains("panel-state")
                .contains("withBusy")
                .contains("刷新中")
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
