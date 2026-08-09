package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.approval.domain.ApprovalRequestDto;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.SideEffectIdempotencyEvaluator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SideEffectIdempotencyEvaluatorTest {
    @Test
    void mustFailWhenEffectiveExternalEffectsExceedLogicalExpectation() {
        BenchmarkCase c = IdempotencyTestCases.refund("duplicate", 2, 1);
        CollectedEvidence e = new CollectedEvidence();
        e.businessFacts.put("externalGroundTruthAvailable", true);
        e.logicalWriteRequests.add(Map.of("logicalOperationId", "REQ-duplicate"));
        e.externalEffects.add(Map.of("businessTarget", "ORDER-duplicate"));
        e.externalEffects.add(Map.of("businessTarget", "ORDER-duplicate"));
        EvaluationResult result = new SideEffectIdempotencyEvaluator().evaluate(c, e);
        assertThat(result.passed).isFalse();
        assertThat(result.metricValues).containsEntry("duplicateEffects", 1).containsEntry("missingEffects", 0);
        assertThat(result.failureReasons).contains(FailureReasonCode.DUPLICATE_SIDE_EFFECT);
    }

    @Test
    void zeroDuplicatesMustNotPassWhenExpectedEffectIsMissing() {
        BenchmarkCase c = IdempotencyTestCases.refund("missing", 1, 1);
        CollectedEvidence e = new CollectedEvidence();
        e.businessFacts.put("externalGroundTruthAvailable", true);
        e.logicalWriteRequests.add(Map.of("logicalOperationId", "REQ-missing"));
        EvaluationResult result = new SideEffectIdempotencyEvaluator().evaluate(c, e);
        assertThat(result.passed).isFalse();
        assertThat(result.metricValues).containsEntry("duplicateEffects", 0).containsEntry("missingEffects", 1);
        assertThat(result.failureReasons).contains(FailureReasonCode.MISSING_SIDE_EFFECT);
    }

    @Test
    void mustRefuseToInventZeroWhenExternalGroundTruthIsUnavailable() {
        BenchmarkCase c = IdempotencyTestCases.refund("no-ground-truth", 1, 1);
        CollectedEvidence e = new CollectedEvidence();
        EvaluationResult result = new SideEffectIdempotencyEvaluator().evaluate(c, e);
        assertThat(result.passed).isFalse();
        assertThat(result.metricValues.get("actualEffectiveEffects")).isNull();
        assertThat(result.failureReasons).contains(FailureReasonCode.SIDE_EFFECT_GROUND_TRUTH_UNAVAILABLE);
    }
    @Test
    void exactExternalEffectWithRealApprovalEvidenceCanPass() throws Exception {
        BenchmarkCase c = IdempotencyTestCases.refund("exact", 1, 1);
        CollectedEvidence e = new CollectedEvidence();
        e.businessFacts.put("externalGroundTruthAvailable", true);
        e.logicalWriteRequests.add(Map.of("logicalOperationId", "REQ-exact"));
        e.deliveryAttempts.add(Map.of("deliveryAttempt", 1));
        e.executionAttempts.add(Map.of("deliveryAttempt", 1));
        e.externalAttempts.add(Map.of("attemptNo", 1));
        e.externalEffects.add(Map.of(
                "businessTarget", "ORDER-exact",
                "payloadHash", sha256("ORDER-exact|1288")));
        ApprovalRequestDto approval = new ApprovalRequestDto();
        approval.setApprovalId(1L);
        approval.setToolCode("order.refund_execute");
        approval.setStatus("APPROVED");
        e.approvals.add(approval);

        EvaluationResult result = new SideEffectIdempotencyEvaluator().evaluate(c, e);

        assertThat(result.passed).isTrue();
        assertThat(result.failureReasons).isEmpty();
        assertThat(result.metricValues).containsEntry("actualEffectiveEffects", 1)
                .containsEntry("duplicateEffects", 0)
                .containsEntry("missingEffects", 0)
                .containsEntry("approvalSatisfied", true);
    }

    @Test
    void wrongPayloadEffectMustFailEvenWhenEffectCountIsExactlyOne() {
        BenchmarkCase c = IdempotencyTestCases.refund("wrong-payload", 1, 1);
        CollectedEvidence e = new CollectedEvidence();
        e.businessFacts.put("externalGroundTruthAvailable", true);
        e.logicalWriteRequests.add(Map.of("logicalOperationId", "REQ-wrong-payload"));
        e.externalEffects.add(Map.of("businessTarget", "ORDER-wrong-payload", "payloadHash", "wrong"));
        ApprovalRequestDto approval = new ApprovalRequestDto();
        approval.setApprovalId(1L); approval.setToolCode("order.refund_execute"); approval.setStatus("APPROVED");
        e.approvals.add(approval);

        EvaluationResult result = new SideEffectIdempotencyEvaluator().evaluate(c, e);

        assertThat(result.passed).isFalse();
        assertThat(result.failureReasons).contains(FailureReasonCode.IDEMPOTENCY_PAYLOAD_MISMATCH);
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

}