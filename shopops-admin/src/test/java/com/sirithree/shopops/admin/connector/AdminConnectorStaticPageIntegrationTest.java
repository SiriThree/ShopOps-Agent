package com.sirithree.shopops.admin.connector;

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
class AdminConnectorStaticPageIntegrationTest {
    private static final Pattern CONNECTOR_SCRIPT = Pattern.compile("src=\"(/admin/assets/connectors-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminConnectorStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/connectors.html",
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
                .contains("ShopOps 数据接入中心")
                .contains("shopops-ui-stack")
                .contains("React")
                .contains("TypeScript")
                .contains("Axios")
                .contains("Ant Design")
                .contains("Connector Center")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/connectors-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/admin/connectors/status")
                .contains("/api/admin/connectors/credentials")
                .contains("/api/admin/connectors/credentials/${connectorCode}/test")
                .contains("/api/admin/connectors/credentials/${connectorCode}/disable")
                .contains("/api/admin/connectors/sync-jobs")
                .contains("/api/admin/connectors/sync-jobs/${jobId}/retry")
                .contains("/api/admin/connectors/api-call-logs")
                .contains("saveCredential")
                .contains("testCredential")
                .contains("disableCredential")
                .contains("triggerSync")
                .contains("filterSyncJobs")
                .contains("filterApiLogs")
                .contains("secret only submitted on save request, not echoed")
                .contains("Masked Secret")
                .contains("manual sync from react connector center")
                .contains("file.order-summary")
                .contains("connector.status.check")
                .contains("shopops.connector.order-summary.file")
                .contains("shopops.connector.negative-comments.file")
                .contains("shopops.connector.product-candidates.file")
                .contains("shopops.connector.ad-performance.file")
                .contains("shopops.connector.external-reports.file")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("X-User-Roles")
                .contains("Authorization")
                .contains("applyInitialQuery")
                .contains("syncUrl")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/audit.html")
                .contains("/admin/workbench.html");
    }

    private static String connectorScriptPath(String body) {
        Matcher matcher = CONNECTOR_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(connectorScriptPath(body)).isNotBlank();
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
