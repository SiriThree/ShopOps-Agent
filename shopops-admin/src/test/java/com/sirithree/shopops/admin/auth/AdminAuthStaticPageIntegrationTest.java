package com.sirithree.shopops.admin.auth;

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
class AdminAuthStaticPageIntegrationTest {
    private static final Pattern AUTH_SCRIPT = Pattern.compile("src=\"(/admin/assets/auth-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminAuthStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/auth.html",
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
                .contains("shopops-ui-stack")
                .contains("React")
                .contains("TypeScript")
                .contains("Axios")
                .contains("Ant Design")
                .contains("Auth Center")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/auth-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/admin/auth/login")
                .contains("/api/admin/auth/me")
                .contains("/api/admin/auth/logout")
                .contains("/api/admin/auth/audit-events")
                .contains("applyInitialQuery")
                .contains("new URLSearchParams(window.location.search)")
                .contains("syncUrl")
                .contains("window.history.replaceState")
                .contains("positiveInt")
                .contains("empty-state")
                .contains("data-retry-list")
                .contains("errorRow")
                .contains("loginSubmit")
                .contains("authFilterSubmit")
                .contains("withBusy")
                .contains("copyUser")
                .contains("copyEventDetail")
                .contains("copyText")
                .contains("navigator.clipboard")
                .contains("fallbackCopy")
                .contains("data-quick-filter")
                .contains("failure")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("X-User-Roles")
                .contains("Authorization")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/audit.html")
                .contains("/admin/tools.html");
    }

    private static String authScriptPath(String body) {
        Matcher matcher = AUTH_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(authScriptPath(body)).isNotBlank();
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
