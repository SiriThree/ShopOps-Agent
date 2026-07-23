package com.sirithree.shopops.admin.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    private static final Pattern APPROVAL_SCRIPT = Pattern.compile("src=\"(/admin/assets/approvals-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminApprovalStaticPage() {
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
                .contains("shopops-ui-stack")
                .contains("React")
                .contains("TypeScript")
                .contains("Axios")
                .contains("Ant Design")
                .contains("Approval Center")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/approvals-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/admin/approvals")
                .contains("/api/admin/approvals/${approvalId}/${action}")
                .contains("/api/admin/approvals/batch/${action}")
                .contains("/api/admin/approvals/expire-stale")
                .contains("withdrawBtn")
                .contains("batchApproveBtn")
                .contains("batchRejectBtn")
                .contains("expireStaleBtn")
                .contains("confirmText")
                .contains("PENDING")
                .contains("WITHDRAWN")
                .contains("EXPIRED")
                .contains("/admin/audit.html?source=APPROVAL")
                .contains("approvalId")
                .contains("data-quick-filter")
                .contains("pending")
                .contains("high")
                .contains("refund")
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

    private static String approvalScriptPath(String body) {
        Matcher matcher = APPROVAL_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(approvalScriptPath(body)).isNotBlank();
        Matcher matcher = JS_ASSET.matcher(body);
        return matcher.results()
                .map(result -> result.group(1))
                .distinct()
                .map(path -> {
                    ResponseEntity<String> script = restTemplate.getForEntity(
                            "http://localhost:" + port + path,
                            String.class
                    );
                    assertThat(script.getStatusCode().is2xxSuccessful()).isTrue();
                    return script.getBody();
                })
                .collect(Collectors.joining("\n"));
    }
}
