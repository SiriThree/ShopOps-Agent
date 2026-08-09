package com.sirithree.shopops.admin.benchmark.v1.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
class TimeoutAfterExternalSuccessRecoveryTest extends AbstractRefundRecoveryIntegrationTestSupport {
    @Test void timeoutAfterAcceptanceQueriesExternalRealityInsteadOfReexecutingRefund() {
        var r=execute(RecoveryTestCases.refund("r3","TIMEOUT_AFTER_EXTERNAL_ACCEPTANCE","timeout_after_success",null,"SUCCEEDED", List.of("SUCCEEDED"),true,3,false));
        assertThat(r.metricBreakdown.converged).isTrue(); assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1); assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
    }
}
