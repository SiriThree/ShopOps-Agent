package com.sirithree.shopops.admin.benchmark.v1.evidence;

import java.time.Instant;

public record EvidenceRef(
        String sourceType,
        String sourceId,
        String summary,
        String hash,
        Instant timestamp
) {
}
