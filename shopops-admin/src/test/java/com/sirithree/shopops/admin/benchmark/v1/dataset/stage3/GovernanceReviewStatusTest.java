package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GovernanceReviewStatusTest {
    @Test
    void newStage3CasesAreModelReviewedAndNeverInventHumanReview() throws Exception {
        var newCases = Stage3GovernanceDatasetSupport.governanceCases().stream()
                .map(Stage3GovernanceDatasetSupport.CaseView::benchmarkCase)
                .filter(c -> c.caseId.startsWith("stage3-"))
                .toList();
        assertThat(newCases).hasSize(23);
        assertThat(newCases).allSatisfy(c -> {
            assertThat(c.reviewStatus).isEqualTo("MODEL_REVIEWED");
            assertThat(c.humanReviewed).isFalse();
        });
    }
}
