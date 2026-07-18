package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfSystemProperty(named = "shopops.jdbc.it", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=jdbc",
                "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                "spring.datasource.username=root",
                "spring.datasource.password=root"
        }
)
class AgentTaskJdbcFlowIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateDailyReviewTaskFromJdbcBusinessData() {
        Map<String, Object> createData = createDailyReviewTask();
        assertThat(createData.get("status")).isEqualTo("SUCCESS");

        Integer taskId = ((Number) createData.get("taskId")).intValue();
        Map<String, Object> taskData = dataOf(get("/api/agent/tasks/" + taskId));
        assertThat(taskData.get("status")).isEqualTo("SUCCESS");
        assertThat(taskData.get("reportId")).isNotNull();

        Map<String, Object> taskPage = dataOf(get("/api/agent/tasks?status=SUCCESS&taskType=daily_review&pageNum=1&pageSize=10"));
        assertThat(((Number) taskPage.get("total")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat((List<Map<String, Object>>) taskPage.get("list"))
                .extracting(task -> task.get("taskId"))
                .contains(taskId);

        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps"));
        assertThat(steps).hasSize(4);
        assertThat(steps).extracting(step -> step.get("status")).containsOnly("SUCCESS");

        Map<String, Object> orderOutput = stepOutput(steps, "order.query_summary");
        assertThat(orderOutput.get("gmv")).isEqualTo(840.0);
        assertThat(orderOutput.get("orderCount")).isEqualTo(5);
        assertThat(orderOutput.get("refundAmount")).isEqualTo(59.0);
        assertThat(orderOutput.get("refundRate")).isEqualTo(0.0702);

        Map<String, Object> commentOutput = stepOutput(steps, "comment.query_negative");
        assertThat(commentOutput.get("negativeCount")).isEqualTo(3);
        assertThat((List<Map<String, Object>>) commentOutput.get("riskComments"))
                .extracting(comment -> comment.get("commentId"))
                .contains(50101, 50102, 50103);

        Map<String, Object> productOutput = stepOutput(steps, "product.query_candidates");
        assertThat(productOutput.get("candidateCount")).isEqualTo(3);
        assertThat((List<Map<String, Object>>) productOutput.get("products"))
                .extracting(product -> product.get("productId"))
                .containsExactly(1016, 1001, 1008);

        Integer reportId = ((Number) taskData.get("reportId")).intValue();
        Map<String, Object> reportData = dataOf(get("/api/reports/" + reportId));
        assertThat(reportData.get("markdown").toString())
                .contains("GMV：840")
                .contains("退款率：7.02%")
                .contains("风险评价数：3");
        Map<String, Object> evidence = castMap(reportData.get("evidence"));
        assertThat((List<Object>) evidence.get("riskCommentIds")).contains(50101, 50102, 50103);
        assertThat((List<Object>) evidence.get("productIds")).containsExactly(1016, 1001, 1008);
        assertThat(reportData.get("createdBy")).isEqualTo(1);
        assertThat(reportData.get("createdAt")).isNotNull();

        Map<String, Object> traceData = dataOf(get("/api/tasks/" + taskId + "/trace"));
        assertThat((List<Object>) traceData.get("spans")).hasSizeGreaterThanOrEqualTo(7);
        assertThat((List<Object>) traceData.get("toolCalls")).hasSize(4);

        String taskNo = taskData.get("taskNo").toString();
        String traceId = taskData.get("traceId").toString();
        String reportNo = reportData.get("reportNo").toString();
        Map<String, Object> reportPage = dataOf(get("/api/reports?taskId=" + taskId
                + "&reportNo=" + reportNo
                + "&reportType=daily_review"
                + "&traceId=" + traceId
                + "&status=SUCCESS"
                + "&createdBy=1&pageNum=1&pageSize=10"));
        assertThat(reportPage.get("total")).isEqualTo(1);
        assertThat((List<Map<String, Object>>) reportPage.get("list"))
                .extracting(report -> report.get("reportId"))
                .containsExactly(reportId);

        Map<String, Object> adminTaskPage = dataOf(get("/api/admin/agent/tasks?taskNo=" + taskNo
                + "&traceId=" + traceId
                + "&reportId=" + reportId
                + "&userId=1&pageNum=1&pageSize=10"));
        assertThat(adminTaskPage.get("total")).isEqualTo(1);
        assertThat((List<Map<String, Object>>) adminTaskPage.get("list"))
                .extracting(task -> task.get("taskId"))
                .containsExactly(taskId);

        Map<String, Object> adminDetail = dataOf(get("/api/admin/agent/tasks/" + taskId + "/detail"));
        assertThat(castMap(adminDetail.get("task")).get("taskId")).isEqualTo(taskId);
        assertThat((List<Object>) adminDetail.get("steps")).hasSize(4);
        assertThat((List<Object>) adminDetail.get("events")).isNotEmpty();
        assertThat(castMap(adminDetail.get("report")).get("reportId")).isEqualTo(reportId);
        assertThat((List<Object>) adminDetail.get("spans")).hasSizeGreaterThanOrEqualTo(7);
        assertThat((List<Object>) adminDetail.get("toolCalls")).hasSize(4);

        Map<String, Object> adminMetrics = dataOf(get("/api/admin/agent/tasks/metrics"));
        assertThat(((Number) adminMetrics.get("total")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat(((Number) adminMetrics.get("success")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat(((Number) adminMetrics.get("successRate")).doubleValue()).isGreaterThan(0.0d);
        assertThat(((Number) adminMetrics.get("avgLatencyMs")).longValue()).isGreaterThanOrEqualTo(0L);
        assertThat(castMap(adminMetrics.get("statusBreakdown"))).containsKey("SUCCESS");

        Map<String, Object> dashboardSummary = dataOf(get("/api/admin/dashboard/summary"));
        Map<String, Object> dashboardTaskMetrics = castMap(dashboardSummary.get("taskMetrics"));
        assertThat(((Number) dashboardTaskMetrics.get("success")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat(((Number) dashboardSummary.get("reportTotal")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat(((Number) dashboardSummary.get("toolCallTotal")).longValue()).isGreaterThanOrEqualTo(4L);
        assertThat(((Number) dashboardSummary.get("toolCallFailed")).longValue()).isGreaterThanOrEqualTo(0L);
        assertThat((List<Object>) dashboardSummary.get("recentFailedEvents")).isNotNull();
        assertThat(dashboardSummary.get("generatedAt")).isNotNull();

        Map<String, Object> toolCallPage = dataOf(get("/api/tools/call-logs?taskId=" + taskId + "&status=SUCCESS&pageNum=1&pageSize=2"));
        assertThat(toolCallPage.get("total")).isEqualTo(4);
        assertThat((List<Map<String, Object>>) toolCallPage.get("list")).hasSize(2);

        Map<String, Object> productToolCallPage = dataOf(get("/api/tools/call-logs?taskId=" + taskId + "&toolCode=product.query_candidates"));
        assertThat(productToolCallPage.get("total")).isEqualTo(1);
        assertThat((List<Map<String, Object>>) productToolCallPage.get("list"))
                .extracting(log -> log.get("toolCode"))
                .containsExactly("product.query_candidates");

        Map<String, Object> healthData = dataOf(get("/api/system/health"));
        Map<String, Object> checks = castMap(healthData.get("checks"));
        Map<String, Object> database = castMap(checks.get("database"));
        Map<String, Object> flyway = castMap(checks.get("flyway"));
        Map<String, Object> redis = castMap(checks.get("redis"));
        Map<String, Object> rabbitmq = castMap(checks.get("rabbitmq"));
        assertThat(healthData.get("status")).isEqualTo("UP");
        assertThat(healthData.get("persistence")).isEqualTo("jdbc");
        assertThat(database.get("status")).isEqualTo("UP");
        assertThat(database.get("mode")).isEqualTo("REQUIRED");
        assertThat(flyway.get("status")).isEqualTo("UP");
        assertThat(flyway.get("version")).isNotNull();
        assertThat(redis.get("status")).isEqualTo("UP");
        assertThat(redis.get("ping")).isEqualTo("PONG");
        assertThat(rabbitmq.get("status")).isEqualTo("UP");
        assertThat(rabbitmq.get("open")).isEqualTo(true);

        List<Map<String, Object>> events = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(events).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_STARTED", "TASK_FINISHED");
        assertThat(events)
                .extracting(event -> castMap(event.get("eventData")).get("traceId"))
                .containsOnly(traceId);

        Map<String, Object> adminEventPage = dataOf(get("/api/admin/agent/tasks/events?taskId=" + taskId
                + "&eventType=TASK_FINISHED&operatorId=1&pageNum=1&pageSize=10"));
        assertThat(adminEventPage.get("total")).isEqualTo(1);
        Map<String, Object> finishedEvent = ((List<Map<String, Object>>) adminEventPage.get("list")).get(0);
        assertThat(finishedEvent.get("eventType")).isEqualTo("TASK_FINISHED");
        assertThat(castMap(finishedEvent.get("eventData")).get("reportId")).isEqualTo(reportId);

        Map<String, Object> retryData = dataOf(post("/api/agent/tasks/" + taskId + "/retry"));
        assertThat(retryData.get("status")).isEqualTo("SUCCESS");
        assertThat(retryData.get("taskId")).isNotEqualTo(taskId);

        List<Map<String, Object>> eventsAfterRetry = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(eventsAfterRetry).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_STARTED", "TASK_FINISHED", "TASK_RETRY_REQUESTED");
        Map<String, Object> retryEventPage = dataOf(get("/api/admin/agent/tasks/events?taskId=" + taskId
                + "&eventType=TASK_RETRY_REQUESTED&pageNum=1&pageSize=10"));
        assertThat(retryEventPage.get("total")).isEqualTo(1);
        assertThat(castMap(((List<Map<String, Object>>) retryEventPage.get("list")).get(0).get("eventData")).get("taskNo"))
                .isEqualTo(taskNo);

        Integer retryTaskId = ((Number) retryData.get("taskId")).intValue();
        Map<String, Object> retryTaskData = dataOf(get("/api/agent/tasks/" + retryTaskId));
        assertThat(retryTaskData.get("status")).isEqualTo("SUCCESS");
        assertThat(retryTaskData.get("reportId")).isNotNull();
    }

    private Map<String, Object> stepOutput(List<Map<String, Object>> steps, String toolCode) {
        return steps.stream()
                .filter(step -> toolCode.equals(step.get("toolCode")))
                .findFirst()
                .map(step -> mapValue(step.get("output")))
                .orElseThrow();
    }
}
