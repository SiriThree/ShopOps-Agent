package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.benchmark.v1.fault.DeterministicReliabilityFaultController;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSystemMode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RecordingRefundExternalSystem;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RefundIdempotencyBenchmarkExecutor;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkEnvironment;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkExecutionLevel;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadataFactory;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Real JDBC idempotency path. Disabled unless explicitly requested because it requires the project's MySQL integration
 * database. The benchmark result must never be described as JDBC-verified unless this class actually runs.
 */
@EnabledIfSystemProperty(named = "shopops.jdbc.it", matches = "true")
@SpringBootTest(properties = {
        "shopops.persistence=jdbc",
        "shopops.agent.dispatch-mode=sync",
        "shopops.mcp.servers.commerce.enabled=false",
        "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
        "spring.datasource.username=root",
        "spring.datasource.password=root",
        "spring.datasource.hikari.initialization-fail-timeout=1",
        "spring.datasource.hikari.connection-timeout=3000"
})
@Import(JdbcRefundIdempotencyIntegrationTest.JdbcPhase3Infrastructure.class)
class JdbcRefundIdempotencyIntegrationTest {
    @Autowired ToolGatewayService toolGateway;
    @Autowired ApprovalRequestService approvals;
    @Autowired ToolCallLogService toolLogs;
    @Autowired WriteOperationService writeOperations;
    @Autowired RecordingRefundExternalSystem external;
    @Autowired DeterministicReliabilityFaultController faults;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanPersistentBenchmarkRows() {
        jdbcTemplate.update("DELETE FROM outbox_event WHERE aggregate_type='WRITE_OPERATION' AND CAST(aggregate_id AS UNSIGNED) IN (SELECT id FROM write_operation WHERE task_id >= 930000)");
        jdbcTemplate.update("DELETE FROM write_operation WHERE task_id >= 930000");
        jdbcTemplate.update("DELETE FROM tool_call_log WHERE task_id >= 930000");
        jdbcTemplate.update("DELETE FROM approval_request WHERE task_id >= 930000");
    }

    @Test
    void repeatedRefundDeliveriesUseJdbcUniquenessAndCreateOneExternalEffect() {
        BenchmarkCase c = IdempotencyTestCases.refund("jdbc-idem-" + UUID.randomUUID(), 4, 4);
        c.input.put("orderId", "SO202607180001");
        c.input.put("refundAmount", 4);
        c.input.put("operationRequestId", "REQ-" + UUID.randomUUID());
        external.reset(ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL);
        faults.reset();
        RefundIdempotencyBenchmarkExecutor executor = new RefundIdempotencyBenchmarkExecutor(
                toolGateway, approvals, toolLogs, writeOperations, external, faults);
        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.benchmarkType = BenchmarkType.IDEMPOTENCY;
        request.executionLevel = BenchmarkExecutionLevel.TOOL_GATEWAY;
        request.environment = BenchmarkEnvironment.JDBC_INTEGRATION;
        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "1.2.0-phase3-idempotency", "jdbc", BenchmarkEnvironment.JDBC_INTEGRATION,
                BenchmarkExecutionLevel.TOOL_GATEWAY, "SPRING_JDBC", "N/A", "N/A", "N/A",
                null, null, null, "LOCAL", "jdbc-mysql", "SIMULATED_DELIVERY", 3301L, 3301L);
        metadata.externalSystemMode = ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL.name();

        EvaluationRecord record = executor.execute(c, request, metadata);

        assertThat(record.metricBreakdown.actualEffectiveSideEffects)
                .as("record observedFacts=%s failureReasons=%s approvalEvents=%s toolAttempts=%s",
                        record.observedFacts, record.failureReasons, record.approvalEvents, record.toolAttempts)
                .isEqualTo(1);
        assertThat(record.metricBreakdown.duplicateSideEffects).isZero();
        assertThat(writeOperations.listByTaskId(1L, 1L, record.taskId)).hasSize(1);
    }

    @TestConfiguration
    static class JdbcPhase3Infrastructure {
        @Bean @Primary
        RecordingRefundExternalSystem recordingRefundExternalSystem() {
            return new RecordingRefundExternalSystem();
        }

        @Bean @Primary
        DeterministicReliabilityFaultController deterministicReliabilityFaultController() {
            return new DeterministicReliabilityFaultController();
        }
    }
}
