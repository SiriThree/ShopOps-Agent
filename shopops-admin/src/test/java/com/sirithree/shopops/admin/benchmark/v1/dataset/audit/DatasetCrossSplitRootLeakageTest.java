package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DatasetCrossSplitRootLeakageTest {
    @Test
    void stage1BaselineMakesCurrentCrossSplitLeakageExplicitInsteadOfHidingIt() throws Exception {
        var dedicated = Stage1DatasetAuditSupport.dedicated();
        assertThat(Stage1DatasetAuditSupport.crossSplitRootCount(dedicated)).isEqualTo(22);
        assertThat(Stage1DatasetAuditSupport.heldOutRootCount(dedicated)).isEqualTo(48);
        assertThat(Stage1DatasetAuditSupport.testExclusiveRootCount(dedicated)).isEqualTo(26);
    }
}
