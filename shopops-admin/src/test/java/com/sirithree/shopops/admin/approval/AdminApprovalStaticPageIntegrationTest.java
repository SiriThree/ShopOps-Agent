package com.sirithree.shopops.admin.approval;

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
class AdminApprovalStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminApprovalStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/approvals.html",
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
                .contains("ShopOps 审批中心")
                .contains("审批中心")
                .contains("审批详情")
                .contains("处理意见")
                .contains("/api/admin/approvals")
                .contains("/api/admin/approvals/${encodeURIComponent(state.selected.approvalId)}/${action}")
                .contains("id=\"withdrawBtn\"")
                .contains("<option value=\"WITHDRAWN\">")
                .contains("/admin/audit.html?source=APPROVAL")
                .contains("id=\"approvalId\"")
                .contains("data-quick-filter=\"pending\"")
                .contains("data-quick-filter=\"high\"")
                .contains("data-quick-filter=\"refund\"")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("X-User-Roles")
                .contains("Authorization")
                .contains("applyInitialQuery")
                .contains("syncUrl")
                .contains("positiveInt(params.get(\"pageNum\"), 1)")
                .contains("positiveInt(params.get(\"pageSize\"), 20)")
                .contains("navigator.clipboard")
                .contains("fallbackCopy")
                .contains("/admin/dashboard.html")
                .contains("/admin/audit.html")
                .contains("/admin/tools.html")
                .contains("/admin/tasks.html");
    }
}
