package com.sirithree.shopops.admin.agent;

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
class AgentTaskMemoryFlowIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateDailyReviewTaskAndPersistReportInMemoryMode() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");

        Map<String, Object> request = Map.of(
                "taskType", "daily_review",
                "userInput", "帮我生成今天店铺运营复盘",
                "dateRange", Map.of("start", "2026-07-18", "end", "2026-07-18")
        );

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                url("/api/agent/tasks"),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertThat(createResponse.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> createBody = createResponse.getBody();
        assertThat(createBody).isNotNull();
        assertThat(createBody.get("code")).isEqualTo(200);
        Map<String, Object> createData = (Map<String, Object>) createBody.get("data");
        assertThat(createData.get("status")).isEqualTo("SUCCESS");

        Integer taskId = ((Number) createData.get("taskId")).intValue();
        Map<String, Object> taskData = dataOf(get("/api/agent/tasks/" + taskId));
        assertThat(taskData.get("status")).isEqualTo("SUCCESS");
        assertThat(taskData.get("reportId")).isNotNull();

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
    }

    private Map get(String path) {
        ResponseEntity<Map> response = restTemplate.getForEntity(url(path), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(Map response) {
        return (Map<String, Object>) dataOfObject(response);
    }

    private Object dataOfObject(Map response) {
        assertThat(response).isNotNull();
        assertThat(response.get("code")).isEqualTo(200);
        return response.get("data");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
