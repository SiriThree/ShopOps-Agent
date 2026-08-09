package com.sirithree.shopops.admin.benchmark.v1.evaluation.stage6;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.FreshReplayApprovalFactory;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyAttributionClassifier;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class IdempotencyPreBoundaryBlockDetectionTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test void reusedConsumedApprovalIsGovernanceBlockNotIdempotencyEvidence() {
        var c = IdempotencyTestCases.refund("stage6-consumed-approval", 1, 1);
        var input = new LinkedHashMap<>(c.input);
        long taskId = 996001L;
        var factory = new FreshReplayApprovalFactory(toolGateway, approvals);
        var approval = factory.createApproved(1L, 1L, 1L, taskId, "stage6-consumed", input);
        var first = toolGateway.invoke(factory.context(1L, 1L, 1L, taskId, "stage6-consumed-1", approval.approvalId()), "order.refund_execute", input);
        assertThat(first.getSuccess()).isTrue();
        var replay = toolGateway.invoke(factory.context(1L, 1L, 1L, taskId, "stage6-consumed-2", approval.approvalId()), "order.refund_execute", input);
        assertThat(replay.getErrorCode()).isEqualTo("APPROVAL_NOT_APPROVED");
        var status = approvals.get(1L, 1L, approval.approvalId()).orElseThrow().getStatus();
        var attribution = IdempotencyAttributionClassifier.classify(2, "REPLAY", true, approval.approvalId(), status,
                replay, true, false);
        assertThat(attribution.writeOperationBoundaryReached()).isFalse();
        assertThat(attribution.preIdempotencyBlocked()).isTrue();
        assertThat(attribution.attributionCode()).isEqualTo("ATTRIBUTION_INVALID_APPROVAL_BLOCK");
    }
}
