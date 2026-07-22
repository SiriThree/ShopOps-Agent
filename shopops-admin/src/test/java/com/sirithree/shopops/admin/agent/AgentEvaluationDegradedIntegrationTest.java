package com.sirithree.shopops.admin.agent;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=memory",
                "shopops.tool.fail-code=comment.query_negative"
        }
)
class AgentEvaluationDegradedIntegrationTest extends AbstractAgentEvaluationIntegrationTestSupport {
    @Override
    protected String caseResource() {
        return "/evaluation/agent-cases-degraded-v1.json";
    }

    @Override
    protected String summaryPrefix() {
        return "agent-eval-degraded";
    }
}
