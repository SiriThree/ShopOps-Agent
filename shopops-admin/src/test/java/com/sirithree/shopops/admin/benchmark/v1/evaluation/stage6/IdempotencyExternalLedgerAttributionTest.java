package com.sirithree.shopops.admin.benchmark.v1.evaluation.stage6;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSystemMode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RecordingRefundExternalSystem;
import org.junit.jupiter.api.Test;

class IdempotencyExternalLedgerAttributionTest {
    @Test void ledgerSeparatesExternalAttemptsFromEffectiveEffects() {
        var external = new RecordingRefundExternalSystem();
        external.reset(ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL);
        external.execute("OP-A", "ORDER-A", 100, "failure");
        external.execute("OP-B", "ORDER-B", 100, "success");
        assertThat(external.attempts()).hasSize(2);
        assertThat(external.effects()).hasSize(1);
        assertThat(external.attempts().get(0).outcome()).isEqualTo("REJECTED");
        assertThat(external.effects().get(0).logicalOperationId()).isEqualTo("OP-B");
    }
}
