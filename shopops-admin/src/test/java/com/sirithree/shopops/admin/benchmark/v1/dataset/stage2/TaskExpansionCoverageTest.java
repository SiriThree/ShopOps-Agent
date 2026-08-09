package com.sirithree.shopops.admin.benchmark.v1.dataset.stage2;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TaskExpansionCoverageTest {
    @Test
    void expansionAddsIndependentBusinessStateRootsInsteadOfOnlyParaphrases() throws Exception {
        var cases = Stage2TaskDatasetSupport.taskCases();
        assertThat(cases).hasSize(93);
        assertThat(Stage2TaskDatasetSupport.roots()).hasSize(52);
        assertThat(Stage2TaskDatasetSupport.heldOutRoots()).hasSize(23);
        Set<String> requiredTags = Set.of("EMPTY_RESULT", "PARTIAL_DATA", "DATE_BOUNDARY");
        for (String tag : requiredTags) {
            assertThat(cases.stream().filter(view -> view.benchmarkCase().tags.contains(tag)).toList())
                    .as(tag).isNotEmpty();
        }
    }
}
