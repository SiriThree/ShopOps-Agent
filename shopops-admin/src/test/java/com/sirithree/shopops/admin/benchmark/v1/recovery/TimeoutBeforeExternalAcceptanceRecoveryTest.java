package com.sirithree.shopops.admin.benchmark.v1.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
class TimeoutBeforeExternalAcceptanceRecoveryTest extends AbstractRefundRecoveryIntegrationTestSupport {
    @Test void timeoutBeforeAcceptanceConvergesToFailedWithoutCreatingEffect() {
        var r=execute(RecoveryTestCases.refund("r2","TIMEOUT_BEFORE_EXTERNAL_ACCEPTANCE","timeout_before_success",null,"NOT_ACCEPTED", List.of("FAILED"),true,3,false));
        assertThat(r.finalState).isEqualTo("FAILED"); assertThat(r.metricBreakdown.converged).isTrue(); assertThat(r.metricBreakdown.actualEffectiveSideEffects).isZero();
    }
}
