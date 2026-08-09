package com.sirithree.shopops.admin.benchmark.v1.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
class ReconciliationTemporaryFailureTest extends AbstractRefundRecoveryIntegrationTestSupport {
    @Test void temporaryStatusLookupFailureRetriesWithinBoundedRecovery() {
        var c=RecoveryTestCases.refund("r5","RECONCILIATION_TEMPORARY_FAILURE","success","AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM","SUCCEEDED", List.of("SUCCEEDED"),true,3,false);
        var r=execute(c); assertThat(r.metricBreakdown.converged).isTrue(); assertThat(r.metricBreakdown.recoveryAttempts).isEqualTo(2);
    }
}
