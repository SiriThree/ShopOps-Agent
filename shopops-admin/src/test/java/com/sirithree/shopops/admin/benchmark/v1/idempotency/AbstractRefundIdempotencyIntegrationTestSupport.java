package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.fault.DeterministicReliabilityFaultController;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkEnvironment;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkExecutionLevel;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadataFactory;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
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
@Import(AbstractRefundIdempotencyIntegrationTestSupport.Phase3TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractRefundIdempotencyIntegrationTestSupport {
    @Autowired protected ToolGatewayService toolGateway;
    @Autowired protected ApprovalRequestService approvals;
    @Autowired protected ToolCallLogService toolLogs;
    @Autowired protected WriteOperationService writeOperations;
    @Autowired protected RecordingRefundExternalSystem external;
    @Autowired protected DeterministicReliabilityFaultController faults;

    @BeforeEach
    void resetPhase3Infrastructure() {
        external.reset(ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL);
        faults.reset();
    }

    protected EvaluationRecord execute(BenchmarkCase c) {
        RefundIdempotencyBenchmarkExecutor executor = new RefundIdempotencyBenchmarkExecutor(
                toolGateway, approvals, toolLogs, writeOperations, external, faults);
        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.benchmarkType = com.sirithree.shopops.admin.benchmark.v1.BenchmarkType.IDEMPOTENCY;
        request.executionLevel = BenchmarkExecutionLevel.TOOL_GATEWAY;
        request.environment = BenchmarkEnvironment.EXTERNAL_SIMULATED;
        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "1.2.0-phase3-idempotency", "dev", BenchmarkEnvironment.EXTERNAL_SIMULATED,
                BenchmarkExecutionLevel.TOOL_GATEWAY, "SPRING_MEMORY", "N/A", "N/A", "N/A",
                null, null, null, "LOCAL", "memory", "SIMULATED_DELIVERY", 3301L, 3301L);
        metadata.externalSystemMode = c.externalSystemMode;
        return executor.execute(c, request, metadata);
    }

    @TestConfiguration
    static class Phase3TestConfiguration {
        @Bean
        @Primary
        RecordingRefundExternalSystem recordingRefundExternalSystem() {
            return new RecordingRefundExternalSystem();
        }


        @Bean
        @Primary
        DeterministicReliabilityFaultController deterministicReliabilityFaultController() {
            return new DeterministicReliabilityFaultController();
        }

    }
}
