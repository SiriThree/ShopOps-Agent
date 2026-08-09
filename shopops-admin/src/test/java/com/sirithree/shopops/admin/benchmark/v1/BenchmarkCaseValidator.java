package com.sirithree.shopops.admin.benchmark.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BenchmarkCaseValidator {
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final Set<String> ORIGINS = Set.of("LEGACY", "HAND_AUTHORED", "PERTURBED", "PUBLIC_DATA_DERIVED");

    public List<String> validate(BenchmarkCase c) {
        List<String> errors = new ArrayList<>();
        if (c == null) return List.of("case is null");
        required(c.caseId, "caseId", errors);
        if (c.benchmarkType == null) errors.add("benchmarkType is required");
        required(c.scenario, "scenario", errors);
        required(c.difficulty, "difficulty", errors);
        if (c.difficulty != null && !DIFFICULTIES.contains(c.difficulty.toUpperCase())) {
            errors.add("difficulty must be EASY, MEDIUM or HARD");
        }
        required(c.goldVersion, "goldVersion", errors);
        if (c.input == null || c.input.isEmpty()) errors.add("input must not be empty");
        if (c.identity == null || c.identity.isEmpty()) errors.add("identity must not be empty");
        if (c.expectedOutcome == null || c.expectedOutcome.isEmpty()) errors.add("expectedOutcome must not be empty");
        if (c.sideEffectExpectation == null) errors.add("sideEffectExpectation is required");
        else if (c.sideEffectExpectation.expectedLogicalSideEffects < 0) errors.add("expectedLogicalSideEffects must be >= 0");
        if (c.approvalExpectation == null) errors.add("approvalExpectation is required");
        if (hasText(c.origin) && !ORIGINS.contains(c.origin.toUpperCase())) {
            errors.add("origin must be LEGACY, HAND_AUTHORED, PERTURBED or PUBLIC_DATA_DERIVED");
        }
        if ("PERTURBED".equalsIgnoreCase(c.origin)) {
            required(c.parentCaseId, "parentCaseId for PERTURBED case", errors);
            required(c.perturbationType, "perturbationType for PERTURBED case", errors);
        }
        if (c.benchmarkType == BenchmarkType.TASK && c.goldVersion != null
                && (c.goldVersion.contains("v1.2-task-stage2") || c.goldVersion.contains("v1.3-task-stage7a"))) {
            required(c.semanticRootId, "semanticRootId for Stage2 TASK case", errors);
            required(c.goldSourceType, "goldSourceType for Stage2 TASK case", errors);
            required(c.reviewStatus, "reviewStatus for Stage2 TASK case", errors);
            if (!Set.of("UNREVIEWED", "MODEL_REVIEWED", "HUMAN_REVIEWED", "HUMAN_SPOT_CHECKED").contains(c.reviewStatus == null ? "" : c.reviewStatus.toUpperCase())) {
                errors.add("reviewStatus is invalid for Stage2 TASK case");
            }
            if (c.reservedForHeldOut == null) errors.add("reservedForHeldOut is required for Stage2 TASK case");
            if (Boolean.TRUE.equals(c.reservedForHeldOut) && (c.tags == null || !c.tags.contains("HELD_OUT"))) {
                errors.add("reservedForHeldOut Stage2 TASK case must include HELD_OUT tag");
            }
            if (Boolean.TRUE.equals(c.humanReviewed) && !"HUMAN_REVIEWED".equalsIgnoreCase(c.reviewStatus)) {
                errors.add("humanReviewed=true requires HUMAN_REVIEWED reviewStatus for versioned TASK case");
            }
            if (c.caseId != null && c.caseId.startsWith("stage7a-") && Boolean.TRUE.equals(c.humanReviewed)) {
                errors.add("new Stage7A TASK cases cannot set humanReviewed=true");
            }
        }
        if (c.benchmarkType == BenchmarkType.IDEMPOTENCY) {
            required(c.operationType, "operationType for IDEMPOTENCY case", errors);
            if (c.logicalWriteCount == null || c.logicalWriteCount <= 0) {
                errors.add("logicalWriteCount must be > 0 for IDEMPOTENCY case");
            }
            if (c.expectedEffectiveSideEffects == null || c.expectedEffectiveSideEffects < 0) {
                errors.add("expectedEffectiveSideEffects must be >= 0 for IDEMPOTENCY case");
            }
            if (c.sideEffectExpectation != null && c.expectedEffectiveSideEffects != null
                    && c.sideEffectExpectation.expectedLogicalSideEffects != c.expectedEffectiveSideEffects) {
                errors.add("expectedEffectiveSideEffects must match sideEffectExpectation.expectedLogicalSideEffects");
            }
            if (c.deliveryPattern == null || c.deliveryPattern.isEmpty()) {
                errors.add("deliveryPattern must not be empty for IDEMPOTENCY case");
            }
            if (c.concurrency == null || c.concurrency.isEmpty()) {
                errors.add("concurrency must not be empty for IDEMPOTENCY case");
            }
            if (c.idempotencyExpectation == null || c.idempotencyExpectation.isEmpty()) {
                errors.add("idempotencyExpectation must not be empty for IDEMPOTENCY case");
            }
            required(c.externalSystemMode, "externalSystemMode for IDEMPOTENCY case", errors);
            if (c.goldVersion != null && c.goldVersion.contains("v1.3-idempotency-stage5")) {
                required(c.semanticRootId, "semanticRootId for Stage5 IDEMPOTENCY case", errors);
                required(c.goldSourceType, "goldSourceType for Stage5 IDEMPOTENCY case", errors);
                required(c.reviewStatus, "reviewStatus for Stage5 IDEMPOTENCY case", errors);
                if (c.reservedForHeldOut == null) errors.add("reservedForHeldOut is required for Stage5 IDEMPOTENCY case");
                if (Boolean.TRUE.equals(c.reservedForHeldOut) && (c.tags == null || !c.tags.contains("HELD_OUT"))) {
                    errors.add("reservedForHeldOut Stage5 IDEMPOTENCY case must include HELD_OUT tag");
                }
                if (c.caseId != null && c.caseId.startsWith("stage5-") && Boolean.TRUE.equals(c.humanReviewed)) {
                    errors.add("new Stage5 IDEMPOTENCY cases cannot set humanReviewed=true");
                }
                if (c.idempotencyExpectation != null) {
                    required(String.valueOf(c.idempotencyExpectation.getOrDefault("keyRelation", "")), "keyRelation for Stage5 IDEMPOTENCY case", errors);
                    required(String.valueOf(c.idempotencyExpectation.getOrDefault("payloadRelation", "")), "payloadRelation for Stage5 IDEMPOTENCY case", errors);
                    required(String.valueOf(c.idempotencyExpectation.getOrDefault("repeatPattern", "")), "repeatPattern for Stage5 IDEMPOTENCY case", errors);
                    required(String.valueOf(c.idempotencyExpectation.getOrDefault("faultSemantics", "")), "faultSemantics for Stage5 IDEMPOTENCY case", errors);
                }
            }
        }
        if (c.benchmarkType == BenchmarkType.RECOVERY) {
            required(c.operationType, "operationType for RECOVERY case", errors);
            required(c.initialLocalState, "initialLocalState for RECOVERY case", errors);
            required(c.initialExternalState, "initialExternalState for RECOVERY case", errors);
            required(c.expectedExternalState, "expectedExternalState for RECOVERY case", errors);
            if (c.expectedConvergence == null) errors.add("expectedConvergence is required for RECOVERY case");
            if (c.maxRecoveryAttempts == null || c.maxRecoveryAttempts <= 0) errors.add("maxRecoveryAttempts must be > 0 for RECOVERY case");
            if (c.expectedTerminalStates == null || c.expectedTerminalStates.isEmpty()) errors.add("expectedTerminalStates must not be empty for RECOVERY case");
            required(c.externalSystemMode, "externalSystemMode for RECOVERY case", errors);
        }
        if (c.benchmarkType == BenchmarkType.GOVERNANCE && (hasText(c.governanceCaseClass) || (c.goldVersion != null && (c.goldVersion.contains("v1.4") || c.goldVersion.contains("v1.5-governance-stage3") || c.goldVersion.contains("v1.6-governance-stage7b"))))) {
            required(c.governanceCaseClass, "governanceCaseClass for GOVERNANCE case", errors);
            if (hasText(c.governanceCaseClass)
                    && !Set.of("NEGATIVE", "POSITIVE").contains(c.governanceCaseClass.toUpperCase())) {
                errors.add("governanceCaseClass must be NEGATIVE or POSITIVE");
            }
            required(c.attackType, "attackType for GOVERNANCE case", errors);
            required(c.toolCode, "toolCode for GOVERNANCE case", errors);
            if (c.arguments == null) errors.add("arguments is required for GOVERNANCE case");
            required(c.expectedDecision, "expectedDecision for GOVERNANCE case", errors);
            if (hasText(c.expectedDecision)
                    && !Set.of("ALLOWED", "REQUIRES_APPROVAL", "BLOCKED", "ERROR").contains(c.expectedDecision.toUpperCase())) {
                errors.add("expectedDecision must be ALLOWED, REQUIRES_APPROVAL, BLOCKED or ERROR");
            }
            if (c.externalSideEffectAllowed == null) errors.add("externalSideEffectAllowed is required for GOVERNANCE case");
            required(c.authorizationMode, "authorizationMode for GOVERNANCE case", errors);
            if (c.goldVersion != null && (c.goldVersion.contains("v1.5-governance-stage3") || c.goldVersion.contains("v1.6-governance-stage7b"))) {
                required(c.semanticRootId, "semanticRootId for Stage3 GOVERNANCE case", errors);
                required(c.goldSourceType, "goldSourceType for Stage3 GOVERNANCE case", errors);
                required(c.reviewStatus, "reviewStatus for Stage3 GOVERNANCE case", errors);
                if (c.reservedForHeldOut == null) errors.add("reservedForHeldOut is required for Stage3 GOVERNANCE case");
                if (Boolean.TRUE.equals(c.reservedForHeldOut) && (c.tags == null || !c.tags.contains("HELD_OUT"))) {
                    errors.add("reservedForHeldOut Stage3 GOVERNANCE case must include HELD_OUT tag");
                }
                if (c.caseId != null && (c.caseId.startsWith("stage3-") || c.caseId.startsWith("stage7b-")) && Boolean.TRUE.equals(c.humanReviewed)) {
                    errors.add("new versioned GOVERNANCE cases cannot set humanReviewed=true");
                }
            }
        }
        return errors;
    }

    public void requireValid(BenchmarkCase c) {
        List<String> errors = validate(c);
        if (!errors.isEmpty()) throw new IllegalArgumentException("Invalid benchmark case "
                + (c == null ? "<null>" : c.caseId) + ": " + String.join("; ", errors));
    }

    private void required(String value, String name, List<String> errors) {
        if (!hasText(value)) errors.add(name + " is required");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
