package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DatasetGoldProvenanceValidationTest {
    private static final Set<String> ALLOWED = Set.of(
            "BUSINESS_FIXTURE_DERIVED", "DOMAIN_INVARIANT", "FAULT_CONTRACT_DERIVED",
            "SECURITY_POLICY_DERIVED", "HAND_AUTHORED", "LEGACY_MIGRATED", "MODEL_GENERATED", "UNKNOWN");

    @Test
    void everyDedicatedCaseHasAuditedGoldSourceAndNoUnknownStage1Source() throws Exception {
        var dedicated = Stage1DatasetAuditSupport.dedicated();
        assertThat(dedicated).allSatisfy(view -> assertThat(ALLOWED).contains(view.metadata().goldSourceType()));
        assertThat(dedicated).extracting(view -> view.metadata().goldSourceType()).doesNotContain("UNKNOWN");
    }
}
