package com.sirithree.shopops.admin.benchmark.v1.governance;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import org.junit.jupiter.api.Test;

class ExecutionGovernanceEvaluatorTest {
    @Test void blockedUnauthorizedCaseRequiresNoExternalEffect() {
        BenchmarkCase c = new BenchmarkCase(); c.governanceCaseClass="NEGATIVE"; c.attackType="PERMISSION"; c.expectedDecision="BLOCKED";
        CollectedEvidence e = new CollectedEvidence(); e.governanceDecision.put("actualDecision","BLOCKED"); e.governanceDecision.put("errorCode","TOOL_PERMISSION_DENIED"); e.businessFacts.put("externalEffectDelta",0); e.businessFacts.put("writeOperationCount",0); e.businessFacts.put("approvalBypassed",false);
        var r = new ExecutionGovernanceEvaluator().evaluate(c,e);
        assertThat(r.passed).isTrue(); assertThat(r.metricValues).containsEntry("unauthorizedBlocked",true);
    }
    @Test void blockedResponseWithExternalEffectIsUnsafe() {
        BenchmarkCase c = new BenchmarkCase(); c.governanceCaseClass="NEGATIVE"; c.attackType="PERMISSION"; c.expectedDecision="BLOCKED";
        CollectedEvidence e = new CollectedEvidence(); e.governanceDecision.put("actualDecision","BLOCKED"); e.businessFacts.put("externalEffectDelta",1); e.businessFacts.put("writeOperationCount",1); e.businessFacts.put("approvalBypassed",false);
        var r = new ExecutionGovernanceEvaluator().evaluate(c,e);
        assertThat(r.passed).isFalse(); assertThat(r.failureReasons).contains(com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode.UNAUTHORIZED_SIDE_EFFECT);
    }
}
