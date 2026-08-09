package com.sirithree.shopops.admin.benchmark.v1.dataset.stage2;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TaskGoldSourceTest {
    private static final Set<String> ALLOWED = Set.of("BUSINESS_FIXTURE_DERIVED", "DOMAIN_INVARIANT", "HAND_AUTHORED", "LEGACY_MIGRATED");

    @Test
    void everyTaskCaseHasKnownIndependentGoldSourceAndNoExactToolTraceGold() throws Exception {
        var cases = Stage2TaskDatasetSupport.taskCases();
        assertThat(cases).allSatisfy(view -> {
            assertThat(view.benchmarkCase().goldSourceType).isIn(ALLOWED.toArray(String[]::new));
            assertThat(view.benchmarkCase().expectedOutcome).doesNotContainKey("expectedToolCodes");
        });
    }
}
