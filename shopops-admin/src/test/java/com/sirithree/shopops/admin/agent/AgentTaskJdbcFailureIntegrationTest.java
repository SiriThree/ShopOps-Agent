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
                "shopops.tool.fail-code=order.query_summary",
                "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                "spring.datasource.username=root",
                "spring.datasource.password=root",
                "spring.datasource.hikari.initialization-fail-timeout=1",
                "spring.datasource.hikari.connection-timeout=3000"
        }
)
class AgentTaskJdbcFailureIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistFailureEvidenceWhenCriticalToolFails() {
        Map<String, Object> createData = createDailyReviewTask();
        assertThat(createData.get("status")).isEqualTo("FAILED");

        Integer taskId = ((Number) createData.get("taskId")).intValue();
        Map<String, Object> taskData = dataOf(get("/api/agent/tasks/" + taskId));
        assertThat(taskData.get("status")).isEqualTo("FAILED");
        assertThat(taskData.get("reportId")).isNull();
        assertThat(taskData.get("errorMessage").toString()).contains("Simulated tool failure: order.query_summary");

        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps"));
        assertThat(steps).extracting(step -> step.get("status"))
                .containsExactly("FAILED", "PENDING", "PENDING", "PENDING");
        assertThat(steps.get(0).get("toolCode")).isEqualTo("order.query_summary");
        assertThat(steps.get(0).get("errorMessage").toString()).contains("Simulated tool failure");

        List<Map<String, Object>> events = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(events).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_STARTED", "TASK_FAILED");

        Map<String, Object> traceData = dataOf(get("/api/tasks/" + taskId + "/trace"));
        assertThat((List<Map<String, Object>>) traceData.get("spans"))
                .extracting(span -> span.get("status"))
                .contains("FAILED");

        Map<String, Object> failedToolCalls = dataOf(get("/api/tools/call-logs?taskId=" + taskId + "&status=FAILED"));
        assertThat(failedToolCalls.get("total")).isEqualTo(1);
        assertThat((List<Map<String, Object>>) failedToolCalls.get("list"))
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.get("toolCode")).isEqualTo("order.query_summary");
                    assertThat(log.get("errorCode")).isEqualTo("TOOL_FAILURE_INJECTED");
                    assertThat(log.get("errorMessage").toString()).contains("Simulated tool failure");
                });
    }
}
