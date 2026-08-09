package com.sirithree.shopops.admin.benchmark.v1.governance;
import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import org.junit.jupiter.api.Test;
class ApprovalBindingEvaluatorTest {
 @Test void payloadMismatchHasStableReason(){ BenchmarkCase c=new BenchmarkCase(); c.governanceCaseClass="NEGATIVE"; c.attackType="APPROVAL_PAYLOAD_MUTATION"; c.expectedDecision="BLOCKED"; CollectedEvidence e=new CollectedEvidence(); e.governanceDecision.put("actualDecision","ALLOWED"); e.businessFacts.put("externalEffectDelta",0); e.businessFacts.put("writeOperationCount",0); e.businessFacts.put("approvalBypassed",false); var r=new ExecutionGovernanceEvaluator().evaluate(c,e); assertThat(r.failureReasons).contains(FailureReasonCode.APPROVAL_PAYLOAD_MISMATCH); }
}
