package com.sirithree.shopops.admin.benchmark.v1.evaluation.stage6;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.FreshReplayApprovalFactory;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyAttributionClassifier;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class IdempotencyPayloadConflictAcrossFreshApprovalTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test void differentBusinessPayloadPassesApprovalThenConflictsAtIdempotencyBoundary() {
        var base = new LinkedHashMap<>(IdempotencyTestCases.refund("stage6-conflict", 1, 1).input);
        long taskId = 996020L;
        var factory = new FreshReplayApprovalFactory(toolGateway, approvals);
        var a = factory.createApproved(1L, 1L, 1L, taskId, "stage6-conflict-a", base);
        var first = toolGateway.invoke(factory.context(1L, 1L, 1L, taskId, "stage6-conflict-run-a", a.approvalId()), "order.refund_execute", base);
        assertThat(first.getSuccess()).isTrue();
        var changed = new LinkedHashMap<>(base);
        changed.put("refundAmount", ((Number) base.get("refundAmount")).intValue() + 1);
        var b = factory.createApproved(1L, 1L, 1L, taskId, "stage6-conflict-b", changed);
        assertThat(b.inputHash()).isNotEqualTo(a.inputHash());
        var second = toolGateway.invoke(factory.context(1L, 1L, 1L, taskId, "stage6-conflict-run-b", b.approvalId()), "order.refund_execute", changed);
        assertThat(second.getErrorCode()).isEqualTo("IDEMPOTENCY_PAYLOAD_MISMATCH");
        var status = approvals.get(1L, 1L, b.approvalId()).orElseThrow().getStatus();
        var attribution = IdempotencyAttributionClassifier.classify(2, "PAYLOAD_CONFLICT", true, b.approvalId(), status,
                second, true, false);
        assertThat(attribution.approvalPassed()).isTrue();
        assertThat(attribution.writeOperationBoundaryReached()).isTrue();
        assertThat(attribution.attributionCode()).isEqualTo("IDEMPOTENCY_PAYLOAD_CONFLICT");
    }
}
