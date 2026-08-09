package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.BenchmarkEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/** Evaluates effective external effects, not invocation/write-row counts. */
public class SideEffectIdempotencyEvaluator implements BenchmarkEvaluator {
    @Override
    public EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        boolean groundTruthAvailable = Boolean.TRUE.equals(evidence.businessFacts.get("externalGroundTruthAvailable"));
        if (!groundTruthAvailable) {
            return result
                    .metric("expectedLogicalEffects", expected(benchmarkCase))
                    .metric("actualEffectiveEffects", null)
                    .metric("duplicateEffects", null)
                    .metric("missingEffects", null)
                    .fail(FailureReasonCode.SIDE_EFFECT_GROUND_TRUTH_UNAVAILABLE);
        }

        int expected = expected(benchmarkCase);
        int actual = evidence.externalEffects.size();
        int duplicate = Math.max(actual - expected, 0);
        int missing = Math.max(expected - actual, 0);
        int wrongTarget = wrongTargetCount(benchmarkCase, evidence);
        int wrongPayload = wrongPayloadCount(benchmarkCase, evidence);
        int payloadConflicts = payloadConflictCount(evidence);
        boolean approvalSatisfied = approvalSatisfied(benchmarkCase, evidence);
        boolean attributionRequired = Boolean.TRUE.equals(evidence.businessFacts.get("attributionEvidenceRequired"));
        boolean attributionEligible = !attributionRequired || Boolean.TRUE.equals(evidence.businessFacts.get("attributionEligible"));
        int intendedReplayAttempts = intFact(evidence, "intendedReplayAttempts");
        int boundaryReached = intFact(evidence, "idempotencyBoundaryReachedAttempts");
        int preBlocked = intFact(evidence, "preIdempotencyBlockedAttempts");

        result.metric("logicalWriteRequests", evidence.logicalWriteRequests.size())
                .metric("deliveryAttempts", evidence.deliveryAttempts.size())
                .metric("executionAttempts", evidence.executionAttempts.size())
                .metric("toolAttempts", evidence.toolLogs.size())
                .metric("externalAttempts", evidence.externalAttempts.size())
                .metric("expectedLogicalEffects", expected)
                .metric("actualEffectiveEffects", actual)
                .metric("duplicateEffects", duplicate)
                .metric("missingEffects", missing)
                .metric("wrongTargetEffects", wrongTarget)
                .metric("wrongPayloadEffects", wrongPayload)
                .metric("payloadConflictCount", payloadConflicts)
                .metric("approvalSatisfied", approvalSatisfied)
                .metric("intendedReplayAttempts", intendedReplayAttempts)
                .metric("idempotencyBoundaryReachedAttempts", boundaryReached)
                .metric("preIdempotencyBlockedAttempts", preBlocked)
                .metric("attributionEligible", attributionEligible);

        if (duplicate > 0) result.fail(FailureReasonCode.DUPLICATE_SIDE_EFFECT);
        if (missing > 0) result.fail(FailureReasonCode.MISSING_SIDE_EFFECT);
        if (wrongTarget > 0) result.fail(FailureReasonCode.WRONG_SIDE_EFFECT_TARGET);
        if (wrongPayload > 0) result.fail(FailureReasonCode.IDEMPOTENCY_PAYLOAD_MISMATCH);
        if (!approvalSatisfied) result.fail(FailureReasonCode.APPROVAL_BYPASS);
        if (attributionRequired && !attributionEligible) {
            result.fail(FailureReasonCode.IDEMPOTENCY_ATTRIBUTION_INVALID);
            addAttributionFailureReasons(result, evidence);
        }

        Object expectedConflict = benchmarkCase.idempotencyExpectation == null
                ? null : benchmarkCase.idempotencyExpectation.get("payloadConflictBehavior");
        if ("REJECT".equalsIgnoreCase(String.valueOf(expectedConflict)) && payloadConflicts == 0) {
            result.fail(FailureReasonCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        return result;
    }

    private int expected(BenchmarkCase benchmarkCase) {
        if (benchmarkCase.expectedEffectiveSideEffects != null) {
            return benchmarkCase.expectedEffectiveSideEffects;
        }
        return benchmarkCase.sideEffectExpectation == null ? 0
                : benchmarkCase.sideEffectExpectation.expectedLogicalSideEffects;
    }

    private int wrongTargetCount(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        Object expectedTarget = benchmarkCase.input == null ? null : benchmarkCase.input.get("orderId");
        if (expectedTarget == null) return 0;
        int count = 0;
        for (Map<String, Object> effect : evidence.externalEffects) {
            Object target = effect.get("businessTarget");
            if (target != null && !String.valueOf(expectedTarget).equals(String.valueOf(target))) count++;
        }
        return count;
    }

    private int wrongPayloadCount(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        if (benchmarkCase.input == null) return 0;
        Object orderId = benchmarkCase.input.get("orderId");
        Object amount = benchmarkCase.input.get("refundAmount");
        if (orderId == null || amount == null) return 0;
        String expectedHash = sha256(String.valueOf(orderId) + "|" + String.valueOf(amount));
        int count = 0;
        for (Map<String, Object> effect : evidence.externalEffects) {
            Object payloadHash = effect.get("payloadHash");
            if (payloadHash != null && !expectedHash.equals(String.valueOf(payloadHash))) count++;
        }
        return count;
    }

    private boolean approvalSatisfied(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        if (benchmarkCase.approvalExpectation == null || !Boolean.TRUE.equals(benchmarkCase.approvalExpectation.required)) {
            return true;
        }
        return evidence.approvals.stream().anyMatch(approval ->
                approval != null
                        && approval.getApprovalId() != null
                        && "order.refund_execute".equals(approval.getToolCode())
                        && ("APPROVED".equalsIgnoreCase(approval.getStatus())
                        || "EXECUTING".equalsIgnoreCase(approval.getStatus())
                        || "EXECUTED".equalsIgnoreCase(approval.getStatus())
                        || "EXECUTION_FAILED".equalsIgnoreCase(approval.getStatus())));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private int payloadConflictCount(CollectedEvidence evidence) {
        int count = 0;
        for (Map<String, Object> decision : evidence.idempotencyDecisions) {
            if ("PAYLOAD_MISMATCH".equals(String.valueOf(decision.get("decision")))) count++;
        }
        return count;
    }

    private int intFact(CollectedEvidence evidence, String key) {
        Object value = evidence.businessFacts.get(key);
        return value instanceof Number n ? n.intValue() : 0;
    }

    private void addAttributionFailureReasons(EvaluationResult result, CollectedEvidence evidence) {
        boolean classified = false;
        for (Map<String, Object> attempt : evidence.idempotencyAttributionAttempts) {
            if (!Boolean.TRUE.equals(attempt.get("preIdempotencyBlocked"))) continue;
            String code = String.valueOf(attempt.getOrDefault("attributionCode", ""));
            if (code.contains("AUTHORIZATION")) {
                result.fail(FailureReasonCode.PRE_IDEMPOTENCY_AUTHORIZATION_BLOCK);
            } else if (code.contains("SCHEMA")) {
                result.fail(FailureReasonCode.PRE_IDEMPOTENCY_SCHEMA_BLOCK);
            } else if (code.contains("SCOPE")) {
                result.fail(FailureReasonCode.PRE_IDEMPOTENCY_SCOPE_BLOCK);
            } else if (code.contains("APPROVAL")) {
                result.fail(FailureReasonCode.PRE_IDEMPOTENCY_APPROVAL_BLOCK);
            } else {
                result.fail(FailureReasonCode.PRE_IDEMPOTENCY_UNKNOWN_BLOCK);
            }
            classified = true;
        }
        if (!classified) result.fail(FailureReasonCode.PRE_IDEMPOTENCY_UNKNOWN_BLOCK);
    }

}
