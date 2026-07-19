package com.sirithree.shopops.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AdminAuditStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAuditCenterStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/audit.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .contains("ShopOps Audit Center")
                .contains("Audit Center")
                .contains("/api/admin/audit/overview")
                .contains("/api/admin/audit/timeline")
                .contains("/api/admin/audit/high-risk")
                .contains("/api/admin/audit/export");
    }
}
