package com.sirithree.shopops.admin.agent;

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
class AdminTaskStaticPageIntegrationTest {
    private static final Pattern TASK_SCRIPT = Pattern.compile("src=\"(/admin/assets/tasks-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminTaskStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/tasks.html",
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
                .contains("ShopOps 任务队列")
                .contains("shopops-ui-stack")
                .contains("React, TypeScript, Axios, ECharts, Ant Design")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/tasks-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/admin/agent/tasks?")
                .contains("/api/admin/agent/tasks/metrics")
                .contains("/api/admin/agent/tasks/events?pageNum=1&pageSize=8")
                .contains("/api/admin/agent/tasks/")
                .contains("/detail")
                .contains("/api/agent/tasks")
                .contains("/retry")
                .contains("/api/agent/tasks/stale/requeue?queuedTimeoutMinutes=10&runningTimeoutMinutes=30&limit=20")
                .contains("pageNum")
                .contains("pageSize")
                .contains("taskId")
                .contains("traceId")
                .contains("reportId")
                .contains("shopops.auth.token")
                .contains("Authorization")
                .contains("window.history.replaceState")
                .contains("navigator.clipboard.writeText")
                .contains("shopConfigSnapshot")
                .contains("refundRateWarnThreshold")
                .contains("agentToolApprovalEnabled")
                .contains("task-metrics-chart")
                .contains("barMaxWidth")
                .contains("data-quick-filter")
                .contains("/admin/reports.html?reportId=")
                .contains("/admin/audit.html?source=TASK&taskId=")
                .contains("/admin/tools.html?taskId=");
    }

    private static String taskScriptPath(String body) {
        Matcher matcher = TASK_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(taskScriptPath(body)).isNotBlank();
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
