package com.sirithree.shopops.admin.agent;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=memory",
                "shopops.model-gateway.report.enabled=true",
                "shopops.model-gateway.report.provider-code=echo",
                "shopops.model-gateway.report.prompt-code=daily_review.report"
        }
)
class AgentEvaluationModelIntegrationTest extends AbstractAgentEvaluationIntegrationTestSupport {
    @Override
    protected String caseResource() {
        return "/evaluation/agent-cases-model-v1.json";
    }

    @Override
    protected String summaryPrefix() {
        return "agent-eval-model";
    }
}
