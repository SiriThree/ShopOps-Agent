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
                .contains("风险评价数：3")
                .contains("运动毛巾");
        Map<String, Object> evidence = castMap(reportData.get("evidence"));
        assertThat((List<Object>) evidence.get("riskCommentIds")).contains(50101, 50102, 50103);
        assertThat((List<Object>) evidence.get("productIds")).containsExactly(1016, 1001, 1008);

        Map<String, Object> traceData = dataOf(get("/api/tasks/" + taskId + "/trace"));
        assertThat((List<Object>) traceData.get("spans")).hasSizeGreaterThanOrEqualTo(7);
        assertThat((List<Object>) traceData.get("toolCalls")).hasSize(4);

        Map<String, Object> healthData = dataOf(get("/api/system/health"));
        Map<String, Object> checks = castMap(healthData.get("checks"));
        Map<String, Object> database = castMap(checks.get("database"));
        assertThat(healthData.get("status")).isEqualTo("UP");
        assertThat(healthData.get("persistence")).isEqualTo("jdbc");
        assertThat(database.get("status")).isEqualTo("UP");
        assertThat(database.get("mode")).isEqualTo("REQUIRED");

        Map<String, Object> retryData = dataOf(post("/api/agent/tasks/" + taskId + "/retry"));
        assertThat(retryData.get("status")).isEqualTo("SUCCESS");
        assertThat(retryData.get("taskId")).isNotEqualTo(taskId);

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
