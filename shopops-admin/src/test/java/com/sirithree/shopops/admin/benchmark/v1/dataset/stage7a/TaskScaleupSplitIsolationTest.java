package com.sirithree.shopops.admin.benchmark.v1.dataset.stage7a;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test;
class TaskScaleupSplitIsolationTest { @Test void rootsAndParentsNeverCrossSplits() throws Exception { assertThat(Stage7ATaskDatasetSupport.crossSplitRootCount()).isZero(); assertThat(Stage7ATaskDatasetSupport.crossSplitParentCount()).isZero(); assertThat(Stage7ATaskDatasetSupport.testExclusiveRoots()).hasSize(63); }}
