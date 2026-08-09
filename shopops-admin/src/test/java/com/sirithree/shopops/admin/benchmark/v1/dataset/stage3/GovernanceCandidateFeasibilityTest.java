package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GovernanceCandidateFeasibilityTest {
    @Test
    void blueprintRecordsAcceptedAndRejectedCandidatesWithReasons() throws Exception {
        Map<String, Object> doc = Stage3GovernanceDatasetSupport.resource("/benchmark/v1/governance/stage3/governance-root-blueprints.json");
        @SuppressWarnings("unchecked") List<Map<String, Object>> roots = (List<Map<String, Object>>) doc.get("roots");
        assertThat(doc.get("proposedRootCount")).isEqualTo(29);
        assertThat(doc.get("acceptedRootCount")).isEqualTo(23);
        assertThat(doc.get("rejectedRootCount")).isEqualTo(6);
        assertThat(roots).hasSize(29);
        assertThat(roots.stream().filter(root -> "ACCEPTED".equals(root.get("feasibilityStatus"))).count()).isEqualTo(23);
        var rejected = roots.stream().filter(root -> String.valueOf(root.getOrDefault("status", "")).startsWith("REJECTED_")).toList();
        assertThat(rejected).hasSize(6);
        assertThat(rejected).allSatisfy(root -> {
            assertThat(String.valueOf(root.get("status"))).startsWith("REJECTED_");
            assertThat(String.valueOf(root.get("reason"))).isNotBlank();
        });
    }
}
