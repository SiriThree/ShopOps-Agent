package com.sirithree.shopops.admin.benchmark.v1.recovery;

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

class Phase4RecoveryBenchmarkIntegrationTest extends AbstractRefundRecoveryIntegrationTestSupport {
    @Autowired ObjectMapper objectMapper;

    @Test
    void recoveryDatasetFlowsThroughUnifiedRunner() throws Exception {
        String split = System.getProperty("shopops.benchmark.split", "dev");
        List<BenchmarkCase> cases = new BenchmarkCaseLoader(objectMapper)
                .loadResource(BenchmarkDatasetResources.resourceFor(BenchmarkType.RECOVERY, split));
        RefundRecoveryBenchmarkExecutor recoveryExecutor = new RefundRecoveryBenchmarkExecutor(
                toolGateway, approvals, writeOperations, reconciliation, external, faults);
        ShopOpsBenchmarkRunner runner = new ShopOpsBenchmarkRunner(null, null, null, null, recoveryExecutor);

        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.benchmarkType = BenchmarkType.RECOVERY;
        request.datasetSplit = split;
        request.caseId = blankToNull(System.getProperty("shopops.benchmark.caseId"));
        request.scenario = blankToNull(System.getProperty("shopops.benchmark.scenario"));
        request.tag = blankToNull(System.getProperty("shopops.benchmark.tag"));
        request.executionLevel = BenchmarkExecutionLevel.TOOL_GATEWAY;
        request.environment = BenchmarkEnvironment.EXTERNAL_SIMULATED;

        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "1.3.0-phase4-recovery", split, BenchmarkEnvironment.EXTERNAL_SIMULATED,
                BenchmarkExecutionLevel.TOOL_GATEWAY, "SPRING_MEMORY", "N/A", "N/A", "N/A",
                null, null, null, "LOCAL", "memory", "DIRECT_RECOVERY", 4401L, 4401L);
        metadata.externalSystemMode = "NON_IDEMPOTENT_EXTERNAL";

        EvaluationRun run = runner.run(cases, request, metadata);
        assertThat(run.caseExecutions).isNotEmpty();
        for (EvaluationRecord record : run.caseExecutions) {
            assertThat(record.executionStatus).isIn(CaseExecutionStatus.PASSED, CaseExecutionStatus.FAILED);
            assertThat(record.metricBreakdown.terminalStateReached).isNotNull();
            assertThat(record.metricBreakdown.localStateConsistentWithExternalReality).isNotNull();
            assertThat(record.metricBreakdown.converged).isNotNull();
            assertThat(record.observedFacts).containsKeys("externalReality", "localState");
            assertThat(record.evidenceRefs).isNotEmpty();
        }
        assertThat(run.recoveryMetrics.faultCases).isGreaterThan(0);

        BenchmarkReportWriter.ReportPaths paths = new BenchmarkReportWriter(objectMapper)
                .write(run, Path.of("target", "benchmark", "phase4-recovery"));
        assertThat(paths.json()).exists();
        assertThat(paths.markdown()).exists();
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
}
