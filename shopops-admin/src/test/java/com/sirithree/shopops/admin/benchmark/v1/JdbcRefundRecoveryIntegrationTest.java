package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.benchmark.v1.fault.DeterministicReliabilityFaultController;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RecordingRefundExternalSystem;
import com.sirithree.shopops.admin.benchmark.v1.recovery.RecoveryTestCases;
import com.sirithree.shopops.admin.benchmark.v1.recovery.RefundRecoveryBenchmarkExecutor;
import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import com.sirithree.shopops.admin.reliability.service.WriteOperationReconciliationService;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@EnabledIfSystemProperty(named="shopops.jdbc.it",matches="true")
@SpringBootTest(properties={
 "shopops.persistence=jdbc","shopops.agent.dispatch-mode=sync","shopops.mcp.servers.commerce.enabled=false",
 "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
 "spring.datasource.username=root","spring.datasource.password=root","spring.datasource.hikari.initialization-fail-timeout=1","spring.datasource.hikari.connection-timeout=3000"
})
@Import(JdbcRefundRecoveryIntegrationTest.Infrastructure.class)
class JdbcRefundRecoveryIntegrationTest {
 @Autowired ToolGatewayService toolGateway; @Autowired ApprovalRequestService approvals; @Autowired WriteOperationService writeOperations;
 @Autowired WriteOperationReconciliationService reconciliation; @Autowired RecordingRefundExternalSystem external; @Autowired DeterministicReliabilityFaultController faults;
 @Test void externalSuccessMissingReferenceRecoversThroughDurableRequestCorrelationOnJdbc(){
   BenchmarkCase c= RecoveryTestCases.refund("jdbc-r1","EXTERNAL_SUCCESS_LOCAL_FAILURE","success","AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM","SUCCEEDED", List.of("SUCCEEDED"),true,3,false); c.input.put("orderId", "SO202607180001");
   RefundRecoveryBenchmarkExecutor executor=new RefundRecoveryBenchmarkExecutor(toolGateway,approvals,writeOperations,reconciliation,external,faults);
   BenchmarkRunRequest request=new BenchmarkRunRequest(); request.benchmarkType=BenchmarkType.RECOVERY; request.executionLevel=BenchmarkExecutionLevel.TOOL_GATEWAY; request.environment=BenchmarkEnvironment.JDBC_INTEGRATION;
   EvaluationRunMetadata metadata=new EvaluationRunMetadataFactory().create("1.3.0-phase4-recovery","jdbc",BenchmarkEnvironment.JDBC_INTEGRATION,BenchmarkExecutionLevel.TOOL_GATEWAY,"SPRING_JDBC","N/A","N/A","N/A",null,null,null,"LOCAL","jdbc-mysql","DIRECT_RECOVERY",4401L,4401L); metadata.externalSystemMode="NON_IDEMPOTENT_EXTERNAL";
   EvaluationRecord r=executor.execute(c,request,metadata); assertThat(r.metricBreakdown.converged).isTrue(); assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
 }
 @TestConfiguration static class Infrastructure { @Bean @Primary RecordingRefundExternalSystem external(){return new RecordingRefundExternalSystem();} @Bean @Primary DeterministicReliabilityFaultController faults(){return new DeterministicReliabilityFaultController();} }
}
