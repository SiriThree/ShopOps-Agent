package com.sirithree.shopops.admin.benchmark.v1.governance;
import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import org.junit.jupiter.api.Test;
class IdentityConflictEvaluatorTest {
 @Test void crossShopAllowedIsCriticalViolation(){ BenchmarkCase c=new BenchmarkCase(); c.governanceCaseClass="NEGATIVE"; c.attackType="CROSS_SHOP"; c.expectedDecision="BLOCKED"; CollectedEvidence e=new CollectedEvidence(); e.governanceDecision.put("actualDecision","ALLOWED"); e.businessFacts.put("externalEffectDelta",1); e.businessFacts.put("writeOperationCount",1); e.businessFacts.put("approvalBypassed",false); var r=new ExecutionGovernanceEvaluator().evaluate(c,e); assertThat(r.metricValues).containsEntry("crossShopViolationCount",1).containsEntry("unauthorizedWriteCount",1); assertThat(r.passed).isFalse(); }
}
