package com.sirithree.shopops.admin.benchmark.v1.dataset.stage5;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test;
class IdempotencyCrossSplitSemanticRootTest { @Test void rootsAndParentsNeverCrossSplits() throws Exception { assertThat(Stage5IdempotencyDatasetSupport.crossSplitRootCount()).isZero(); assertThat(Stage5IdempotencyDatasetSupport.crossSplitParentCount()).isZero(); assertThat(Stage5IdempotencyDatasetSupport.testExclusiveRoots()).hasSize(4); }}
