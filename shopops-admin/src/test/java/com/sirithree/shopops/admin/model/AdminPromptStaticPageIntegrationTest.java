package com.sirithree.shopops.admin.model;

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
class AdminPromptStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminPromptStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/prompts.html",
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
                .contains("ShopOps 提示词模板")
                .contains("模板版本")
                .contains("版本编辑")
                .contains("渲染测试")
                .contains("daily_review.plan")
                .contains("daily_review.report")
                .contains("/api/admin/prompts")
                .contains("/versions")
                .contains("/enable")
                .contains("/render-test")
                .contains("applyInitialQuery")
                .contains("new URLSearchParams(window.location.search)")
                .contains("syncUrl")
                .contains("withBusy")
                .contains("保存中")
                .contains("启用中")
                .contains("渲染中")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("applyStoredSession")
                .contains("id=\"sessionLine\"")
                .contains("navigator.clipboard")
                .contains("aria-label=\"后台导航\"")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/tools.html")
                .contains("/admin/audit.html");
    }
}
