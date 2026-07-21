package com.sirithree.shopops.admin.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AdminUserStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminUserStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<byte[]> response = restTemplate.exchange(
                "http://localhost:" + port + "/admin/users.html",
                HttpMethod.GET,
                null,
                byte[].class
        );
        String body = new String(response.getBody(), StandardCharsets.UTF_8);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .contains("text/html")
                .contains("charset=UTF-8");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .contains("no-store")
                .contains("no-cache");
        assertThat(body)
                .contains("用户租户")
                .contains("组织目录")
                .contains("店铺成员")
                .contains("新增用户")
                .contains("新增租户")
                .contains("新增店铺")
                .contains("绑定店铺成员")
                .contains("店铺配置")
                .contains("重置密码")
                .contains("成员权限已更新")
                .contains("用户已创建")
                .contains("租户已更新")
                .contains("店铺已创建")
                .contains("店铺成员已绑定")
                .contains("店铺配置已保存")
                .contains("/api/admin/organization/overview")
                .contains("/api/admin/organization/users")
                .contains("/password")
                .contains("/api/admin/organization/tenants")
                .contains("/api/admin/organization/shops")
                .contains("/configs")
                .contains("/api/admin/organization/shop-members")
                .contains("shopops.auth.token")
                .contains("Authorization")
                .contains("aria-label=\"管理导航\"")
                .contains("/admin/dashboard.html")
                .contains("/admin/auth.html")
                .contains("/admin/audit.html");
    }
}
