package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import java.time.Instant;

public record ExternalAttempt(
        long attemptNo,
        String operationType,
        String logicalOperationId,
        String businessTarget,
        String payloadHash,
        String simulation,
        String outcome,
        String externalEffectId,
        Instant attemptedAt) {
}
