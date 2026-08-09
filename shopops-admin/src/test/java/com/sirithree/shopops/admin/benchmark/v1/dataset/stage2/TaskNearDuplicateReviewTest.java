package com.sirithree.shopops.admin.benchmark.v1.dataset.stage2;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskNearDuplicateReviewTest {
    @Test
    void everyDeterministicNearDuplicateCandidateWasReviewedAndNoneRemainUnresolved() throws Exception {
        Map<String, Object> doc = Stage2TaskDatasetSupport.resource("/benchmark/v1/task/stage2/task-near-duplicate-review.json");
        assertThat(doc.get("candidateCount")).isEqualTo(213);
        assertThat(doc.get("reviewedCount")).isEqualTo(213);
        assertThat(doc.get("unresolvedCount")).isEqualTo(0);
        @SuppressWarnings("unchecked") List<Map<String, Object>> decisions = (List<Map<String, Object>>) doc.get("decisions");
        assertThat(decisions).hasSize(213).allSatisfy(decision -> {
            assertThat(String.valueOf(decision.get("reviewStatus"))).isEqualTo("MODEL_REVIEWED");
            assertThat(String.valueOf(decision.get("reviewDecision"))).isIn("KEEP_DISTINCT", "SAME_ROOT");
        });
    }
}
