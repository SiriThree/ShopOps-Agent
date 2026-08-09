package com.sirithree.shopops.admin.benchmark.v1.dataset.stage2;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TaskCrossSplitSemanticRootTest {
    @Test
    void noTaskSemanticRootOrParentLineageCrossesSplits() throws Exception {
        assertThat(Stage2TaskDatasetSupport.crossSplitRootCount()).isZero();
        assertThat(Stage2TaskDatasetSupport.crossSplitParentCount()).isZero();
    }
}
