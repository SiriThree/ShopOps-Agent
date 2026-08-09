package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.ShopOpsAdminApplication;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.CompositeTaskBenchmarkEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evidence.ProductionBenchmarkEvidenceCollector;
import com.sirithree.shopops.admin.benchmark.v1.report.BenchmarkReportWriter;
import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.mcp.support.InMemoryCommerceMcpClient;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import java.util.List;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(classes = {ShopOpsAdminApplication.class, BenchmarkRunnerLifecycleTest.TestInfrastructure.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "shopops.persistence=memory")
class BenchmarkRunnerLifecycleTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired AgentTaskService agentTaskService;
    @Autowired ToolCallLogService toolCallLogService;
    @Autowired TraceService traceService;
    @Autowired ApprovalRequestService approvalRequestService;
    @Autowired WriteOperationService writeOperationService;
    @Autowired OperationReportService operationReportService;

    @Test
    void benchmarkCaseFlowsThroughRealHttpAgentRuntimeIntoEvaluationRecord() throws Exception {
        String split = System.getProperty("shopops.benchmark.split", "smoke");
        List<BenchmarkCase> cases = new BenchmarkCaseLoader(objectMapper)
                .loadResource(BenchmarkDatasetResources.resourceFor(split));
        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.datasetSplit = split;
        request.benchmarkType = BenchmarkType.TASK;
        request.caseId = System.getProperty("shopops.benchmark.caseId");
        if (request.caseId == null && "smoke".equalsIgnoreCase(split)) {
            request.caseId = "smoke-task-daily-review-001";
        }
        request.scenario = System.getProperty("shopops.benchmark.scenario");
        request.tag = System.getProperty("shopops.benchmark.tag");

        ShopOpsBenchmarkRunner runner = new ShopOpsBenchmarkRunner(
                new HttpShopOpsBenchmarkRuntime(restTemplate, "http://localhost:" + port),
                new ProductionBenchmarkEvidenceCollector(agentTaskService, toolCallLogService, traceService,
                        approvalRequestService, writeOperationService, operationReportService, objectMapper),
                new CompositeTaskBenchmarkEvaluator());
        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "shopopsbench-v1.1-" + split, split, BenchmarkEnvironment.DETERMINISTIC,
                BenchmarkExecutionLevel.HTTP, "spring-memory-sync", "RULE_BASED", "RULE_BASED",
                "DISABLED", null, null, null, "LOCAL+MCP_TEST_ADAPTER", "MEMORY", "SYNC", 1L, 1L);

        EvaluationRun run = runner.run(cases, request, metadata);
        if (request.caseId != null) {
            assertThat(run.caseExecutions).hasSize(1);
        } else {
            assertThat(run.caseExecutions).isNotEmpty();
        }
        for (EvaluationRecord record : run.caseExecutions) {
            assertThat(record.taskId).isNotNull();
            assertThat(record.evidenceRefs).isNotEmpty();
            assertThat(record.runtimeMetadata).containsEntry("executionLevel", BenchmarkExecutionLevel.HTTP);
            assertThat(record.observedPlan).containsKey("actualPlannerObservation");
            assertThat(record.metricBreakdown.taskSuccess).isNotNull();
            assertThat(record.businessOutcome).isNotEmpty();
            // Verifies the full runner/evidence/evaluator lifecycle, not a formal benchmark score.
            assertThat(record.executionStatus).isIn(CaseExecutionStatus.PASSED, CaseExecutionStatus.FAILED);
        }
        BenchmarkReportWriter.ReportPaths paths = new BenchmarkReportWriter(objectMapper)
                .write(run, Path.of("target", "benchmark"));
        assertThat(paths.json()).exists();
        assertThat(paths.markdown()).exists();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestInfrastructure {
        @Bean @Primary
        InMemoryCommerceMcpClient inMemoryCommerceMcpClient(CommentRiskService commentRiskService) {
            return new InMemoryCommerceMcpClient(commentRiskService);
        }
    }
}
