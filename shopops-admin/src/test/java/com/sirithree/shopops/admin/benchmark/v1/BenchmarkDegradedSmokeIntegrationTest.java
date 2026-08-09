package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.ShopOpsAdminApplication;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.CompositeTaskBenchmarkEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.evidence.ProductionBenchmarkEvidenceCollector;
import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.mcp.support.InMemoryCommerceMcpClient;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(classes = {ShopOpsAdminApplication.class, BenchmarkDegradedSmokeIntegrationTest.TestInfrastructure.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "shopops.persistence=memory",
        "shopops.tool.fail-code=ad.query_performance"
})
class BenchmarkDegradedSmokeIntegrationTest {
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
    void injectedInfrastructureFaultIsCapturedAsRuntimeEvidenceNotHiddenByFinalReport() throws Exception {
        List<BenchmarkCase> cases = new BenchmarkCaseLoader(objectMapper)
                .loadResource("/benchmark/v1/smoke/task-cases.json");
        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.benchmarkType = BenchmarkType.TASK;
        request.caseId = "smoke-task-degraded-ad-001";

        ShopOpsBenchmarkRunner runner = new ShopOpsBenchmarkRunner(
                new HttpShopOpsBenchmarkRuntime(restTemplate, "http://localhost:" + port),
                new ProductionBenchmarkEvidenceCollector(agentTaskService, toolCallLogService, traceService,
                        approvalRequestService, writeOperationService, operationReportService, objectMapper),
                new CompositeTaskBenchmarkEvaluator());
        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "shopopsbench-v1-smoke", "smoke", BenchmarkEnvironment.DETERMINISTIC,
                BenchmarkExecutionLevel.HTTP, "spring-memory-sync+tool-fault", "RULE_BASED", "RULE_BASED",
                "DISABLED", null, null, null, "LOCAL+MCP_TEST_ADAPTER", "MEMORY", "SYNC", 1L, 17L);

        EvaluationRun run = runner.run(cases, request, metadata);
        EvaluationRecord record = run.caseExecutions.get(0);
        assertThat(record.taskId).isNotNull();
        assertThat(record.finalState).isEqualToIgnoringCase("DEGRADED");
        assertThat(record.failureReasons).contains(FailureReasonCode.TOOL_EXECUTION_ERROR.name());
        assertThat(record.evidenceRefs).isNotEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestInfrastructure {
        @Bean @Primary
        InMemoryCommerceMcpClient inMemoryCommerceMcpClient(CommentRiskService commentRiskService) {
            return new InMemoryCommerceMcpClient(commentRiskService);
        }
    }
}
