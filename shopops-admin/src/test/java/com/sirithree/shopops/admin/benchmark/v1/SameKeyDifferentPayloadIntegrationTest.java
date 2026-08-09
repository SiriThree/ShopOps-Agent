package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import org.junit.jupiter.api.Test;

class SameKeyDifferentPayloadIntegrationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test
    void sameLogicalKeyWithDifferentRefundAmountMustRejectConflictInsteadOfTreatingAsReplay() {
        BenchmarkCase c = IdempotencyTestCases.refund("payload-conflict", 1, 1);
        c.scenario = "SAME_KEY_DIFFERENT_PAYLOAD";
        c.idempotencyExpectation.put("exercisePayloadConflict", true);
        c.idempotencyExpectation.put("payloadConflictBehavior", "REJECT");
        EvaluationRecord r = execute(c);
        assertThat(r.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1);
        assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
        assertThat(r.toolAttempts).anyMatch(log -> "IDEMPOTENCY_PAYLOAD_MISMATCH".equals(log.get("errorCode")));
    }
}
