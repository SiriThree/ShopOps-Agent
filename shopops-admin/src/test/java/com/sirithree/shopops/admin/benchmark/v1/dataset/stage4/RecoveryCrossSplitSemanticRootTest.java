package com.sirithree.shopops.admin.benchmark.v1.dataset.stage4;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test;
class RecoveryCrossSplitSemanticRootTest { @Test void recoveryRootsAndParentsDoNotCrossSplits() throws Exception { assertThat(Stage4RecoveryDatasetSupport.crossSplitRootCount()).isZero(); assertThat(Stage4RecoveryDatasetSupport.crossSplitParentCount()).isZero(); }}
