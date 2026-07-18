package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AgentTaskMemoryFlowIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateDailyReviewTaskAndPersistReportInMemoryMode() {
        Map<String, Object> createData = createDailyReviewTask();
        assertThat(createData.get("status")).isEqualTo("SUCCESS");

        Integer taskId = ((Number) createData.get("taskId")).intValue();
        Map<String, Object> taskData = dataOf(get("/api/agent/tasks/" + taskId));
        assertThat(taskData.get("status")).isEqualTo("SUCCESS");
        assertThat(taskData.get("reportId")).isNotNull();

        Map<String, Object> taskPage = dataOf(get("/api/agent/tasks?status=SUCCESS&taskType=daily_review&pageNum=1&pageSize=5"));
        assertThat(taskPage.get("total")).isEqualTo(1);
        assertThat((List<Map<String, Object>>) taskPage.get("list"))
                .extracting(task -> task.get("taskId"))
                .contains(taskId);

        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps"));
        assertThat(steps).hasSize(4);
        assertThat(steps).extracting(step -> step.get("status")).containsOnly("SUCCESS");
        assertThat(steps).extracting(step -> step.get("toolCode"))
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates", "report.generate_daily_review");

        Integer reportId = ((Number) taskData.get("reportId")).intValue();
        Map<String, Object> reportData = dataOf(get("/api/reports/" + reportId));
        assertThat(reportData.get("markdown").toString())
                .contains("GMV：128936.5")
                .contains("风险评价数：7")
                .contains("商品优化清单");

        Map<String, Object> traceData = dataOf(get("/api/tasks/" + taskId + "/trace"));
        assertThat((List<Object>) traceData.get("spans")).isNotEmpty();
        assertThat((List<Object>) traceData.get("toolCalls")).hasSize(4);

        List<Map<String, Object>> events = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(events).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_STARTED", "TASK_FINISHED");

        Map<String, Object> retryData = dataOf(post("/api/agent/tasks/" + taskId + "/retry"));
        assertThat(retryData.get("status")).isEqualTo("SUCCESS");
        assertThat(retryData.get("taskId")).isNotEqualTo(taskId);

        List<Map<String, Object>> eventsAfterRetry = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(eventsAfterRetry).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_STARTED", "TASK_FINISHED", "TASK_RETRY_REQUESTED");

        Integer retryTaskId = ((Number) retryData.get("taskId")).intValue();
        Map<String, Object> retryTaskData = dataOf(get("/api/agent/tasks/" + retryTaskId));
        assertThat(retryTaskData.get("status")).isEqualTo("SUCCESS");
        assertThat(retryTaskData.get("reportId")).isNotNull();
    }
}
