package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DatasetCoverageAggregationTest {
    @Test
    void machineBaselineSeparatesRawCasesFromIndependentSemanticRoots() throws Exception {
        var dedicated = Stage1DatasetAuditSupport.dedicated();
        assertThat(dedicated).hasSize(154);
        assertThat(Stage1DatasetAuditSupport.roots(dedicated)).hasSize(91);
        Map<String, Long> counts = dedicated.stream().collect(Collectors.groupingBy(
                view -> view.benchmarkCase().benchmarkType.name(), Collectors.counting()));
        assertThat(counts).containsEntry("TASK", 93L)
                .containsEntry("IDEMPOTENCY", 15L)
                .containsEntry("RECOVERY", 13L)
                .containsEntry("GOVERNANCE", 33L);
    }
}
