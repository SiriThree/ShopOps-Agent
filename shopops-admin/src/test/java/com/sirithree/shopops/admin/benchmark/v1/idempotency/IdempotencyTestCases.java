package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import java.util.List;
import java.util.Map;

public final class IdempotencyTestCases {
    public static BenchmarkCase refund(String caseId, int attempts, int workers) {
        BenchmarkCase c = new BenchmarkCase();
        c.caseId = caseId;
        c.benchmarkType = BenchmarkType.IDEMPOTENCY;
        c.scenario = workers > 1 ? "CONCURRENT_RETRY" : attempts > 1 ? "SEQUENTIAL_RETRY" : "BASELINE_SINGLE_DELIVERY";
        c.difficulty = workers > 1 ? "HARD" : "MEDIUM";
        c.input.putAll(Map.of(
                "shopId", 1,
                "orderId", "ORDER-" + caseId,
                "refundAmount", 1288,
                "operationRequestId", "REQ-" + caseId,
                "simulation", "success"));
        c.identity.putAll(Map.of("tenantId", 1, "shopId", 1, "userId", 1));
        c.expectedOutcome.put("expectedExternalEffects", 1);
        c.requiredCapabilities.addAll(List.of("REFUND_WRITE", "IDEMPOTENCY"));
        c.acceptableTools.add("order.refund_execute");
        c.sideEffectExpectation.expectedLogicalSideEffects = 1;
        c.sideEffectExpectation.allowedEffectTypes.add("REFUND");
        c.approvalExpectation.required = true;
        c.approvalExpectation.mustBlockBeforeApproval = true;
        c.approvalExpectation.requiredRiskLevel = "HIGH";
        c.goldVersion = "shopopsbench-gold-v1.2-idempotency";
        c.origin = "HAND_AUTHORED";
        c.humanReviewed = true;
        c.operationType = "order.refund_execute";
        c.logicalWriteCount = 1;
        c.expectedEffectiveSideEffects = 1;
        c.deliveryPattern.putAll(Map.of("mode", workers > 1 ? "CONCURRENT" : "RETRY", "attempts", attempts));
        c.concurrency.putAll(Map.of("workers", workers, "simultaneous", workers > 1));
        c.idempotencyExpectation.putAll(Map.of("sameKeyBehavior", "REPLAY", "payloadConflictBehavior", "NOT_EXERCISED", "exercisePayloadConflict", false));
        c.externalSystemMode = ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL.name();
        c.faultSeed = 3301L;
        return c;
    }

    private IdempotencyTestCases() { }
}
