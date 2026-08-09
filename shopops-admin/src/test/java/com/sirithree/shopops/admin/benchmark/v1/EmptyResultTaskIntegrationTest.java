package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.ShopOpsAdminApplication;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.CompositeTaskBenchmarkEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evidence.ProductionBenchmarkEvidenceCollector;
import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.mcp.support.InMemoryCommerceMcpClient;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(classes = {ShopOpsAdminApplication.class, EmptyResultTaskIntegrationTest.TestInfrastructure.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=memory",
                "shopops.connector.ad-performance.file=shopops-admin/src/test/resources/benchmark/v1/fixtures/ad-performance-empty.json"
        })
class EmptyResultTaskIntegrationTest {
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
    void legitimateEmptyAdResultFlowsThroughRealAgentAsNoData() {
        BenchmarkCase c = TaskEvaluationFixtures.benchmarkCase("ad_anomaly");
        c.caseId = "integration-ad-empty-001";
        c.input.put("userInput", "Analyze ad performance for 2018-08-09 and tell me whether there is an anomaly");
        c.input.put("dateRange", Map.of("start", "2018-08-09", "end", "2018-08-09"));
        c.expectedOutcome.put("resultClass", "NO_DATA");
        c.tags.add("EMPTY_RESULT");

        ShopOpsBenchmarkRunner runner = new ShopOpsBenchmarkRunner(
                new HttpShopOpsBenchmarkRuntime(restTemplate, "http://localhost:" + port),
                new ProductionBenchmarkEvidenceCollector(agentTaskService, toolCallLogService, traceService,
                        approvalRequestService, writeOperationService, operationReportService, objectMapper),
                new CompositeTaskBenchmarkEvaluator());
        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.caseId = c.caseId;
        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "shopopsbench-v1.1-empty-result", "dev-fixture", BenchmarkEnvironment.DETERMINISTIC,
                BenchmarkExecutionLevel.HTTP, "spring-memory-sync+empty-ad-file", "RULE_BASED", "RULE_BASED",
                "DISABLED", null, null, null, "LOCAL+MCP_TEST_ADAPTER", "MEMORY", "SYNC", 1L, 29L);

        EvaluationRun run = runner.run(List.of(c), request, metadata);
        EvaluationRecord record = run.caseExecutions.get(0);
        assertThat(record.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(record.metricBreakdown.businessOutcomeCorrect).isTrue();
        assertThat(record.metricBreakdown.taskSuccess).isTrue();
        assertThat(record.businessOutcome).containsEntry("reportAdDataStatus", "NO_DATA");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestInfrastructure {
        @Bean @Primary
        InMemoryCommerceMcpClient inMemoryCommerceMcpClient(CommentRiskService commentRiskService) {
            return new InMemoryCommerceMcpClient(commentRiskService);
        }
    }
}
