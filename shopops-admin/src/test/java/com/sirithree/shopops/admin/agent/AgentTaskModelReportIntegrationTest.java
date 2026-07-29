package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=memory",
                "shopops.model-gateway.planner.enabled=false",
                "shopops.model-gateway.report.enabled=true",
                "shopops.model-gateway.report.provider-code=echo",
                "shopops.model-gateway.report.prompt-code=daily_review.report"
        }
)
class AgentTaskModelReportIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateDailyReviewReportThroughModelGatewayWhenEnabled() {
        Map<String, Object> createData = createDailyReviewTask();
        assertThat(createData.get("status")).isEqualTo("SUCCESS");

        Integer taskId = ((Number) createData.get("taskId")).intValue();
        Map<String, Object> taskData = dataOf(get("/api/agent/tasks/" + taskId));
        Integer reportId = ((Number) taskData.get("reportId")).intValue();

        Map<String, Object> reportData = dataOf(get("/api/reports/" + reportId));
        Map<String, Object> evidence = mapValue(reportData.get("evidence"));
        assertThat(reportData.get("markdown").toString())
                .contains("Echo model response:")
                .contains("请根据以下结构化经营数据生成一份中文 Markdown 每日经营复盘报告");
        assertThat(evidence)
                .containsEntry("generationMode", "MODEL_GATEWAY")
                .containsEntry("modelProviderCode", "echo");
        assertThat(evidence.get("modelCallId")).isNotNull();

        Map<String, Object> modelCallLogs = dataOf(get("/api/admin/model-gateway/call-logs?taskId=" + taskId));
        assertThat(modelCallLogs.get("total")).isEqualTo(1);
        Map<String, Object> modelCall = ((List<Map<String, Object>>) modelCallLogs.get("list")).get(0);
        assertThat(modelCall)
                .containsEntry("providerCode", "echo")
                .containsEntry("promptCode", "daily_review.report")
                .containsEntry("status", "SUCCESS");
    }
}
