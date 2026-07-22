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
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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

        Map<String, Object> approved = dataOf(post(
                "/api/admin/approvals/" + approvalId + "/approve",
                Map.of(
                        "comment", "Approved for retry",
                        "confirmText", "确认通过"
                ),
                adminHeaders()
        ));
        assertThat(approved).containsEntry("status", "APPROVED");

        Map<String, Object> retryResult = dataOf(post(
                "/api/tools/order.refund_execute/invoke",
                Map.of("shopId", 1, "refundAmount", 1288, "reason", "high value refund", "approvalId", approvalId),
                adminHeaders()
        ));
        Integer retryLogId = ((Number) retryResult.get("toolCallLogId")).intValue();
        assertThat(retryResult)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS")
                .containsEntry("approvalId", approvalId)
                .containsEntry("toolCallLogId", retryLogId);
        Map<String, Object> output = (Map<String, Object>) retryResult.get("data");
        assertThat(output)
                .containsEntry("status", "EXECUTED")
                .containsEntry("approvalId", approvalId)
                .containsEntry("refundAmount", 1288);

        Map<String, Object> successLogPage = dataOf(get(
                "/api/tools/call-logs?status=SUCCESS&toolCode=order.refund_execute",
                adminHeaders()
        ));
        assertThat(successLogPage.get("total")).isEqualTo(1);
        Map<String, Object> successLog = ((List<Map<String, Object>>) successLogPage.get("list")).get(0);
        assertThat(successLog)
                .containsEntry("id", retryLogId)
                .containsEntry("approvalId", approvalId)
                .containsEntry("status", "SUCCESS");

        Map<String, Object> approvalAuditPage = dataOf(get(
                "/api/admin/audit/timeline?source=APPROVAL&toolCode=order.refund_execute&pageNum=1&pageSize=10",
                adminHeaders()
        ));
        assertThat(((Number) approvalAuditPage.get("total")).longValue()).isGreaterThanOrEqualTo(2L);
        List<Map<String, Object>> approvalAuditEvents = (List<Map<String, Object>>) approvalAuditPage.get("list");
        List<Map<String, Object>> currentApprovalAuditEvents = approvalAuditEvents.stream()
                .filter(event -> approvalId.toString().equals(event.get("resourceId")))
                .toList();
        assertThat(currentApprovalAuditEvents).hasSize(2);
        assertThat(currentApprovalAuditEvents)
                .extracting(event -> event.get("eventType"))
                .contains("APPROVAL_CREATED", "APPROVAL_DECIDED");
        assertThat(currentApprovalAuditEvents)
                .extracting(event -> event.get("resourceType"))
                .containsOnly("approval_request");

        Map<String, Object> approvalAuditDetail = dataOf(get(
                "/api/admin/audit/timeline/APPROVAL/" + approvalId,
                adminHeaders()
        ));
        assertThat((Map<String, Object>) approvalAuditDetail.get("event"))
                .containsEntry("source", "APPROVAL")
                .containsEntry("resourceType", "approval_request")
                .containsEntry("resourceId", approvalId.toString());
        assertThat((Map<String, Object>) approvalAuditDetail.get("resource")).containsKey("approvalRequest");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBypassApprovalWhenShopConfigDisablesToolApproval() {
        Map<String, Object> savedConfig = dataOf(post(
                "/api/admin/organization/shops/1/configs",
                Map.of(
                        "configKey", "agent_tool_approval_enabled",
                        "configValue", "false",
                        "valueType", "boolean"
                ),
                adminHeaders()
        ));
        assertThat(savedConfig)
                .containsEntry("configKey", "agent_tool_approval_enabled")
                .containsEntry("configValue", "false");

        Map<String, Object> result = dataOf(post(
                "/api/tools/order.refund_execute/invoke",
                Map.of("shopId", 1, "refundAmount", 1288, "reason", "shop policy disabled approval"),
                adminHeaders()
        ));

        Integer toolCallLogId = ((Number) result.get("toolCallLogId")).intValue();
        assertThat(result)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS")
                .containsEntry("toolCallLogId", toolCallLogId);
        assertThat(result.get("approvalId")).isNull();
        Map<String, Object> output = (Map<String, Object>) result.get("data");
        assertThat(output)
                .containsEntry("status", "EXECUTED")
                .containsEntry("approvalId", null)
                .containsEntry("refundAmount", 1288);

        Map<String, Object> approvalPage = dataOf(get(
                "/api/admin/approvals?status=PENDING&toolCode=order.refund_execute",
                adminHeaders()
        ));
        assertThat(approvalPage.get("total")).isEqualTo(0);

        Map<String, Object> logPage = dataOf(get(
                "/api/tools/call-logs?status=SUCCESS&toolCode=order.refund_execute",
                adminHeaders()
        ));
        assertThat(logPage.get("total")).isEqualTo(1);
        Map<String, Object> log = ((List<Map<String, Object>>) logPage.get("list")).get(0);
        assertThat(log)
                .containsEntry("id", toolCallLogId)
                .containsEntry("approvalId", null)
                .containsEntry("riskLevel", "HIGH")
                .containsEntry("errorCode", "APPROVAL_BYPASSED_BY_SHOP_CONFIG");
        assertThat(log.get("errorMessage").toString()).contains("agent_tool_approval_enabled=false");

        Map<String, Object> auditPage = dataOf(get(
                "/api/admin/audit/timeline?source=TOOL&toolCode=order.refund_execute&pageNum=1&pageSize=10",
                adminHeaders()
        ));
        List<Map<String, Object>> events = (List<Map<String, Object>>) auditPage.get("list");
        Map<String, Object> event = events.stream()
                .filter(item -> toolCallLogId.toString().equals(item.get("resourceId")))
                .findFirst()
                .orElseThrow();
        assertThat(event)
                .containsEntry("source", "TOOL")
                .containsEntry("eventStatus", "SUCCESS")
                .containsEntry("riskLevel", "HIGH");
        Map<String, Object> detail = (Map<String, Object>) event.get("detail");
        assertThat(detail)
                .containsEntry("errorCode", "APPROVAL_BYPASSED_BY_SHOP_CONFIG");
        assertThat(detail.get("errorMessage").toString()).contains("agent_tool_approval_enabled=false");

        Map<String, Object> auditDetail = dataOf(get(
                "/api/admin/audit/timeline/TOOL/" + toolCallLogId,
                adminHeaders()
        ));
        Map<String, Object> context = (Map<String, Object>) auditDetail.get("context");
        assertThat((Map<String, Object>) auditDetail.get("event"))
                .containsEntry("source", "TOOL")
                .containsEntry("resourceId", toolCallLogId.toString());
        assertThat((Map<String, Object>) context.get("recentShopConfigChange"))
                .containsEntry("eventType", "ORG_SHOP_CONFIG_SAVED");
        assertThat(context.get("recentShopConfigChange").toString()).contains("agent_tool_approval_enabled");
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
