package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCaseLoader;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.report.BenchmarkReportWriter;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkDatasetResources;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkEnvironment;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkExecutionLevel;
import com.sirithree.shopops.admin.benchmark.v1.runtime.BenchmarkRunRequest;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRun;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadata;
import com.sirithree.shopops.admin.benchmark.v1.runtime.EvaluationRunMetadataFactory;
import com.sirithree.shopops.admin.benchmark.v1.runtime.ShopOpsBenchmarkRunner;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unified Phase-1 runner replay for Phase-3 idempotency cases. This is a Tool-Gateway benchmark, not an Agent task run.
 */
class Phase3IdempotencyBenchmarkIntegrationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Autowired ObjectMapper objectMapper;

    @Test
    void idempotencyDatasetFlowsThroughUnifiedRunnerIntoAuditableRecords() throws Exception {
        String split = System.getProperty("shopops.benchmark.split", "dev");
        List<BenchmarkCase> cases = new BenchmarkCaseLoader(objectMapper)
                .loadResource(BenchmarkDatasetResources.resourceFor(BenchmarkType.IDEMPOTENCY, split));

        RefundIdempotencyBenchmarkExecutor idempotencyExecutor = new RefundIdempotencyBenchmarkExecutor(
                toolGateway, approvals, toolLogs, writeOperations, external, faults);
        ShopOpsBenchmarkRunner runner = new ShopOpsBenchmarkRunner(null, null, null, idempotencyExecutor);

        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.benchmarkType = BenchmarkType.IDEMPOTENCY;
        request.datasetSplit = split;
        request.caseId = blankToNull(System.getProperty("shopops.benchmark.caseId"));
        request.scenario = blankToNull(System.getProperty("shopops.benchmark.scenario"));
        request.tag = blankToNull(System.getProperty("shopops.benchmark.tag"));
        request.executionLevel = BenchmarkExecutionLevel.TOOL_GATEWAY;
        request.environment = BenchmarkEnvironment.EXTERNAL_SIMULATED;

        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "1.2.0-phase3-idempotency", split, BenchmarkEnvironment.EXTERNAL_SIMULATED,
                BenchmarkExecutionLevel.TOOL_GATEWAY, "SPRING_MEMORY", "N/A", "N/A", "N/A",
                null, null, null, "LOCAL", "memory", "SIMULATED_DELIVERY", 3301L, 3301L);
        metadata.externalSystemMode = "CASE_DEFINED";

        EvaluationRun run = runner.run(cases, request, metadata);
        assertThat(run.caseExecutions).isNotEmpty();
        for (EvaluationRecord record : run.caseExecutions) {
            assertThat(record.executionStatus).isIn(CaseExecutionStatus.PASSED, CaseExecutionStatus.FAILED);
            assertThat(record.metricBreakdown.logicalWriteRequests).isNotNull();
            assertThat(record.metricBreakdown.externalAttempts).isNotNull();
            assertThat(record.metricBreakdown.actualEffectiveSideEffects).isNotNull();
            assertThat(record.sideEffects).allSatisfy(effect ->
                    assertThat(effect).containsEntry("sourceType", "EXTERNAL_TEST_SYSTEM"));
            assertThat(record.evidenceRefs).isNotEmpty();
        }
        assertThat(run.idempotencyMetrics.logicalWriteRequests).isGreaterThan(0);
        assertThat(run.idempotencyMetrics.actualEffectiveSideEffects).isGreaterThanOrEqualTo(0);

        BenchmarkReportWriter.ReportPaths paths = new BenchmarkReportWriter(objectMapper)
                .write(run, Path.of("target", "benchmark", "phase3-idempotency"));
        assertThat(paths.json()).exists();
        assertThat(paths.markdown()).exists();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
