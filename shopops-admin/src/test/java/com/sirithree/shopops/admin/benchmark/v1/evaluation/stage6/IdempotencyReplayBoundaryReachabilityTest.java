package com.sirithree.shopops.admin.benchmark.v1.evaluation.stage6;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import org.junit.jupiter.api.Test;

class IdempotencyReplayBoundaryReachabilityTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test void sequentialReplayMustReachWriteOperationBoundaryForEveryAttempt() {
        var record = execute(IdempotencyTestCases.refund("stage6-boundary", 3, 1));
        assertThat(record.metricBreakdown.attributionEligible).isTrue();
        assertThat(record.metricBreakdown.idempotencyBoundaryReachedAttempts).isEqualTo(3);
        assertThat(record.metricBreakdown.preIdempotencyBlockedAttempts).isZero();
        assertThat(record.failureReasons).doesNotContain("PRE_IDEMPOTENCY_APPROVAL_BLOCK", "IDEMPOTENCY_ATTRIBUTION_INVALID");
    }
}
