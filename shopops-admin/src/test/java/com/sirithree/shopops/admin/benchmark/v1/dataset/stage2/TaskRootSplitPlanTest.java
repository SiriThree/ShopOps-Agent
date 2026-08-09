package com.sirithree.shopops.admin.benchmark.v1.dataset.stage2;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskRootSplitPlanTest {
    @Test
    void splitPlanAssignsEverySemanticRootExactlyOnce() throws Exception {
        Map<String, Object> doc = Stage2TaskDatasetSupport.resource("/benchmark/v1/task/stage2/task-root-split-plan.json");
        @SuppressWarnings("unchecked") List<Map<String, Object>> roots = (List<Map<String, Object>>) doc.get("roots");
        assertThat(roots).hasSize(52);
        assertThat(new HashSet<>(roots.stream().map(root -> String.valueOf(root.get("semanticRootId"))).toList())).hasSize(52);
        assertThat(roots).allSatisfy(root -> {
            assertThat(String.valueOf(root.get("assignedSplit"))).isIn("dev", "validation", "test");
            assertThat(String.valueOf(root.get("assignmentReason"))).isNotBlank();
        });
    }
}
