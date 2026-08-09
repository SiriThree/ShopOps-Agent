package com.sirithree.shopops.admin.benchmark.v1.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
class DuplicateReconciliationTest extends AbstractRefundRecoveryIntegrationTestSupport {
    @Test void concurrentRecoveryQueriesDoNotCreateAnotherExternalRefund() {
        var c=RecoveryTestCases.refund("r8","DUPLICATE_RECONCILIATION","timeout_after_success",null,"SUCCEEDED", List.of("SUCCEEDED"),true,3,false);
        c.concurrency.put("workers",2); c.concurrency.put("simultaneous",true);
        var r=execute(c); assertThat(r.metricBreakdown.converged).isTrue(); assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1); assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
    }
}
