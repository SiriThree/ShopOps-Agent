package com.sirithree.shopops.admin.model;

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
class AdminPromptStaticPageIntegrationTest {
    private static final Pattern PROMPT_SCRIPT = Pattern.compile("src=\"(/admin/assets/prompts-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminPromptStaticPage() {
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
                .contains("ShopOps 提示词与模型网关")
                .contains("shopops-ui-stack")
                .contains("React")
                .contains("TypeScript")
                .contains("Axios")
                .contains("Ant Design")
                .contains("Prompt and Model Gateway")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/prompts-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/admin/prompts")
                .contains("/versions")
                .contains("/enable")
                .contains("/render-test")
                .contains("/api/admin/model-gateway/call-logs")
                .contains("daily_review.plan")
                .contains("daily_review.report")
                .contains("daily_review.summary")
                .contains("applyInitialQuery")
                .contains("new URLSearchParams(window.location.search)")
                .contains("syncUrl")
                .contains("navigator.clipboard")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("aria-label")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/tools.html")
                .contains("/admin/audit.html");
    }

    private static String promptScriptPath(String body) {
        Matcher matcher = PROMPT_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(promptScriptPath(body)).isNotBlank();
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
