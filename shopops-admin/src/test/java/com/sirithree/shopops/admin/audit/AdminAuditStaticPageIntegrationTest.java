package com.sirithree.shopops.admin.audit;

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
class AdminAuditStaticPageIntegrationTest {
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

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

        String html = response.getBody();
        assertThat(html)
                .contains("ShopOps 审计中心")
                .contains("shopops-ui-stack")
                .contains("React")
                .contains("TypeScript")
                .contains("Axios")
                .contains("ECharts")
                .contains("Ant Design")
                .contains("Audit center")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/audit-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, html);
        assertThat(scripts)
                .contains("/api/admin/audit/overview")
                .contains("/api/admin/audit/high-risk")
                .contains("/api/admin/audit/timeline?")
                .contains("/api/admin/audit/timeline/")
                .contains("/api/admin/audit/export.csv")
                .contains("source")
                .contains("eventType")
                .contains("eventStatus")
                .contains("taskId")
                .contains("traceId")
                .contains("toolCode")
                .contains("riskLevel")
                .contains("elevatedRisk")
                .contains("pageNum")
                .contains("pageSize")
                .contains("window.history.replaceState")
                .contains("new URLSearchParams(window.location.search)")
                .contains("navigator.clipboard.writeText")
                .contains("shopops.auth.token")
                .contains("Authorization")
                .contains("data-quick-filter")
                .contains("data-empty-reset")
                .contains("audit-risk-chart")
                .contains("configSnapshotBox")
                .contains("configChangeBox")
                .contains("shopConfigSnapshot")
                .contains("recentShopConfigChange")
                .contains("refundRateWarnThreshold")
                .contains("negativeCommentWarnThreshold")
                .contains("agentToolApprovalEnabled")
                .contains("agentModelPolicy")
                .contains("openTask")
                .contains("openReport")
                .contains("openApproval")
                .contains("openToolLogs")
                .contains("/admin/tasks.html?taskId=")
                .contains("/admin/reports.html?reportId=")
                .contains("/admin/approvals.html")
                .contains("/admin/tools.html?toolCode=");
    }

    private String allScriptText(TestRestTemplate restTemplate, String html) {
        Matcher matcher = SCRIPT_PATTERN.matcher(html);
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
