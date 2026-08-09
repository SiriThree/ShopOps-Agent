package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSystemMode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RecordingRefundExternalSystem;
import org.junit.jupiter.api.Test;

class SideEffectLedgerTest {
    @Test
    void nonIdempotentExternalMustRecordEveryAcceptedCallAsSeparateEffect() {
        RecordingRefundExternalSystem external = new RecordingRefundExternalSystem();
        external.reset(ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL);
        external.execute("REQ-1", "ORDER-1", 100, "success");
        external.execute("REQ-1", "ORDER-1", 100, "success");
        assertThat(external.attempts()).hasSize(2);
        assertThat(external.effects()).hasSize(2);
        assertThat(external.effects()).extracting(effect -> effect.externalEffectId()).doesNotHaveDuplicates();
    }

    @Test
    void idempotentExternalComparisonModeMustCollapseExternalEffectButStillRecordAttempts() {
        RecordingRefundExternalSystem external = new RecordingRefundExternalSystem();
        external.reset(ExternalSystemMode.IDEMPOTENT_EXTERNAL);
        external.execute("REQ-1", "ORDER-1", 100, "success");
        external.execute("REQ-1", "ORDER-1", 100, "success");
        assertThat(external.attempts()).hasSize(2);
        assertThat(external.effects()).hasSize(1);
    }
}
