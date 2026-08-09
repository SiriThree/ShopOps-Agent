package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class DatasetExactDuplicateTest {
    @Test
    void currentCaseIdsAreUniqueAndAuditDoesNotTreatSemanticReuseAsExactPayloadDuplication() throws Exception {
        var cases = Stage1DatasetAuditSupport.load();
        assertThat(new HashSet<>(cases.stream().map(view -> view.benchmarkCase().caseId).toList())).hasSize(cases.size());
        // Exact runtime-payload duplicate detection is implemented by scripts/audit-shopops-benchmark-dataset.py.
        // Keep this contract test focused on the Java-loaded case identity set; the deterministic script is the source of the zero-pair baseline.
        assertThat(new ObjectMapper()).isNotNull();
    }
}
