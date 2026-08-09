package com.sirithree.shopops.admin.benchmark.v1.evaluation.stage6;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.AbstractRefundIdempotencyIntegrationTestSupport;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.FreshReplayApprovalFactory;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyTestCases;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class IdempotencyCanonicalPayloadAcrossFreshApprovalTest extends AbstractRefundIdempotencyIntegrationTestSupport {
    @Test void freshApprovalIdsDoNotChangeCanonicalBusinessPayloadBinding() {
        var input = new LinkedHashMap<>(IdempotencyTestCases.refund("stage6-hash", 1, 1).input);
        var factory = new FreshReplayApprovalFactory(toolGateway, approvals);
        var a = factory.createApproved(1L, 1L, 1L, 996010L, "stage6-hash-a", input);
        var b = factory.createApproved(1L, 1L, 1L, 996010L, "stage6-hash-b", input);
        assertThat(a.approvalId()).isNotEqualTo(b.approvalId());
        assertThat(a.inputHash()).isEqualTo(b.inputHash());
        assertThat(a.inputSummary()).isEqualTo(b.inputSummary());
        assertThat(a.businessObjectId()).isEqualTo(b.businessObjectId());
    }
}
