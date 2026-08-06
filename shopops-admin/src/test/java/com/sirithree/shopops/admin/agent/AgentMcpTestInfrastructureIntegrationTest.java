package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.mcp.support.InMemoryCommerceMcpClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AgentMcpTestInfrastructureIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Autowired
    private InMemoryCommerceMcpClient mcpClient;

    @BeforeEach
    void resetMcpEvidence() {
        mcpClient.reset();
    }

    @Test
    void shouldExecuteMcpCatalogEntryThroughTestScopeClientWithoutExternalProcess() {
        Map<String, Object> created = createDailyReviewTask();

        assertThat(created.get("status")).isEqualTo("SUCCESS");
        assertThat(mcpClient.discoveryCallCount()).isEqualTo(1);
        assertThat(mcpClient.toolCallCount()).isEqualTo(1);
    }
}
