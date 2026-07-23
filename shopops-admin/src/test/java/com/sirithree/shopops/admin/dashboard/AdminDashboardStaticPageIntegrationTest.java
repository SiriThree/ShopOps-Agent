package com.sirithree.shopops.admin.dashboard;

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
class AdminDashboardStaticPageIntegrationTest {
    private static final Pattern DASHBOARD_SCRIPT = Pattern.compile("src=\"(/admin/assets/dashboard-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminDashboardStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/dashboard.html",
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
                .contains("ShopOps 管理总览")
                .contains("shopops-ui-stack")
                .contains("React")
                .contains("TypeScript")
                .contains("Axios")
                .contains("ECharts")
                .contains("Ant Design")
                .contains("Dashboard overview")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/dashboard-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/admin/dashboard/summary")
                .contains("/api/system/health")
                .contains("/api/admin/audit/high-risk")
                .contains("Promise.allSettled")
                .contains("renderSummaryError")
                .contains("renderHealthError")
                .contains("renderRiskError")
                .contains("panel-state")
                .contains("dashboard-task-chart")
                .contains("taskModuleHint")
                .contains("auditModuleHint")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("taskMetrics")
                .contains("successRate")
                .contains("avgLatencyMs")
                .contains("recentFailedEvents")
                .contains("recentElevatedRiskEvents")
                .contains("checks")
                .contains("toolRegistry")
                .contains("/admin/workbench.html")
                .contains("/admin/tasks.html?status=FAILED")
                .contains("/admin/tools.html?status=FAILED")
                .contains("/admin/prompts.html")
                .contains("/admin/audit.html");
    }

    private static String dashboardScriptPath(String body) {
        Matcher matcher = DASHBOARD_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(dashboardScriptPath(body)).isNotBlank();
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
