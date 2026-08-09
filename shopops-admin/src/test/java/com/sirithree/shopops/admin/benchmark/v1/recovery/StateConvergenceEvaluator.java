package com.sirithree.shopops.admin.benchmark.v1.recovery;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import java.util.Map;

public class StateConvergenceEvaluator {
    public EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        String local = string(evidence.businessFacts.get("localState"));
        String external = string(evidence.businessFacts.get("externalReality"));
        int effects = number(evidence.businessFacts.get("effectiveSideEffects"));
        int attempts = number(evidence.businessFacts.get("recoveryAttempts"));

        boolean terminal = WriteOperationStatus.terminal(local);
        boolean stateCorrect = stateCorrect(local, external, benchmarkCase);
        int expectedEffects = benchmarkCase.expectedEffectiveSideEffects == null
                ? (benchmarkCase.sideEffectExpectation == null ? 0 : benchmarkCase.sideEffectExpectation.expectedLogicalSideEffects)
                : benchmarkCase.expectedEffectiveSideEffects;
        boolean noDuplicate = effects <= expectedEffects;
        boolean converged = terminal && stateCorrect && noDuplicate;
        boolean manual = WriteOperationStatus.NEEDS_MANUAL_ACTION.equals(local);
        boolean incorrectTerminal = terminal && !stateCorrect && !manual;
        boolean permanentStuck = !terminal;

        result.metricValues.put("terminalStateReached", terminal);
        result.metricValues.put("localStateConsistentWithExternalReality", stateCorrect);
        result.metricValues.put("converged", converged);
        result.metricValues.put("recoveryAttempts", attempts);
        result.metricValues.put("manualReviewCount", manual ? 1 : 0);
        result.metricValues.put("permanentStuckCount", permanentStuck ? 1 : 0);
        result.metricValues.put("incorrectTerminalStateCount", incorrectTerminal ? 1 : 0);
        result.metricValues.put("duplicateSideEffects", Math.max(effects - expectedEffects, 0));

        if (external == null || "UNAVAILABLE".equals(external)) result.failureReasons.add(FailureReasonCode.EXTERNAL_GROUND_TRUTH_UNAVAILABLE);
        if (!terminal) result.failureReasons.add(FailureReasonCode.STATE_NOT_CONVERGED);
        if (terminal && !stateCorrect) result.failureReasons.add(FailureReasonCode.EXTERNAL_STATE_MISMATCH);
        if (manual) result.failureReasons.add(FailureReasonCode.MANUAL_REVIEW_REQUIRED);
        if (!noDuplicate) result.failureReasons.add(FailureReasonCode.DUPLICATE_SIDE_EFFECT);
        if ("RECOVERY_BUDGET_EXHAUSTED".equals(evidence.businessFacts.get("recoveryReason"))) result.failureReasons.add(FailureReasonCode.RECOVERY_BUDGET_EXHAUSTED);
        boolean expectationMatched = benchmarkCase.expectedConvergence == null
                ? converged
                : benchmarkCase.expectedConvergence.booleanValue() == converged;
        if (Boolean.FALSE.equals(benchmarkCase.expectedConvergence) && Boolean.TRUE.equals(benchmarkCase.manualReviewAllowed)) {
            expectationMatched = expectationMatched && manual;
        }
        result.metricValues.put("expectationMatched", expectationMatched);
        result.passed = expectationMatched;
        return result;
    }

    private boolean stateCorrect(String local, String external, BenchmarkCase benchmarkCase) {
        if (local == null || external == null) return false;
        if (benchmarkCase.expectedExternalState != null && !benchmarkCase.expectedExternalState.equals(external)) return false;
        return switch (external) {
            case "SUCCEEDED" -> WriteOperationStatus.SUCCEEDED.equals(local);
            case "NOT_ACCEPTED", "FAILED" -> WriteOperationStatus.FAILED.equals(local);
            case "UNKNOWN" -> WriteOperationStatus.NEEDS_MANUAL_ACTION.equals(local) && Boolean.TRUE.equals(benchmarkCase.manualReviewAllowed);
            case "DUPLICATE" -> WriteOperationStatus.NEEDS_MANUAL_ACTION.equals(local);
            default -> false;
        };
    }

    private String string(Object value) { return value == null ? null : String.valueOf(value); }
    private int number(Object value) { return value instanceof Number n ? n.intValue() : 0; }
}
