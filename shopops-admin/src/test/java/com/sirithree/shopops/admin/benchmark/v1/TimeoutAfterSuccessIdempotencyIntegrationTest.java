package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import org.junit.jupiter.api.Test;

class TimeoutAfterSuccessIdempotencyIntegrationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test
    void responseLostAfterExternalAcceptanceMustNotBlindlyExecuteRefundAgain() {
        BenchmarkCase c = IdempotencyTestCases.refund("timeout-after-success", 2, 1);
        c.scenario = "TIMEOUT_AFTER_EXTERNAL_SUCCESS";
        c.input.put("simulation", "timeout_after_success");
        EvaluationRecord r = execute(c);
        assertThat(r.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(r.metricBreakdown.externalAttempts).isEqualTo(1);
        assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1);
        assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
    }
}
