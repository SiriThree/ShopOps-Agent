package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GovernanceCrossSplitSemanticRootTest {
    @Test
    void governanceRootsAndParentsDoNotCrossSplits() throws Exception {
        assertThat(Stage3GovernanceDatasetSupport.crossSplitRootCount()).isZero();
        assertThat(Stage3GovernanceDatasetSupport.crossSplitParentCount()).isZero();
        assertThat(Stage3GovernanceDatasetSupport.testExclusiveRoots()).hasSize(18);
    }
}
