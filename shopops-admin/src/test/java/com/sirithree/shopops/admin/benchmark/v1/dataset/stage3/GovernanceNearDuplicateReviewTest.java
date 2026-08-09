package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GovernanceNearDuplicateReviewTest {
    @Test
    void everyNearDuplicateCandidateHasExplicitReviewDecision() throws Exception {
        Map<String, Object> doc = Stage3GovernanceDatasetSupport.resource("/benchmark/v1/governance/stage3/governance-near-duplicate-review.json");
        @SuppressWarnings("unchecked") List<Map<String, Object>> candidates = (List<Map<String, Object>>) doc.get("candidates");
        assertThat(doc.get("candidateCount")).isEqualTo(555);
        assertThat(doc.get("reviewedCount")).isEqualTo(555);
        assertThat(doc.get("unresolvedCount")).isEqualTo(0);
        assertThat(candidates).hasSize(555);
        assertThat(candidates).allSatisfy(candidate -> assertThat(String.valueOf(candidate.get("decision")))
                .isIn("KEEP_DISTINCT", "KEEP_DISTINCT_PAIRED_CONTROL", "SAME_ROOT"));
    }
}
