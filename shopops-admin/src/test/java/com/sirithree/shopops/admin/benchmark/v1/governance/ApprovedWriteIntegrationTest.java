package com.sirithree.shopops.admin.benchmark.v1.governance;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ApprovedWriteIntegrationTest extends AbstractGovernanceIntegrationTestSupport {
    @Test void productionBoundaryEnforcesCase() throws Exception {
        var r = executeCase("validation", "gov-val-valid-approved-refund-control");
        assertThat(r.governanceDecision).isEqualTo("ALLOWED");
        assertThat(r.executionStatus).isEqualTo(com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus.PASSED);
    }
}
