package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DatasetReviewStatusValidationTest {
    @Test
    void codingAgentAuditIsModelReviewedAndDoesNotInventHumanReviewEvidence() throws Exception {
        var cases = Stage1DatasetAuditSupport.load();
        assertThat(cases).allSatisfy(view -> {
            assertThat(view.metadata().reviewStatus()).isEqualTo("MODEL_REVIEWED");
            assertThat(view.metadata().humanReviewEvidencePresent()).isFalse();
        });
    }
}
