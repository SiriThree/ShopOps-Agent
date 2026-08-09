package com.sirithree.shopops.admin.benchmark.v1.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCaseLoader;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSystemMode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RecordingRefundExternalSystem;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkDatasetResources;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkEnvironment;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkExecutionLevel;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadataFactory;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
        "shopops.persistence=memory",
        "shopops.agent.dispatch-mode=sync",
        "shopops.mcp.servers.commerce.enabled=false"
})
@Import(AbstractGovernanceIntegrationTestSupport.Phase5TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractGovernanceIntegrationTestSupport {
    @Autowired protected ToolGatewayService toolGateway;
    @Autowired protected ApprovalRequestService approvals;
    @Autowired protected WriteOperationService writeOperations;
    @Autowired protected RecordingRefundExternalSystem external;
    @Autowired protected GovernanceAuthorizationFixture authorization;
    @Autowired protected ObjectMapper objectMapper;

    @BeforeEach
    void resetGovernanceInfrastructure() {
        external.reset(ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL);
        authorization.reset();
    }

    protected EvaluationRecord executeCase(String split, String caseId) throws Exception {
        BenchmarkCase c = new BenchmarkCaseLoader(objectMapper)
                .loadResource(BenchmarkDatasetResources.resourceFor(BenchmarkType.GOVERNANCE, split))
                .stream().filter(item -> caseId.equals(item.caseId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Governance case not found: " + caseId));
        RefundGovernanceBenchmarkExecutor executor = new RefundGovernanceBenchmarkExecutor(
                toolGateway, approvals, writeOperations, external, authorization);
        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.benchmarkType = BenchmarkType.GOVERNANCE;
        request.datasetSplit = split;
        request.caseId = caseId;
        request.executionLevel = BenchmarkExecutionLevel.TOOL_GATEWAY;
        request.environment = BenchmarkEnvironment.EXTERNAL_SIMULATED;
        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "1.4.0-phase5-governance", split, BenchmarkEnvironment.EXTERNAL_SIMULATED,
                BenchmarkExecutionLevel.TOOL_GATEWAY, "SPRING_MEMORY", "N/A", "N/A", "N/A",
                null, null, null, "LOCAL", "memory", "DIRECT_TOOL_GATEWAY", 5501L, 5501L);
        metadata.externalSystemMode = "NON_IDEMPOTENT_EXTERNAL";
        metadata.authorizationMode = "AUTHORIZATION_FIXTURE";
        return executor.execute(c, request, metadata);
    }

    protected List<BenchmarkCase> loadGovernance(String split) throws Exception {
        return new BenchmarkCaseLoader(objectMapper)
                .loadResource(BenchmarkDatasetResources.resourceFor(BenchmarkType.GOVERNANCE, split));
    }

    @TestConfiguration
    static class Phase5TestConfiguration {
        @Bean @Primary GovernanceAuthorizationFixture governanceAuthorizationFixture() {
            return new GovernanceAuthorizationFixture();
        }
        @Bean @Primary RecordingRefundExternalSystem recordingRefundExternalSystem() {
            return new RecordingRefundExternalSystem();
        }
    }
}
