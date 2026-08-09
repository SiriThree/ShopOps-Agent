package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class FinalStateEvaluator implements BenchmarkEvaluator {
    @Override
    public EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        String actual = evidence.task == null ? null : evidence.task.getStatus();
        List<String> expected = strings(benchmarkCase.expectedOutcome.get("requiredTerminalTaskStates"));
        if (expected.isEmpty()) {
            expected = strings(benchmarkCase.expectedOutcome.get("terminalTaskStates"));
        }
        boolean correct = expected.isEmpty()
                ? actual != null && isTerminal(actual)
                : actual != null && expected.stream().anyMatch(item -> sameState(item, actual));
        result.metric("finalStateCorrect", correct);
        result.metric("finalState", actual);
        if (!correct) result.fail(FailureReasonCode.FINAL_STATE_INCORRECT);
        return result;
    }

    private boolean isTerminal(String state) {
        String normalized = state.toUpperCase(Locale.ROOT);
        return List.of("SUCCESS", "SUCCEEDED", "FAILED", "DEGRADED", "NEEDS_MANUAL_ACTION", "CANCELLED").contains(normalized);
    }

    private boolean sameState(String expected, String actual) {
        String left = normalize(expected);
        String right = normalize(actual);
        if ("SUCCEEDED".equals(left)) left = "SUCCESS";
        if ("NEEDS_MANUAL_ACTION".equals(left)) left = "DEGRADED";
        if ("SUCCEEDED".equals(right)) right = "SUCCESS";
        if ("NEEDS_MANUAL_ACTION".equals(right)) right = "DEGRADED";
        return left.equals(right);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private List<String> strings(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        return value == null ? List.of() : List.of(String.valueOf(value));
    }
}
