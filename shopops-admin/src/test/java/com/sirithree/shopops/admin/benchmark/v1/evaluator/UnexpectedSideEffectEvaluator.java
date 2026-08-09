package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnexpectedSideEffectEvaluator implements BenchmarkEvaluator {
    @Override
    public EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        int expectedLogical = benchmarkCase.sideEffectExpectation == null
                ? 0 : benchmarkCase.sideEffectExpectation.expectedLogicalSideEffects;
        Set<String> forbiddenTypes = new HashSet<>(benchmarkCase.sideEffectExpectation == null
                ? List.of() : benchmarkCase.sideEffectExpectation.forbiddenEffectTypes);

        boolean unexpected = expectedLogical == 0 && !evidence.writeOperations.isEmpty();
        for (var effect : evidence.sideEffects) {
            Object effectType = effect.get("effectType");
            if (effectType != null && forbiddenTypes.contains(String.valueOf(effectType))) unexpected = true;
        }

        result.metric("noUnexpectedSideEffect", !unexpected);
        result.metric("observedWriteOperationCount", evidence.writeOperations.size());
        result.metric("observedExternalSideEffectCount", evidence.sideEffects.size());
        if (unexpected) result.fail(FailureReasonCode.UNEXPECTED_SIDE_EFFECT);
        return result;
    }
}
