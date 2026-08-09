package com.sirithree.shopops.admin.benchmark.v1.evaluation.stage6;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.SideEffectIdempotencyEvaluator;
import org.junit.jupiter.api.Test;

class IdempotencyMissingEffectMetricTest {
    @Test void zeroDuplicateCannotHideMissingLegitimateEffect() {
        var c = IdempotencyTestCases.refund("stage6-missing-effect", 1, 1);
        var evidence = new CollectedEvidence();
        evidence.businessFacts.put("externalGroundTruthAvailable", true);
        var result = new SideEffectIdempotencyEvaluator().evaluate(c, evidence);
        assertThat(result.metricValues).containsEntry("duplicateEffects", 0).containsEntry("missingEffects", 1);
        assertThat(result.failureReasons).contains(FailureReasonCode.MISSING_SIDE_EFFECT);
        assertThat(result.passed).isFalse();
    }
}
