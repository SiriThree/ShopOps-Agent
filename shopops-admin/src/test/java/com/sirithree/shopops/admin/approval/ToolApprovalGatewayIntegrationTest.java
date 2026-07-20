package com.sirithree.shopops.admin.approval;

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
class ToolApprovalGatewayIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateApprovalRequestWhenToolRequiresApproval() {
        Map<String, Object> result = dataOf(post(
                "/api/tools/order.refund_execute/invoke",
                Map.of("shopId", 1, "refundAmount", 1288, "reason", "high value refund"),
                adminHeaders()
        ));

        Integer approvalId = ((Number) result.get("approvalId")).intValue();
        Integer toolCallLogId = ((Number) result.get("toolCallLogId")).intValue();
        assertThat(result)
                .containsEntry("success", false)
                .containsEntry("status", "APPROVAL_REQUIRED")
                .containsEntry("errorCode", "APPROVAL_REQUIRED")
                .containsEntry("approvalId", approvalId)
                .containsEntry("toolCallLogId", toolCallLogId);

        Map<String, Object> approvalPage = dataOf(get(
                "/api/admin/approvals?status=PENDING&toolCode=order.refund_execute",
                adminHeaders()
        ));
        assertThat(approvalPage.get("total")).isEqualTo(1);
        Map<String, Object> approval = ((List<Map<String, Object>>) approvalPage.get("list")).get(0);
        assertThat(approval)
                .containsEntry("approvalId", approvalId)
                .containsEntry("sourceType", "TOOL_CALL")
                .containsEntry("sourceId", toolCallLogId)
                .containsEntry("toolCode", "order.refund_execute")
                .containsEntry("riskLevel", "HIGH")
                .containsEntry("status", "PENDING");

        Map<String, Object> logPage = dataOf(get(
                "/api/tools/call-logs?status=APPROVAL_REQUIRED&toolCode=order.refund_execute",
                adminHeaders()
        ));
        assertThat(logPage.get("total")).isEqualTo(1);
        Map<String, Object> log = ((List<Map<String, Object>>) logPage.get("list")).get(0);
        assertThat(log)
                .containsEntry("id", toolCallLogId)
                .containsEntry("approvalId", approvalId)
                .containsEntry("riskLevel", "HIGH")
                .containsEntry("status", "APPROVAL_REQUIRED")
                .containsEntry("errorCode", "APPROVAL_REQUIRED");
    }

    private Map<String, Object> get(String path, HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(
                url(path),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private Map<String, Object> post(String path, Map<String, Object> body, HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(
                url(path),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(Map<String, Object> response) {
        assertThat(response).isNotNull();
        assertThat(response.get("code")).isEqualTo(200);
        return (Map<String, Object>) response.get("data");
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        headers.set("X-User-Name", "admin");
        headers.set("X-User-Roles", "ADMIN");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
