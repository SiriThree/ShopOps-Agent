package com.sirithree.shopops.admin.benchmark.v1.formal;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.ShopOpsAdminApplication;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.benchmark.v1.*;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.CompositeTaskBenchmarkEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evidence.ProductionBenchmarkEvidenceCollector;
import com.sirithree.shopops.admin.benchmark.v1.report.BenchmarkReportWriter;
import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.mcp.support.InMemoryCommerceMcpClient;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@EnabledIfSystemProperty(named="shopops.formal.it",matches="true")
@SpringBootTest(classes={ShopOpsAdminApplication.class, FormalTaskBenchmarkIntegrationTest.Infrastructure.class},
        webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "shopops.persistence=jdbc","shopops.agent.dispatch-mode=sync","shopops.mcp.servers.commerce.enabled=false",
        "shopops.connector.order-summary.file=shopops-admin/src/test/resources/benchmark/v1/task/fixtures/stage2/order-summary-stage2.json",
        "shopops.connector.negative-comments.file=shopops-admin/src/test/resources/benchmark/v1/task/fixtures/stage2/negative-comments-stage2.json",
        "shopops.connector.product-candidates.file=shopops-admin/src/test/resources/benchmark/v1/task/fixtures/stage2/product-candidates-stage2.json",
        "shopops.connector.ad-performance.file=shopops-admin/src/test/resources/benchmark/v1/task/fixtures/stage2/ad-performance-stage2.json",
        "shopops.connector.external-reports.file=shopops-admin/src/test/resources/benchmark/v1/task/fixtures/stage2/external-reports-stage2.json",
        "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
        "spring.datasource.username=root","spring.datasource.password=root","spring.datasource.hikari.initialization-fail-timeout=10000"
})
class FormalTaskBenchmarkIntegrationTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate; @Autowired ObjectMapper objectMapper; @Autowired AgentTaskService tasks;
    @Autowired ToolCallLogService toolLogs; @Autowired TraceService traces; @Autowired ApprovalRequestService approvals;
    @Autowired WriteOperationService writes; @Autowired OperationReportService reports;

    @Test void executesFrozenHeldOutTaskSplitThroughRealHttpAgentRuntime() throws Exception {
        List<BenchmarkCase> cases = new BenchmarkCaseLoader(objectMapper).loadResource("benchmark/v1/test/cases.json")
                .stream().filter(c -> c.benchmarkType == BenchmarkType.TASK).toList();
        BenchmarkRunRequest request=new BenchmarkRunRequest(); request.benchmarkType=BenchmarkType.TASK; request.datasetSplit="test";
        request.executionLevel=BenchmarkExecutionLevel.HTTP; request.environment=BenchmarkEnvironment.JDBC_INTEGRATION;
        ShopOpsBenchmarkRunner runner=new ShopOpsBenchmarkRunner(
                new HttpShopOpsBenchmarkRuntime(restTemplate,"http://localhost:"+port),
                new ProductionBenchmarkEvidenceCollector(tasks,toolLogs,traces,approvals,writes,reports,objectMapper),
                new CompositeTaskBenchmarkEvaluator());
        EvaluationRunMetadata metadata=new EvaluationRunMetadataFactory().create("1.2.0-stage2-task-candidate","test",BenchmarkEnvironment.JDBC_INTEGRATION,
                BenchmarkExecutionLevel.HTTP,"SPRING_JDBC_SYNC","RULE_BASED","RULE_BASED","DISABLED",null,null,null,
                "LOCAL+MCP_TEST_ADAPTER","jdbc-mysql","SYNC",6101L,6101L);
        metadata.authorizationMode="JDBC"; metadata.externalSystemMode="N/A";
        EvaluationRun run=runner.run(cases,request,metadata);
        assertThat(run.caseExecutions).hasSize(cases.size());
        assertThat(run.aggregate.executedCases).isGreaterThan(0);
        new BenchmarkReportWriter(objectMapper).write(run,Path.of("target","benchmark","formal","task"));
    }

    @TestConfiguration(proxyBeanMethods=false)
    static class Infrastructure {
        @Bean @Primary InMemoryCommerceMcpClient inMemoryCommerceMcpClient(CommentRiskService service) { return new InMemoryCommerceMcpClient(service); }
    }
}
