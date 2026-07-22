package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AgentTaskMemoryFlowIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateDailyReviewTaskAndPersistReportInMemoryMode() {
        dataOf(post(
                "/api/admin/organization/shops/1/configs",
                Map.of(
                        "configKey", "refund_rate_warn_threshold",
                        "configValue", "0.08",
                        "valueType", "number"
                )
        ));
        dataOf(post(
                "/api/admin/organization/shops/1/configs",
                Map.of(
                        "configKey", "agent_model_policy",
                        "configValue", "balanced",
                        "valueType", "string"
                )
        ));
        Map<String, Object> savedConfig = dataOf(post(
                "/api/admin/organization/shops/1/configs",
                Map.of(
                        "configKey", "negative_comment_warn_threshold",
                        "configValue", "10",
                        "valueType", "number"
                )
        ));
        assertThat(savedConfig)
                .containsEntry("configKey", "negative_comment_warn_threshold")
                .containsEntry("configValue", "10");

        Map<String, Object> createData = createDailyReviewTask();
        assertThat(createData.get("status")).isEqualTo("SUCCESS");

        Integer taskId = ((Number) createData.get("taskId")).intValue();
        Map<String, Object> taskData = dataOf(get("/api/agent/tasks/" + taskId));
        assertThat(taskData.get("status")).isEqualTo("SUCCESS");
        assertThat(taskData.get("reportId")).isNotNull();

        Map<String, Object> taskPage = dataOf(get("/api/agent/tasks?status=SUCCESS&taskType=daily_review&pageNum=1&pageSize=5"));
        assertThat(((Number) taskPage.get("total")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat((List<Map<String, Object>>) taskPage.get("list"))
                .extracting(task -> task.get("taskId"))
                .contains(taskId);

        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps"));
        assertThat(steps).hasSize(6);
        assertThat(steps).extracting(step -> step.get("status")).containsOnly("SUCCESS");
        assertThat(steps).extracting(step -> step.get("toolCode"))
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates",
                        "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review");

        Integer reportId = ((Number) taskData.get("reportId")).intValue();
        Map<String, Object> reportData = dataOf(get("/api/reports/" + reportId));
        assertThat(reportData.get("markdown").toString())
                .contains("GMV：128936.5")
                .contains("风险评价数：7")
                .contains("平台报表")
                .contains("商品优化清单");
        Map<String, Object> reportEvidence = castMap(reportData.get("evidence"));
        assertThat(castMap(reportEvidence.get("shopConfig")))
                .containsEntry("refundRateWarnThreshold", "0.08")
                .containsEntry("negativeCommentWarnThreshold", "10")
                .containsEntry("agentModelPolicy", "balanced");

        Map<String, Object> traceData = dataOf(get("/api/tasks/" + taskId + "/trace"));
        assertThat((List<Object>) traceData.get("spans")).isNotEmpty();
        assertThat((List<Object>) traceData.get("toolCalls")).hasSize(6);

        Map<String, Object> toolCallPage = dataOf(get("/api/tools/call-logs?taskId=" + taskId + "&status=SUCCESS&pageNum=1&pageSize=2"));
        assertThat(toolCallPage.get("total")).isEqualTo(6);
        assertThat((List<Map<String, Object>>) toolCallPage.get("list")).hasSize(2);

        Map<String, Object> productToolCallPage = dataOf(get("/api/tools/call-logs?taskId=" + taskId + "&toolCode=product.query_candidates"));
        assertThat(productToolCallPage.get("total")).isEqualTo(1);
        assertThat((List<Map<String, Object>>) productToolCallPage.get("list"))
                .extracting(log -> log.get("toolCode"))
                .containsExactly("product.query_candidates");

        Map<String, Object> auditOverview = dataOf(get("/api/admin/audit/overview"));
        assertThat(((Number) auditOverview.get("taskEventTotal")).longValue()).isGreaterThanOrEqualTo(3L);
        assertThat(((Number) auditOverview.get("toolCallTotal")).longValue()).isGreaterThanOrEqualTo(6L);
        assertThat((List<Map<String, Object>>) auditOverview.get("recentTaskEvents")).isNotEmpty();
        assertThat((List<Map<String, Object>>) auditOverview.get("recentToolCalls")).isNotEmpty();

        Map<String, Object> auditTimeline = dataOf(get("/api/admin/audit/timeline?pageNum=1&pageSize=20"));
        assertThat(((Number) auditTimeline.get("total")).longValue()).isGreaterThanOrEqualTo(7L);
        assertThat((List<Map<String, Object>>) auditTimeline.get("list"))
                .extracting(event -> event.get("source"))
                .contains("TASK", "TOOL");

        Map<String, Object> toolAuditTimeline = dataOf(get("/api/admin/audit/timeline?source=TOOL&eventStatus=SUCCESS&pageNum=1&pageSize=10"));
        assertThat(((Number) toolAuditTimeline.get("total")).longValue()).isGreaterThanOrEqualTo(6L);
        assertThat((List<Map<String, Object>>) toolAuditTimeline.get("list"))
                .extracting(event -> event.get("eventType"))
                .containsOnly("TOOL_CALL");
        assertThat((List<Map<String, Object>>) toolAuditTimeline.get("list"))
                .extracting(event -> event.get("resourceType"))
                .containsOnly("tool_call_log");

        Map<String, Object> lowRiskToolAuditTimeline = dataOf(get("/api/admin/audit/timeline?source=TOOL&riskLevel=low&pageNum=1&pageSize=10"));
        assertThat(((Number) lowRiskToolAuditTimeline.get("total")).longValue()).isGreaterThanOrEqualTo(6L);
        assertThat((List<Map<String, Object>>) lowRiskToolAuditTimeline.get("list"))
                .extracting(event -> event.get("riskLevel"))
                .containsOnly("low");

        List<Map<String, Object>> toolAuditEvents = (List<Map<String, Object>>) toolAuditTimeline.get("list");
        String toolAuditResourceId = toolAuditEvents.get(0).get("resourceId").toString();
        Map<String, Object> toolAuditDetail = dataOf(get("/api/admin/audit/timeline/TOOL/" + toolAuditResourceId));
        assertThat((Map<String, Object>) toolAuditDetail.get("event"))
                .containsEntry("source", "TOOL")
                .containsEntry("resourceType", "tool_call_log");
        assertThat((Map<String, Object>) toolAuditDetail.get("resource")).containsKey("toolCallLog");
        assertThat((Map<String, Object>) toolAuditDetail.get("context")).containsKey("taskDetail");
        assertThat(castMap(castMap(toolAuditDetail.get("context")).get("shopConfigSnapshot")))
                .containsEntry("refundRateWarnThreshold", "0.08")
                .containsEntry("negativeCommentWarnThreshold", "10")
                .containsEntry("agentModelPolicy", "balanced");
        assertThat(castMap(castMap(toolAuditDetail.get("context")).get("recentShopConfigChange")))
                .containsEntry("eventType", "ORG_SHOP_CONFIG_SAVED");
        assertThat(castMap(castMap(toolAuditDetail.get("context")).get("recentShopConfigChange")).get("message").toString())
                .contains("negative_comment_warn_threshold");

        Map<String, Object> taskAuditDetail = dataOf(get("/api/admin/audit/timeline/TASK/" + taskId));
        assertThat((Map<String, Object>) taskAuditDetail.get("event"))
                .containsEntry("source", "TASK")
                .containsEntry("resourceType", "agent_task");
        assertThat((Map<String, Object>) taskAuditDetail.get("resource")).containsKey("taskDetail");
        assertThat(castMap(castMap(taskAuditDetail.get("context")).get("shopConfigSnapshot")))
                .containsEntry("refundRateWarnThreshold", "0.08")
                .containsEntry("negativeCommentWarnThreshold", "10")
                .containsEntry("agentModelPolicy", "balanced");
        assertThat(castMap(castMap(taskAuditDetail.get("context")).get("recentShopConfigChange")))
                .containsEntry("eventType", "ORG_SHOP_CONFIG_SAVED");
        assertThat(castMap(castMap(taskAuditDetail.get("context")).get("recentShopConfigChange")).get("message").toString())
                .contains("negative_comment_warn_threshold");

        List<Map<String, Object>> events = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(events).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_STARTED", "TASK_FINISHED");

        Map<String, Object> retryData = dataOf(post("/api/agent/tasks/" + taskId + "/retry"));
        assertThat(retryData.get("status")).isEqualTo("SUCCESS");
        assertThat(retryData.get("taskId")).isNotEqualTo(taskId);

        List<Map<String, Object>> eventsAfterRetry = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(eventsAfterRetry).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_STARTED", "TASK_FINISHED", "TASK_RETRY_REQUESTED");

        Map<String, Object> highRiskAudit = dataOf(get("/api/admin/audit/high-risk"));
        assertThat(((Number) highRiskAudit.get("elevatedRiskTotal")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat((Map<String, Object>) highRiskAudit.get("riskBreakdown")).containsKey("MEDIUM");
        assertThat((List<Map<String, Object>>) highRiskAudit.get("recentElevatedRiskEvents"))
                .extracting(event -> event.get("eventType"))
                .contains("TASK_RETRY_REQUESTED");

        Map<String, Object> elevatedRiskTimeline = dataOf(get("/api/admin/audit/timeline?elevatedRisk=true&pageNum=1&pageSize=20"));
        assertThat(((Number) elevatedRiskTimeline.get("total")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat((List<Map<String, Object>>) elevatedRiskTimeline.get("list"))
                .extracting(event -> event.get("riskLevel"))
                .doesNotContain("LOW", "UNKNOWN")
                .contains("MEDIUM");
        assertThat(((Number) highRiskAudit.get("elevatedRiskTotal")).longValue())
                .isEqualTo(((Number) elevatedRiskTimeline.get("total")).longValue());
        long riskBreakdownTotal = ((Map<String, Object>) highRiskAudit.get("riskBreakdown")).values().stream()
                .mapToLong(value -> ((Number) value).longValue())
                .sum();
        assertThat(riskBreakdownTotal).isEqualTo(((Number) highRiskAudit.get("total")).longValue());

        Map<String, Object> toolAuditExport = dataOf(get("/api/admin/audit/export?source=TOOL&eventStatus=SUCCESS"));
        assertThat(toolAuditExport.get("contentType")).isEqualTo("text/csv");
        assertThat(((Number) toolAuditExport.get("rowCount")).longValue()).isGreaterThanOrEqualTo(12L);
        assertThat((List<String>) toolAuditExport.get("columns"))
                .containsExactly("createdAt", "source", "eventType", "eventStatus", "riskLevel", "userId", "username",
                        "taskId", "traceId", "toolCode", "requestId", "resourceType", "resourceId", "summary");
        assertThat((List<Map<String, Object>>) toolAuditExport.get("rows"))
                .extracting(row -> row.get("source"))
                .containsOnly("TOOL");

        ResponseEntity<String> toolAuditCsv = restTemplate.exchange(
                url("/api/admin/audit/export.csv?source=TOOL&eventStatus=SUCCESS"),
                HttpMethod.GET,
                new HttpEntity<>(null, adminHeaders()),
                String.class
        );
        assertThat(toolAuditCsv.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(toolAuditCsv.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("shopops-audit-")
                .contains(".csv");
        assertThat(toolAuditCsv.getBody())
                .startsWith("createdAt,source,eventType,eventStatus,riskLevel")
                .contains("TOOL,TOOL_CALL,SUCCESS")
                .contains("tool_call_log");

        ResponseEntity<String> elevatedRiskCsv = restTemplate.exchange(
                url("/api/admin/audit/export.csv?elevatedRisk=true"),
                HttpMethod.GET,
                new HttpEntity<>(null, adminHeaders()),
                String.class
        );
        assertThat(elevatedRiskCsv.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(elevatedRiskCsv.getBody())
                .contains("TASK_RETRY_REQUESTED")
                .contains("MEDIUM")
                .doesNotContain(",LOW,")
                .doesNotContain(",UNKNOWN,");

        Integer retryTaskId = ((Number) retryData.get("taskId")).intValue();
        Map<String, Object> retryTaskData = dataOf(get("/api/agent/tasks/" + retryTaskId));
        assertThat(retryTaskData.get("status")).isEqualTo("SUCCESS");
        assertThat(retryTaskData.get("reportId")).isNotNull();
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        headers.set("X-User-Roles", "ADMIN");
        return headers;
    }
}
