package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GovernanceRootSplitPlanTest {
    @Test
    void splitPlanAssignsEveryGovernanceRootExactlyOnce() throws Exception {
        Map<String, Object> doc = Stage3GovernanceDatasetSupport.resource("/benchmark/v1/governance/stage3/governance-root-split-plan.json");
        @SuppressWarnings("unchecked") List<Map<String, Object>> roots = (List<Map<String, Object>>) doc.get("roots");
        assertThat(roots).hasSize(46);
        assertThat(new HashSet<>(roots.stream().map(root -> String.valueOf(root.get("semanticRootId"))).toList())).hasSize(46);
        assertThat(roots).allSatisfy(root -> {
            assertThat(String.valueOf(root.get("assignedSplit"))).isIn("dev", "validation", "test");
            assertThat(String.valueOf(root.get("assignmentReason"))).isNotBlank();
        });
    }
}
