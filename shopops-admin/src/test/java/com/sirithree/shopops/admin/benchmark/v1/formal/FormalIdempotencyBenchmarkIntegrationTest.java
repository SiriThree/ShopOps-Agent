package com.sirithree.shopops.admin.benchmark.v1.formal;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.benchmark.v1.*;
import com.sirithree.shopops.admin.benchmark.v1.fault.DeterministicReliabilityFaultController;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.*;
import com.sirithree.shopops.admin.benchmark.v1.report.BenchmarkReportWriter;
import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.nio.file.Path; import java.util.List;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Import; import org.springframework.context.annotation.Primary;

@EnabledIfSystemProperty(named="shopops.formal.it",matches="true")
@SpringBootTest(properties={"shopops.persistence=jdbc","shopops.agent.dispatch-mode=sync","shopops.mcp.servers.commerce.enabled=false",
 "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
 "spring.datasource.username=root","spring.datasource.password=root","spring.datasource.hikari.initialization-fail-timeout=10000"})
@Import(FormalIdempotencyBenchmarkIntegrationTest.Infrastructure.class)
class FormalIdempotencyBenchmarkIntegrationTest {
 @Autowired ObjectMapper objectMapper; @Autowired ToolGatewayService gateway; @Autowired ApprovalRequestService approvals;
 @Autowired ToolCallLogService logs; @Autowired WriteOperationService writes; @Autowired RecordingRefundExternalSystem external; @Autowired DeterministicReliabilityFaultController faults;
 @Test void executesFrozenHeldOutIdempotencySplitWithJdbcAndIndependentNonIdempotentExternalTruth() throws Exception {
   List<BenchmarkCase> cases=new BenchmarkCaseLoader(objectMapper).loadResource("benchmark/v1/idempotency/test/cases.json");
   RefundIdempotencyBenchmarkExecutor executor=new RefundIdempotencyBenchmarkExecutor(gateway,approvals,logs,writes,external,faults);
   ShopOpsBenchmarkRunner runner=new ShopOpsBenchmarkRunner(null,null,null,executor);
   BenchmarkRunRequest request=new BenchmarkRunRequest(); request.benchmarkType=BenchmarkType.IDEMPOTENCY; request.datasetSplit="test"; request.executionLevel=BenchmarkExecutionLevel.TOOL_GATEWAY; request.environment=BenchmarkEnvironment.JDBC_INTEGRATION;
   EvaluationRunMetadata metadata=new EvaluationRunMetadataFactory().create("1.2.1-phase6-final-idempotency","test",BenchmarkEnvironment.JDBC_INTEGRATION,BenchmarkExecutionLevel.TOOL_GATEWAY,"SPRING_JDBC","N/A","N/A","N/A",null,null,null,"LOCAL","jdbc-mysql","REPEATED_REQUEST",6201L,6201L);
   metadata.authorizationMode="JDBC"; metadata.externalSystemMode=ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL.name();
   EvaluationRun run=runner.run(cases,request,metadata); assertThat(run.caseExecutions).hasSize(5); assertThat(run.idempotencyMetrics.executedCases).isEqualTo(5);
   new BenchmarkReportWriter(objectMapper).write(run,Path.of("target","benchmark","formal","idempotency"));
 }
 @TestConfiguration static class Infrastructure { @Bean @Primary RecordingRefundExternalSystem external(){return new RecordingRefundExternalSystem();} @Bean @Primary DeterministicReliabilityFaultController faults(){return new DeterministicReliabilityFaultController();} }
}
