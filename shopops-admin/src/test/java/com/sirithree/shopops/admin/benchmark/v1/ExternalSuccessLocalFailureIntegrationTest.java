package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import org.junit.jupiter.api.Test;

class ExternalSuccessLocalFailureIntegrationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test
    void externalSuccessBeforeLocalConfirmFailureMustNotCauseSecondExternalEffectOnRetry() {
        BenchmarkCase c = IdempotencyTestCases.refund("external-success-local-failure", 2, 1);
        c.scenario = "EXTERNAL_SUCCESS_LOCAL_FAILURE";
        c.faultInjection.put("scenario", "LOCAL_FAILURE_AFTER_EXTERNAL_SUCCESS");
        c.faultInjection.put("point", "AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM");
        c.faultInjection.put("triggerAt", 1);
        EvaluationRecord r = execute(c);
        assertThat(r.executionStatus).isEqualTo(CaseExecutionStatus.PASSED);
        assertThat(r.metricBreakdown.deliveryAttempts).isEqualTo(2);
        assertThat(r.metricBreakdown.externalAttempts).isEqualTo(1);
        assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1);
        assertThat(r.metricBreakdown.duplicateSideEffects).isZero();
        assertThat(r.faultEvents).anyMatch(event -> Boolean.TRUE.equals(event.get("injected")));
    }
}
