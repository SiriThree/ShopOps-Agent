package com.sirithree.shopops.admin.report;

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
class AdminReportStaticPageIntegrationTest {
    private static final Pattern REPORT_SCRIPT = Pattern.compile("src=\"(/admin/assets/reports-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminReportStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/reports.html",
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
                .contains("ShopOps 报告中心")
                .contains("shopops-ui-stack")
                .contains("React, TypeScript, Axios, ECharts, Ant Design")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/reports-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/reports?")
                .contains("/api/reports/")
                .contains("pageNum")
                .contains("pageSize")
                .contains("reportId")
                .contains("taskId")
                .contains("traceId")
                .contains("createdBy")
                .contains("shopops.auth.token")
                .contains("Authorization")
                .contains("window.history.replaceState")
                .contains("navigator.clipboard.writeText")
                .contains("orderSummary")
                .contains("negativeComments")
                .contains("productCandidates")
                .contains("refundRateWarnThreshold")
                .contains("agentToolApprovalEnabled")
                .contains("/admin/tasks.html?taskId=")
                .contains("/admin/audit.html?source=TASK&taskId=")
                .contains("/admin/tools.html?taskId=");
    }

    private static String reportScriptPath(String body) {
        Matcher matcher = REPORT_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(reportScriptPath(body)).isNotBlank();
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
