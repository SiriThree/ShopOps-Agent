package com.sirithree.shopops.admin.connector;

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
class AdminConnectorStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminConnectorStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<byte[]> response = restTemplate.exchange(
                "http://localhost:" + port + "/admin/connectors.html",
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
                .contains("ShopOps 连接器中心")
                .contains("文件数据源状态")
                .contains("连接器状态已刷新")
                .contains("/api/admin/connectors/status")
                .contains("shopops.connector.order-summary.file")
                .contains("shopops.connector.negative-comments.file")
                .contains("shopops.connector.product-candidates.file")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("管理导航")
                .contains("/admin/dashboard.html")
                .contains("/admin/tasks.html")
                .contains("/admin/reports.html")
                .contains("/admin/audit.html");
    }
}
