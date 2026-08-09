package com.sirithree.shopops.admin.benchmark.v1.dataset.stage2;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TaskReviewStatusTest {
    @Test
    void newCasesAreModelReviewedAndNeverInventHumanReview() throws Exception {
        var newCases = Stage2TaskDatasetSupport.taskCases().stream()
                .map(Stage2TaskDatasetSupport.CaseView::benchmarkCase)
                .filter(c -> c.caseId.startsWith("stage2-"))
                .toList();
        assertThat(newCases).hasSize(72);
        assertThat(newCases).allSatisfy(c -> {
            assertThat(c.reviewStatus).isEqualTo("MODEL_REVIEWED");
            assertThat(c.humanReviewed).isFalse();
        });
    }
}
