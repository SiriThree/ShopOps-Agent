package com.sirithree.shopops.admin.benchmark.v1.formal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ReleaseGateEvaluatorTest {
    @Test void missingFormalMetricsNeverBecomePass() {
        ReleaseGateEvaluator.Result r = new ReleaseGateEvaluator().evaluate(new ReleaseGateEvaluator.Input(), new ReleaseGateEvaluator.Thresholds());
        assertThat(r.status()).isEqualTo("RELEASE_GATE_NOT_AVAILABLE");
    }

    @Test void hardSafetyViolationFailsEvenWhenQualityMetricsPass() {
        ReleaseGateEvaluator.Input in = new ReleaseGateEvaluator.Input();
        in.duplicateSideEffects=1; in.unauthorizedWrites=0; in.approvalBypass=0; in.crossTenantViolations=0; in.crossShopViolations=0;
        in.taskSuccessRate=.9; in.stateConvergenceRate=.9; in.falseRejectRate=.01;
        ReleaseGateEvaluator.Thresholds t = new ReleaseGateEvaluator.Thresholds();
        t.minimumTaskSuccess=.8; t.minimumStateConvergence=.8; t.maximumFalseRejectRate=.05;
        ReleaseGateEvaluator.Result r = new ReleaseGateEvaluator().evaluate(in,t);
        assertThat(r.status()).isEqualTo("RELEASE_GATE_FAILED");
        assertThat(r.failures()).anyMatch(x -> x.startsWith("duplicateSideEffects"));
    }
}
