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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class ApprovalCenterIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateListReadAndApproveApprovalRequest() {
        Map<String, Object> approval = dataOf(post(
                "/api/admin/approvals",
                Map.of(
                        "sourceType", "TOOL_CALL",
                        "sourceId", 77,
                        "taskId", 11,
                        "stepId", 22,
                        "traceId", "tr_approval_001",
                        "toolCode", "order.refund_execute",
                        "riskLevel", "high",
                        "title", "High risk refund",
                        "reason", "Manual approval required",
                        "inputSummary", "{\"amount\":1288}"
                ),
                operatorHeaders()
        ));

        Integer approvalId = ((Number) approval.get("approvalId")).intValue();
        assertThat(approval.get("approvalNo").toString()).startsWith("APR");
        assertThat(approval)
                .containsEntry("status", "PENDING")
                .containsEntry("sourceType", "TOOL_CALL")
                .containsEntry("toolCode", "order.refund_execute")
                .containsEntry("riskLevel", "HIGH")
                .containsEntry("requesterId", 2)
                .containsEntry("requesterName", "operator");

        Map<String, Object> page = dataOf(get("/api/admin/approvals?status=PENDING&toolCode=order.refund_execute", adminHeaders()));
        assertThat(page.get("total")).isEqualTo(1);
        assertThat((List<Map<String, Object>>) page.get("list"))
                .extracting(item -> item.get("approvalId"))
                .contains(approvalId);

        Map<String, Object> detail = dataOf(get("/api/admin/approvals/" + approvalId, adminHeaders()));
        assertThat(detail).containsEntry("approvalId", approvalId);

        Map<String, Object> approved = dataOf(post(
                "/api/admin/approvals/" + approvalId + "/approve",
                Map.of("comment", "Approved after review"),
                adminHeaders()
        ));
        assertThat(approved)
                .containsEntry("status", "APPROVED")
                .containsEntry("approverId", 1)
                .containsEntry("approverName", "admin")
                .containsEntry("decisionComment", "Approved after review");
        assertThat(approved.get("decidedAt")).isNotNull();

        Map<String, Object> rejectedAgain = post(
                "/api/admin/approvals/" + approvalId + "/reject",
                Map.of("comment", "Too late"),
                adminHeaders()
        );
        assertThat(rejectedAgain.get("code")).isEqualTo(500);
        assertThat(rejectedAgain.get("data")).isNull();
    }

    @Test
    void shouldRestrictCreateAndDecisionRoles() {
        ResponseEntity<Map> createResponse = exchange(
                "/api/admin/approvals",
                HttpMethod.POST,
                Map.of("title", "viewer request"),
                viewerHeaders()
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().get("code")).isEqualTo(403);

        Map<String, Object> approval = dataOf(post(
                "/api/admin/approvals",
                Map.of("title", "operator request"),
                operatorHeaders()
        ));
        Integer approvalId = ((Number) approval.get("approvalId")).intValue();

        ResponseEntity<Map> approveResponse = exchange(
                "/api/admin/approvals/" + approvalId + "/approve",
                HttpMethod.POST,
                Map.of("comment", "operator cannot approve"),
                operatorHeaders()
        );
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(approveResponse.getBody()).isNotNull();
        assertThat(approveResponse.getBody().get("code")).isEqualTo(403);
    }

    private Map<String, Object> get(String path, HttpHeaders headers) {
        ResponseEntity<Map> response = exchange(path, HttpMethod.GET, null, headers);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private Map<String, Object> post(String path, Map<String, Object> body, HttpHeaders headers) {
        ResponseEntity<Map> response = exchange(path, HttpMethod.POST, body, headers);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Map<String, Object> body, HttpHeaders headers) {
        return restTemplate.exchange(
                url(path),
                method,
                new HttpEntity<>(body, headers),
                Map.class
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(Map<String, Object> response) {
        assertThat(response).isNotNull();
        assertThat(response.get("code")).isEqualTo(200);
        return (Map<String, Object>) response.get("data");
    }

    private HttpHeaders adminHeaders() {
        return headers("1", "admin", "ADMIN");
    }

    private HttpHeaders operatorHeaders() {
        return headers("2", "operator", "OPERATOR");
    }

    private HttpHeaders viewerHeaders() {
        return headers("3", "viewer", "VIEWER");
    }

    private HttpHeaders headers(String userId, String username, String roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", userId);
        headers.set("X-User-Name", username);
        headers.set("X-User-Roles", roles);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
