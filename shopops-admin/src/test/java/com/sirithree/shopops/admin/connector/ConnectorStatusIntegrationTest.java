package com.sirithree.shopops.admin.connector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class ConnectorStatusIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldListFileConnectorStatus() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        headers.set("X-User-Roles", "ADMIN");

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/connectors/status",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(200);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(data)
                .hasSize(4)
                .extracting(item -> item.get("connectorCode"))
                .containsExactly("file.order-summary", "file.negative-comments", "file.product-candidates", "file.ad-performance");
        assertThat(data)
                .allSatisfy(item -> {
                    assertThat(item.get("status")).isEqualTo("NOT_CONFIGURED");
                    assertThat(item.get("configured")).isEqualTo(false);
                    assertThat(item.get("available")).isEqualTo(false);
                    assertThat(item.get("message")).isEqualTo("未配置，将使用内存默认数据");
                    assertThat(item.get("lastCheckedAt")).isNotNull();
                });
    }
}
