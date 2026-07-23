package com.sirithree.shopops.admin.organization;

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
class AdminUserStaticPageIntegrationTest {
    private static final Pattern USER_SCRIPT = Pattern.compile("src=\"(/admin/assets/users-[^\"]+\\.js)\"");
    private static final Pattern JS_ASSET = Pattern.compile("(?:src|href)=\"(/admin/assets/[^\"]+\\.js)\"");

    @LocalServerPort
    private int port;

    @Test
    void shouldServeReactAdminUserStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/users.html",
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
                .contains("ShopOps 组织中心")
                .contains("shopops-ui-stack")
                .contains("React")
                .contains("TypeScript")
                .contains("Axios")
                .contains("Ant Design")
                .contains("Organization Center")
                .contains("<div id=\"root\"></div>")
                .contains("/admin/assets/users-")
                .contains("/admin/assets/styles-");

        String scripts = allScriptText(restTemplate, response.getBody());

        assertThat(scripts)
                .contains("/api/admin/organization/overview")
                .contains("/api/admin/organization/users")
                .contains("/password")
                .contains("/api/admin/organization/tenants")
                .contains("/api/admin/organization/shops")
                .contains("/configs")
                .contains("/api/admin/organization/shop-members")
                .contains("createUserBtn")
                .contains("resetPasswordBtn")
                .contains("saveTenantBtn")
                .contains("saveShopBtn")
                .contains("addShopMemberBtn")
                .contains("updateMemberBtn")
                .contains("saveShopConfigBtn")
                .contains("shopConfigFields")
                .contains("refundRateWarnThreshold")
                .contains("negativeCommentWarnThreshold")
                .contains("agentToolApprovalEnabled")
                .contains("agentModelPolicy")
                .contains("restoreShopConfigDefaults")
                .contains("TENANT_ADMIN")
                .contains("SHOP_OPERATOR")
                .contains("refund_rate_warn_threshold")
                .contains("agent_tool_approval_enabled")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("X-User-Roles")
                .contains("Authorization")
                .contains("/admin/workbench.html")
                .contains("/admin/dashboard.html")
                .contains("/admin/audit.html")
                .contains("/admin/connectors.html");
    }

    private static String userScriptPath(String body) {
        Matcher matcher = USER_SCRIPT.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String allScriptText(TestRestTemplate restTemplate, String body) {
        assertThat(userScriptPath(body)).isNotBlank();
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
