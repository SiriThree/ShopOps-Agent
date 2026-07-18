package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDispatchMessage;
import com.sirithree.shopops.admin.agent.service.impl.JdbcAgentTaskExecutionWorker;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfSystemProperty(named = "shopops.rabbitmq.it", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=jdbc",
                "shopops.agent.dispatch-mode=rabbitmq",
                "shopops.agent.rabbitmq.queue=shopops.agent.task.execute.it.${random.uuid}",
                "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                "spring.datasource.username=root",
                "spring.datasource.password=root"
        }
)
class AgentTaskRabbitDispatchIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Autowired
    private JdbcAgentTaskExecutionWorker executionWorker;

    @Test
    @SuppressWarnings("unchecked")
    void shouldDispatchAndExecuteTaskViaRabbitMq() throws InterruptedException {
        Map<String, Object> createData = createDailyReviewTask();
        assertThat(createData.get("status")).isEqualTo("QUEUED");

        Integer taskId = ((Number) createData.get("taskId")).intValue();
        Map<String, Object> taskData = awaitTaskStatus(taskId, "SUCCESS", Duration.ofSeconds(10));
        assertThat(taskData.get("reportId")).isNotNull();

        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps"));
        assertThat(steps).hasSize(4);
        assertThat(steps).extracting(step -> step.get("status")).containsOnly("SUCCESS");

        List<Map<String, Object>> events = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(events).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_QUEUED", "TASK_STARTED", "TASK_FINISHED");

        executionWorker.execute(duplicateMessage(createData));
        List<Map<String, Object>> eventsAfterDuplicate = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/events"));
        assertThat(eventsAfterDuplicate).hasSize(events.size());
        assertThat(eventsAfterDuplicate).extracting(event -> event.get("eventType"))
                .containsExactly("TASK_CREATED", "TASK_QUEUED", "TASK_STARTED", "TASK_FINISHED");
    }

    private Map<String, Object> awaitTaskStatus(Integer taskId, String expectedStatus, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        Map<String, Object> taskData = dataOf(get("/api/agent/tasks/" + taskId));
        while (!expectedStatus.equals(taskData.get("status")) && System.nanoTime() < deadline) {
            Thread.sleep(250L);
            taskData = dataOf(get("/api/agent/tasks/" + taskId));
        }
        assertThat(taskData.get("status")).isEqualTo(expectedStatus);
        return taskData;
    }

    private AgentTaskDispatchMessage duplicateMessage(Map<String, Object> createData) {
        AgentTaskDispatchMessage message = new AgentTaskDispatchMessage();
        message.setTenantId(1L);
        message.setShopId(1L);
        message.setUserId(1L);
        message.setTaskId(((Number) createData.get("taskId")).longValue());
        message.setTraceId(createData.get("traceId").toString());
        return message;
    }
}
