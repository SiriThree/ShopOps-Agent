package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.dataset.DatasetQualityValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatasetNearDuplicateTest {
    @Test void phase2TaskDatasetHasNoExactNormalizedOrTrivialParentDuplicates() throws Exception {
        var cases = new BenchmarkCaseLoader(new ObjectMapper()).loadResources(List.of(
                "/benchmark/v1/dev/cases.json", "/benchmark/v1/validation/cases.json", "/benchmark/v1/test/cases.json"))
                .stream().filter(c -> c.benchmarkType == BenchmarkType.TASK).toList();
        assertThat(new DatasetQualityValidator().findIssues(cases)).isEmpty();
    }
}
