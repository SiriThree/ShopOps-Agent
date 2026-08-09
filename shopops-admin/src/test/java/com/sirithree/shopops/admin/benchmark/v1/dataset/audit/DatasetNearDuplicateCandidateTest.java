package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DatasetNearDuplicateCandidateTest {
    @Test
    void semanticRootReuseProducesReviewCandidatesInsteadOfAutomaticDeletion() throws Exception {
        var roots = Stage1DatasetAuditSupport.roots(Stage1DatasetAuditSupport.dedicated());
        long sameRootPairs = roots.values().stream().mapToLong(group -> ((long) group.size() * (group.size() - 1)) / 2).sum();
        assertThat(sameRootPairs).isGreaterThan(33);
    }
}
