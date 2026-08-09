package com.sirithree.shopops.admin.benchmark.v1.formal;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService; import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import com.sirithree.shopops.admin.benchmark.v1.*; import com.sirithree.shopops.admin.benchmark.v1.governance.*;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.*; import com.sirithree.shopops.admin.benchmark.v1.report.BenchmarkReportWriter; import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService; import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.nio.file.Path; import java.util.List;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.condition.EnabledIfSystemProperty; import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest; import org.springframework.boot.test.context.TestConfiguration; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Import; import org.springframework.context.annotation.Primary;

@EnabledIfSystemProperty(named="shopops.formal.it",matches="true")
@SpringBootTest(properties={"shopops.persistence=jdbc","shopops.agent.dispatch-mode=sync","shopops.mcp.servers.commerce.enabled=false",
 "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
 "spring.datasource.username=root","spring.datasource.password=root","spring.datasource.hikari.initialization-fail-timeout=10000"})
@Import(FormalGovernanceBenchmarkIntegrationTest.Infrastructure.class)
class FormalGovernanceBenchmarkIntegrationTest {
 @Autowired ObjectMapper objectMapper; @Autowired ToolGatewayService gateway; @Autowired ApprovalRequestService approvals; @Autowired WriteOperationService writes;
 @Autowired RecordingRefundExternalSystem external; @Autowired AuthorizationService authorization;
 @Test void executesFrozenHeldOutNegativeAndPositiveControlsWithJdbcAuthorization() throws Exception {
   List<BenchmarkCase> cases=new BenchmarkCaseLoader(objectMapper).loadResource("benchmark/v1/governance/test/cases.json");
   RefundGovernanceBenchmarkExecutor executor=new RefundGovernanceBenchmarkExecutor(gateway,approvals,writes,external,authorization);
   ShopOpsBenchmarkRunner runner=new ShopOpsBenchmarkRunner(null,null,null,null,null,executor);
   BenchmarkRunRequest request=new BenchmarkRunRequest(); request.benchmarkType=BenchmarkType.GOVERNANCE; request.datasetSplit="test"; request.executionLevel=BenchmarkExecutionLevel.TOOL_GATEWAY; request.environment=BenchmarkEnvironment.JDBC_INTEGRATION;
   EvaluationRunMetadata metadata=new EvaluationRunMetadataFactory().create("1.4.1-phase6-final-governance","test",BenchmarkEnvironment.JDBC_INTEGRATION,BenchmarkExecutionLevel.TOOL_GATEWAY,"SPRING_JDBC","N/A","N/A","N/A",null,null,null,"LOCAL","jdbc-mysql","DIRECT_TOOL_GATEWAY",6401L,6401L);
   metadata.authorizationMode="JDBC"; metadata.externalSystemMode=ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL.name();
   EvaluationRun run=runner.run(cases,request,metadata); assertThat(run.caseExecutions).hasSize(5); assertThat(run.governanceMetrics.unauthorizedCasesExecuted).isGreaterThan(0); assertThat(run.governanceMetrics.legitimateCasesExecuted).isGreaterThan(0);
   new BenchmarkReportWriter(objectMapper).write(run,Path.of("target","benchmark","formal","governance"));
 }
 @TestConfiguration static class Infrastructure { @Bean @Primary RecordingRefundExternalSystem external(){return new RecordingRefundExternalSystem();} }
}
