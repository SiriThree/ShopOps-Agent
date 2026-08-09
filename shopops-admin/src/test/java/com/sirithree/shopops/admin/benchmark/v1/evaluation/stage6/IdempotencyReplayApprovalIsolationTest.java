package com.sirithree.shopops.admin.benchmark.v1.evaluation.stage6;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class IdempotencyReplayApprovalIsolationTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test
    void everyIntendedReplayUsesFreshApprovalButSameLogicalOperation() {
        var record = execute(IdempotencyTestCases.refund("stage6-fresh-approval", 3, 1));
        var ids = record.approvalEvents.stream().map(v -> v.get("approvalId")).collect(java.util.stream.Collectors.toSet());
        assertThat(record.approvalEvents).hasSize(3);
        assertThat(ids).hasSize(3);
        assertThat(record.observedFacts).containsEntry("attributionEligible", true)
                .containsEntry("idempotencyBoundaryReachedAttempts", 3)
                .containsEntry("preIdempotencyBlockedAttempts", 0);
        assertThat(record.writeOperations).extracting(v -> v.get("logicalOperationId")).containsOnly("REQ-stage6-fresh-approval");
    }
}
