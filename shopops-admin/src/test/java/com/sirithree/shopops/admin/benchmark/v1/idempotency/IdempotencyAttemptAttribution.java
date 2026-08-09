package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import java.util.LinkedHashMap;
import java.util.Map;

/** Machine-readable per-attempt attribution evidence for Stage 6. */
public record IdempotencyAttemptAttribution(
        int attemptNo,
        String attemptKind,
        boolean intendedReplay,
        Long approvalId,
        boolean gatewayReached,
        boolean authorizationPassed,
        boolean schemaPassed,
        boolean businessScopePassed,
        boolean approvalPassed,
        boolean writeExecutorReached,
        boolean writeOperationBoundaryReached,
        boolean externalAttemptObserved,
        boolean preIdempotencyBlocked,
        String attributionCode,
        String resultStatus,
        String errorCode) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("attemptNo", attemptNo);
        map.put("attemptKind", attemptKind);
        map.put("intendedReplay", intendedReplay);
        map.put("approvalId", approvalId == null ? -1L : approvalId);
        map.put("gatewayReached", gatewayReached);
        map.put("authorizationPassed", authorizationPassed);
        map.put("schemaPassed", schemaPassed);
        map.put("businessScopePassed", businessScopePassed);
        map.put("approvalPassed", approvalPassed);
        map.put("writeExecutorReached", writeExecutorReached);
        map.put("writeOperationBoundaryReached", writeOperationBoundaryReached);
        map.put("externalAttemptObserved", externalAttemptObserved);
        map.put("preIdempotencyBlocked", preIdempotencyBlocked);
        map.put("attributionCode", attributionCode == null ? "" : attributionCode);
        map.put("resultStatus", resultStatus == null ? "" : resultStatus);
        map.put("errorCode", errorCode == null ? "" : errorCode);
        return map;
    }
}
