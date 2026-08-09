package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class DatasetSemanticRootValidationTest {
    @Test
    void everyCurrentCaseHasNonBlankSemanticRootMetadata() throws Exception {
        var cases = Stage1DatasetAuditSupport.load();
        assertThat(cases).hasSize(161);
        assertThat(cases).allSatisfy(view -> assertThat(view.metadata().semanticRootId()).isNotBlank());
        assertThat(new HashSet<>(cases.stream().map(view -> view.benchmarkCase().caseId).toList())).hasSize(161);
    }
}
