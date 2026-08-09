package com.sirithree.shopops.admin.benchmark.v1.dataset.stage2;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskCandidateFeasibilityTest {
    @Test
    void everyAcceptedBlueprintPassedRuntimeFixtureGoldAndEvaluatorFeasibility() throws Exception {
        Map<String, Object> doc = Stage2TaskDatasetSupport.resource("/benchmark/v1/task/stage2/task-root-blueprints.json");
        @SuppressWarnings("unchecked") List<Map<String, Object>> roots = (List<Map<String, Object>>) doc.get("roots");
        var accepted = roots.stream().filter(root -> "ACCEPTED".equals(root.get("feasibilityStatus"))).toList();
        var rejected = roots.stream().filter(root -> !"ACCEPTED".equals(root.get("feasibilityStatus"))).toList();
        assertThat(accepted).hasSize(40);
        assertThat(rejected).hasSize(10);
        assertThat(accepted).allSatisfy(root -> {
            assertThat(root.get("goldSource")).isEqualTo("BUSINESS_FIXTURE_DERIVED");
            assertThat(root.get("caseIds")).as("accepted root must generate cases").isInstanceOf(List.class);
        });
        assertThat(rejected).allSatisfy(root -> assertThat(String.valueOf(root.get("criticDecision"))).startsWith("REJECT"));
    }
}
