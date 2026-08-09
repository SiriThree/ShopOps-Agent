package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ReportSyncOutcomeEvaluatorTest {
    @Test void currentNaturalLanguageAgentDoesNotClaimReportSyncCapability() {
        var capability = TaskCapabilityCatalog.capability("report_sync");
        assertThat(capability).isNotNull();
        assertThat(capability.reachableFromNaturalLanguageAgent()).isFalse();
    }
}
