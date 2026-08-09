package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GovernanceGoldSourceTest {
    private static final Set<String> ALLOWED = Set.of("SECURITY_POLICY_DERIVED", "DOMAIN_INVARIANT", "HAND_AUTHORED", "LEGACY_MIGRATED");

    @Test
    void everyGovernanceCaseHasKnownIndependentGoldSource() throws Exception {
        assertThat(Stage3GovernanceDatasetSupport.governanceCases()).allSatisfy(view ->
                assertThat(view.benchmarkCase().goldSourceType).isIn(ALLOWED.toArray(String[]::new)));
    }
}
