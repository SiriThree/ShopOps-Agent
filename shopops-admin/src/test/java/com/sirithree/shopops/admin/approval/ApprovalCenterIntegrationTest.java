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
                Map.of(
                        "comment", "Approved after review",
                        "confirmText", "确认通过"
                ),
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
    void shouldRequireConfirmTextForHighRiskApproval() {
        Integer highRiskApprovalId = createApproval("High risk confirmation required");

        ResponseEntity<Map> missingConfirm = exchange(
                "/api/admin/approvals/" + highRiskApprovalId + "/approve",
                HttpMethod.POST,
                Map.of("comment", "Approve without confirmation"),
                adminHeaders()
        );
        assertThat(missingConfirm.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingConfirm.getBody()).isNotNull();
        assertThat(missingConfirm.getBody().get("code")).isEqualTo(400);
        assertThat(missingConfirm.getBody().get("message").toString()).contains("确认通过");

        Map<String, Object> approved = dataOf(post(
                "/api/admin/approvals/" + highRiskApprovalId + "/approve",
                Map.of(
                        "comment", "Confirmed high risk approval",
                        "confirmText", "确认通过"
                ),
                adminHeaders()
        ));
        assertThat(approved)
                .containsEntry("status", "APPROVED")
                .containsEntry("decisionComment", "Confirmed high risk approval");
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

    @Test
    @SuppressWarnings("unchecked")
    void shouldWithdrawPendingApprovalRequest() {
        Map<String, Object> approval = dataOf(post(
                "/api/admin/approvals",
                Map.of(
                        "sourceType", "TOOL_CALL",
                        "sourceId", 88,
                        "traceId", "tr_approval_withdraw",
                        "toolCode", "order.refund_execute",
                        "riskLevel", "high",
                        "title", "Withdraw refund approval"
                ),
                operatorHeaders()
        ));
        Integer approvalId = ((Number) approval.get("approvalId")).intValue();

        ResponseEntity<Map> viewerWithdraw = exchange(
                "/api/admin/approvals/" + approvalId + "/withdraw",
                HttpMethod.POST,
                Map.of("comment", "viewer cannot withdraw"),
                viewerHeaders()
        );
        assertThat(viewerWithdraw.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> withdrawn = dataOf(post(
                "/api/admin/approvals/" + approvalId + "/withdraw",
                Map.of("comment", "Requester no longer needs this approval"),
                operatorHeaders()
        ));
        assertThat(withdrawn)
                .containsEntry("status", "WITHDRAWN")
                .containsEntry("approverId", 2)
                .containsEntry("approverName", "operator")
                .containsEntry("decisionComment", "Requester no longer needs this approval");
        assertThat(withdrawn.get("decidedAt")).isNotNull();

        Map<String, Object> approveAfterWithdraw = post(
                "/api/admin/approvals/" + approvalId + "/approve",
                Map.of("comment", "Too late"),
                adminHeaders()
        );
        assertThat(approveAfterWithdraw.get("code")).isEqualTo(500);
        assertThat(approveAfterWithdraw.get("data")).isNull();

        Map<String, Object> auditPage = dataOf(get(
                "/api/admin/audit/timeline?source=APPROVAL&eventStatus=CANCELED&pageNum=1&pageSize=20",
                adminHeaders()
        ));
        List<Map<String, Object>> currentEvents = ((List<Map<String, Object>>) auditPage.get("list")).stream()
                .filter(event -> approvalId.toString().equals(event.get("resourceId")))
                .toList();
        assertThat(currentEvents)
                .extracting(event -> event.get("eventType"))
                .contains("APPROVAL_WITHDRAWN");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBatchApproveAndRejectApprovalRequests() {
        Integer firstId = createApproval("Batch approval first");
        Integer secondId = createApproval("Batch approval second");
        Integer thirdId = createApproval("Batch approval third");

        Map<String, Object> approved = dataOf(post(
                "/api/admin/approvals/batch/approve",
                Map.of(
                        "approvalIds", List.of(firstId, secondId),
                        "comment", "Batch approved",
                        "confirmText", "确认通过"
                ),
                adminHeaders()
        ));
        assertThat(approved)
                .containsEntry("requestedCount", 2)
                .containsEntry("successCount", 2)
                .containsEntry("failedCount", 0);
        assertThat((List<Map<String, Object>>) approved.get("succeeded"))
                .extracting(item -> item.get("status"))
                .containsOnly("APPROVED");

        Map<String, Object> rejected = dataOf(post(
                "/api/admin/approvals/batch/reject",
                Map.of(
                        "approvalIds", List.of(firstId, thirdId, 99999),
                        "comment", "Batch rejected"
                ),
                adminHeaders()
        ));
        assertThat(rejected)
                .containsEntry("requestedCount", 3)
                .containsEntry("successCount", 1)
                .containsEntry("failedCount", 2);
        assertThat((List<Map<String, Object>>) rejected.get("succeeded"))
                .extracting(item -> item.get("approvalId"), item -> item.get("status"), item -> item.get("decisionComment"))
                .containsExactly(org.assertj.core.groups.Tuple.tuple(thirdId, "REJECTED", "Batch rejected"));
        assertThat((List<Integer>) rejected.get("failedApprovalIds")).contains(firstId, 99999);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExpireStalePendingApprovalRequests() {
        Integer approvalId = createApproval("Expire stale approval");

        Map<String, Object> expired = dataOf(post(
                "/api/admin/approvals/expire-stale?timeoutMinutes=0&limit=10",
                Map.of(),
                adminHeaders()
        ));
        assertThat(((Number) expired.get("requestedCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) expired.get("successCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(expired).containsEntry("failedCount", 0);
        Map<String, Object> item = ((List<Map<String, Object>>) expired.get("succeeded")).stream()
                .filter(candidate -> approvalId.equals(((Number) candidate.get("approvalId")).intValue()))
                .findFirst()
                .orElseThrow();
        assertThat(item)
                .containsEntry("approvalId", approvalId)
                .containsEntry("status", "EXPIRED")
                .containsEntry("decisionComment", "审批超时自动关闭");

        Map<String, Object> approveAfterExpire = post(
                "/api/admin/approvals/" + approvalId + "/approve",
                Map.of("comment", "Too late"),
                adminHeaders()
        );
        assertThat(approveAfterExpire.get("code")).isEqualTo(500);
        assertThat(approveAfterExpire.get("data")).isNull();

        Map<String, Object> auditPage = dataOf(get(
                "/api/admin/audit/timeline?source=APPROVAL&eventStatus=CANCELED&pageNum=1&pageSize=20",
                adminHeaders()
        ));
        List<Map<String, Object>> currentEvents = ((List<Map<String, Object>>) auditPage.get("list")).stream()
                .filter(event -> approvalId.toString().equals(event.get("resourceId")))
                .toList();
        assertThat(currentEvents)
                .extracting(event -> event.get("eventType"))
                .contains("APPROVAL_EXPIRED");
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
                new HttpEntity<>(body == null || body.isEmpty() ? null : body, headers),
                Map.class
        );
    }

    private Integer createApproval(String title) {
        Map<String, Object> approval = dataOf(post(
                "/api/admin/approvals",
                Map.of(
                        "sourceType", "MANUAL",
                        "toolCode", "order.refund_execute",
                        "riskLevel", "high",
                        "title", title
                ),
                operatorHeaders()
        ));
        return ((Number) approval.get("approvalId")).intValue();
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
