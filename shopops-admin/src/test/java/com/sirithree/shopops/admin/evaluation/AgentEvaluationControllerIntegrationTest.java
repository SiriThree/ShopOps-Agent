package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AgentEvaluationControllerIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Test
    void shouldReadStoredNaturalLanguageBatchSummary() {
        Map<String, Object> result = dataOf(get("/api/admin/evaluation/agent-natural-language-batch"));

        assertThat(result)
                .containsEntry("available", true)
                .containsKey("summaryPath")
                .containsKey("summary");

        Map<String, Object> summary = castMap(result.get("summary"));
        assertThat(summary)
                .containsEntry("evaluationName", "shopops-agent-natural-language-batch-v1")
                .containsEntry("caseCount", 280)
                .containsEntry("toolInvocationCount", 1260);
    }
}
