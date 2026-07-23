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
class AdminWorkbenchStaticPageIntegrationTest {
    private static final Pattern WORKBENCH_SCRIPT = Pattern.compile("src=\"(/admin/assets/workbench-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminWorkbenchStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/workbench.html",
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
                .contains("ShopOps Agent 工作台")
                .contains("shopops-ui-stack")
                .contains("React, TypeScript, Axios, ECharts, Ant Design")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/workbench-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/agent/tasks/natural-language")
                .contains("/api/agent/tasks/")
                .contains("/api/admin/agent/tasks?pageNum=1&pageSize=5")
                .contains("/api/reports/")
                .contains("/admin/tasks.html?taskId=")
                .contains("shopops.auth.token")
                .contains("Authorization")
                .contains("2018-08-07")
                .contains("useOlistDemo")
                .contains("order.query_summary")
                .contains("comment.query_negative")
                .contains("product.query_candidates")
                .contains("ad.query_performance")
                .contains("barMaxWidth")
                .contains("metrics-chart")
                .contains("natural-language")
                .contains("orderSummary")
                .contains("negativeComments")
                .contains("productCandidates");
    }

    @Test
    void shouldServeAdminIndexAsWorkbenchEntry() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/index.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .contains("/admin/workbench.html")
                .contains("ShopOps Agent 工作台");
    }

    private static String workbenchScriptPath(String body) {
        Matcher matcher = WORKBENCH_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(workbenchScriptPath(body)).isNotBlank();
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
