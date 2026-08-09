package com.sirithree.shopops.admin.benchmark.v1.dataset.stage3;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GovernancePairedControlTest {
    @Test
    void pairMatrixReferencesRealDistinctRoots() throws Exception {
        Map<String, Object> doc = Stage3GovernanceDatasetSupport.resource("/benchmark/v1/governance/stage3/governance-pair-matrix.json");
        @SuppressWarnings("unchecked") List<Map<String, Object>> pairs = (List<Map<String, Object>>) doc.get("pairs");
        Set<String> roots = Stage3GovernanceDatasetSupport.roots().keySet();
        assertThat(pairs).hasSize(19);
        assertThat(pairs).allSatisfy(pair -> {
            String negative = String.valueOf(pair.get("negativeRoot"));
            String positive = String.valueOf(pair.get("positiveRoot"));
            assertThat(negative).isNotEqualTo(positive);
            assertThat(roots).contains(negative, positive);
            assertThat(String.valueOf(pair.get("negativeDecision"))).isEqualTo("BLOCKED");
            assertThat(String.valueOf(pair.get("positiveDecision"))).isIn("ALLOWED", "REQUIRES_APPROVAL");
        });
    }
}
