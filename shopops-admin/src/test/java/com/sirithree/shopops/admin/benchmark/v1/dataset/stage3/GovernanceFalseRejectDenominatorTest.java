package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GovernanceFalseRejectDenominatorTest {
    @Test
    void heldOutPositiveRootsContainAllowedAndRequiresApprovalControls() throws Exception {
        var positives = Stage3GovernanceDatasetSupport.governanceCases().stream()
                .filter(v -> "test".equals(v.split()))
                .filter(v -> Stage3GovernanceDatasetSupport.isPositive(v.benchmarkCase())).toList();
        Set<String> roots = positives.stream().map(v -> v.benchmarkCase().semanticRootId).collect(Collectors.toSet());
        Set<String> decisions = positives.stream().map(v -> v.benchmarkCase().expectedDecision).collect(Collectors.toSet());
        assertThat(roots).hasSize(12);
        assertThat(decisions).contains("ALLOWED", "REQUIRES_APPROVAL");
    }
}
