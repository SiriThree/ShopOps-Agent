package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GovernancePositiveControlCoverageTest {
    @Test
    void positiveRootsAndHeldOutControlsImproveOverStage1() throws Exception {
        var cases = Stage3GovernanceDatasetSupport.governanceCases();
        Set<String> positiveRoots = cases.stream().filter(v -> Stage3GovernanceDatasetSupport.isPositive(v.benchmarkCase()))
                .map(v -> v.benchmarkCase().semanticRootId).collect(Collectors.toSet());
        Set<String> testPositiveRoots = cases.stream().filter(v -> "test".equals(v.split()))
                .filter(v -> Stage3GovernanceDatasetSupport.isPositive(v.benchmarkCase()))
                .map(v -> v.benchmarkCase().semanticRootId).collect(Collectors.toSet());
        assertThat(positiveRoots).hasSize(22);
        assertThat(testPositiveRoots).hasSize(12);
        assertThat(positiveRoots.size()).isGreaterThan(5);
        assertThat(testPositiveRoots.size()).isGreaterThan(1);
    }
}
