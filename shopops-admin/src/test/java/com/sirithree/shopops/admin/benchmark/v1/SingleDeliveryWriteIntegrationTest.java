package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import org.junit.jupiter.api.Test;

class SingleDeliveryWriteIntegrationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test
    void oneApprovedLogicalRefundMustCreateExactlyOneIndependentExternalEffect() {
        EvaluationRecord r = execute(IdempotencyTestCases.refund("single-delivery", 1, 1));
        assertThat(r.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(r.metricBreakdown.deliveryAttempts).isEqualTo(1);
        assertThat(r.metricBreakdown.externalAttempts).isEqualTo(1);
        assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1);
        assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
        assertThat(r.approvalEvents).isNotEmpty();
    }
}
