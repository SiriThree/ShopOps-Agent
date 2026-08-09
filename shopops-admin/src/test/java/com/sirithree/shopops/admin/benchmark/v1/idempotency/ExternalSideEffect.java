package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import java.time.Instant;

public record ExternalSideEffect(
        String externalEffectId,
        String operationType,
        String logicalOperationId,
        String businessTarget,
        String payloadHash,
        Instant acceptedAt,
        String externalStatus) {
}
