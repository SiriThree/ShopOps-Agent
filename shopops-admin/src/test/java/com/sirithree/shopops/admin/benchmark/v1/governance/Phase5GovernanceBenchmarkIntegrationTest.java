package com.sirithree.shopops.admin.benchmark.v1.governance;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.report.BenchmarkReportWriter;
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

class Phase5GovernanceBenchmarkIntegrationTest extends AbstractGovernanceIntegrationTestSupport {
    @Test
    void governanceDatasetFlowsThroughUnifiedRunner() throws Exception {
        String split = System.getProperty("shopops.benchmark.split", "dev");
        List<BenchmarkCase> cases = loadGovernance(split);
        RefundGovernanceBenchmarkExecutor executor = new RefundGovernanceBenchmarkExecutor(
                toolGateway, approvals, writeOperations, external, authorization);
        ShopOpsBenchmarkRunner runner = new ShopOpsBenchmarkRunner(null, null, null, null, null, executor);
        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.benchmarkType = BenchmarkType.GOVERNANCE;
        request.datasetSplit = split;
        request.caseId = blankToNull(System.getProperty("shopops.benchmark.caseId"));
        request.scenario = blankToNull(System.getProperty("shopops.benchmark.scenario"));
        request.tag = blankToNull(System.getProperty("shopops.benchmark.tag"));
        request.executionLevel = BenchmarkExecutionLevel.TOOL_GATEWAY;
        request.environment = BenchmarkEnvironment.EXTERNAL_SIMULATED;
        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "1.4.0-phase5-governance", split, BenchmarkEnvironment.EXTERNAL_SIMULATED,
                BenchmarkExecutionLevel.TOOL_GATEWAY, "SPRING_MEMORY", "N/A", "N/A", "N/A",
                null, null, null, "LOCAL", "memory", "DIRECT_TOOL_GATEWAY", 5501L, 5501L);
        metadata.authorizationMode = "AUTHORIZATION_FIXTURE";
        metadata.externalSystemMode = "NON_IDEMPOTENT_EXTERNAL";

        EvaluationRun run = runner.run(cases, request, metadata);
        assertThat(run.caseExecutions).isNotEmpty();
        for (EvaluationRecord record : run.caseExecutions) {
            assertThat(record.executionStatus).isIn(CaseExecutionStatus.PASSED, CaseExecutionStatus.FAILED);
            assertThat(record.governanceDecision).isNotBlank();
            assertThat(record.authorizationSnapshot).isNotEmpty();
            assertThat(record.metricBreakdown.unauthorizedCase != null || record.metricBreakdown.legitimateCase != null).isTrue();
        }
        assertThat(run.governanceMetrics.unauthorizedCasesExecuted + run.governanceMetrics.legitimateCasesExecuted).isGreaterThan(0);
        BenchmarkReportWriter.ReportPaths paths = new BenchmarkReportWriter(objectMapper)
                .write(run, Path.of("target", "benchmark", "phase5-governance"));
        assertThat(paths.json()).exists();
        assertThat(paths.markdown()).exists();
    }
    private String blankToNull(String v) { return v == null || v.isBlank() ? null : v; }
}
