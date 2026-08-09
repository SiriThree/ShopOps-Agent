package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import org.junit.jupiter.api.Test;

class SequentialRetryIdempotencyIntegrationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test
    void repeatedDeliveriesMustReachProductionButOnlyFirstMayReachNonIdempotentExternalSystem() {
        EvaluationRecord r = execute(IdempotencyTestCases.refund("sequential-retry", 5, 1));
        assertThat(r.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(r.metricBreakdown.deliveryAttempts).isEqualTo(5);
        assertThat(r.metricBreakdown.externalAttempts).isEqualTo(1);
        assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1);
        assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
    }
}
