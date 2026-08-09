package com.sirithree.shopops.admin.benchmark.v1.dataset.audit;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.TaskCapabilityCatalog;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TaskCapabilityReachabilityAuditTest {
    @Test
    void dedicatedTaskCasesOnlyUseCurrentlyReachableNlFamiliesAndCapabilities() throws Exception {
        Set<String> reachableScenarios = Set.of("daily_review", "comment_risk", "product_optimization", "ad_anomaly");
        var tasks = Stage1DatasetAuditSupport.dedicated().stream()
                .filter(view -> "TASK".equals(view.benchmarkCase().benchmarkType.name()))
                .toList();
        assertThat(tasks).allSatisfy(view -> {
            assertThat(reachableScenarios).contains(view.benchmarkCase().scenario);
            for (String capability : view.benchmarkCase().requiredCapabilities) {
                var definition = TaskCapabilityCatalog.capability(capability);
                assertThat(definition).as(capability).isNotNull();
                assertThat(definition.reachableFromNaturalLanguageAgent()).as(capability).isTrue();
            }
        });
    }
}
