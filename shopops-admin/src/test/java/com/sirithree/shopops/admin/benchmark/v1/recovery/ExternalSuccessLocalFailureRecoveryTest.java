package com.sirithree.shopops.admin.benchmark.v1.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalSuccessLocalFailureRecoveryTest extends AbstractRefundRecoveryIntegrationTestSupport {
    @Test void requestCorrelationRecoversExternalSuccessAfterReferenceWasNeverPersisted() {
        EvaluationRecord r = execute(RecoveryTestCases.refund("r1", "EXTERNAL_SUCCESS_LOCAL_FAILURE", "success",
                "AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM", "SUCCEEDED", List.of("SUCCEEDED"), true, 3, false));
        assertThat(r.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(r.finalState).isEqualTo("SUCCEEDED");
        assertThat(r.metricBreakdown.converged).isTrue();
        assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
    }
}
