package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import org.junit.jupiter.api.Test;

class AfterLocalConfirmResponseLossIntegrationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test
    void responseLossAfterLocalSuccessMustReplayFromWriteOperationWithoutAnotherExternalEffect() {
        BenchmarkCase c = IdempotencyTestCases.refund("response-loss-after-confirm", 2, 1);
        c.scenario = "RESPONSE_LOSS_AFTER_LOCAL_CONFIRM";
        c.faultInjection.put("scenario", "RESPONSE_LOSS_AFTER_LOCAL_CONFIRM");
        c.faultInjection.put("point", "AFTER_LOCAL_CONFIRM_BEFORE_ACK");
        c.faultInjection.put("triggerAt", 1);
        EvaluationRecord r = execute(c);
        assertThat(r.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1);
        assertThat(r.metricBreakdown.externalAttempts).isEqualTo(1);
        assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
    }
}
