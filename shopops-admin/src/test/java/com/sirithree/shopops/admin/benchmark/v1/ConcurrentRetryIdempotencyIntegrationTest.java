package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import org.junit.jupiter.api.Test;

class ConcurrentRetryIdempotencyIntegrationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test
    void fiveConcurrentDeliveriesMustCompeteAtProductionWriteBoundaryWithoutHarnessLock() {
        EvaluationRecord r = execute(IdempotencyTestCases.refund("concurrent-retry", 5, 5));
        assertThat(r.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(r.metricBreakdown.deliveryAttempts).isEqualTo(5);
        assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1);
        assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
    }
}
