package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatasetProvenanceTest {
    @Test void phase2TaskCasesCarryReviewAndOriginMetadata() throws Exception {
        var cases = new BenchmarkCaseLoader(new ObjectMapper()).loadResources(List.of(
                "/benchmark/v1/dev/cases.json", "/benchmark/v1/validation/cases.json", "/benchmark/v1/test/cases.json"));
        var taskCases = cases.stream().filter(c -> c.benchmarkType == BenchmarkType.TASK).toList();
        assertThat(taskCases).isNotEmpty();
        assertThat(taskCases).allSatisfy(c -> {
            assertThat(c.semanticTaskId).isNotBlank();
            assertThat(c.origin).isIn("LEGACY", "HAND_AUTHORED", "PERTURBED", "PUBLIC_DATA_DERIVED");
            assertThat(c.humanReviewed).isTrue();
            if ("PERTURBED".equals(c.origin)) {
                assertThat(c.parentCaseId).isNotBlank();
                assertThat(c.perturbationType).isNotBlank();
            }
        });
    }
}
