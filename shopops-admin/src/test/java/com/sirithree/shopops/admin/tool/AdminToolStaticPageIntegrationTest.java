package com.sirithree.shopops.admin.tool;

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
class AdminToolStaticPageIntegrationTest {
    private static final Pattern TOOL_SCRIPT = Pattern.compile("src=\"(/admin/assets/tools-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminToolStaticPage() {
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
                .contains("shopops-ui-stack")
                .contains("React")
                .contains("TypeScript")
                .contains("Axios")
                .contains("ECharts")
                .contains("Ant Design")
                .contains("MCP tool logs")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/tools-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/tools")
                .contains("/api/tools/")
                .contains("/invoke")
                .contains("/api/tools/call-logs?")
                .contains("pageNum")
                .contains("pageSize")
                .contains("logId")
                .contains("taskId")
                .contains("status")
                .contains("toolCode")
                .contains("approvalId")
                .contains("riskLevel")
                .contains("needApproval")
                .contains("window.history.replaceState")
                .contains("new URLSearchParams(window.location.search)")
                .contains("navigator.clipboard.writeText")
                .contains("fallbackCopy")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("tool-status-chart")
                .contains("data-quick-filter")
                .contains("toolSummary")
                .contains("logSummary")
                .contains("invokeSubmit")
                .contains("toolFilterSubmit")
                .contains("copyToolDetail")
                .contains("copyInvokeResult")
                .contains("copyLogPayload")
                .contains("openTask")
                .contains("openAudit")
                .contains("openReports")
                .contains("/admin/tasks.html?taskId=")
                .contains("/admin/reports.html?taskId=")
                .contains("/admin/audit.html?source=TOOL&toolCode=");
    }

    private static String toolScriptPath(String body) {
        Matcher matcher = TOOL_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(toolScriptPath(body)).isNotBlank();
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
