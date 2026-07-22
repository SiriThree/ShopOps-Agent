package com.sirithree.shopops.admin.agent;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AgentEvaluationIntegrationTest extends AbstractAgentEvaluationIntegrationTestSupport {
    @Override
    protected String caseResource() {
        return "/evaluation/agent-cases-v1.json";
    }

    @Override
    protected String summaryPrefix() {
        return "agent-eval";
    }
}
