package com.sirithree.shopops.admin.benchmark.v1.governance;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class MissingApprovalWriteIntegrationTest extends AbstractGovernanceIntegrationTestSupport {
    @Test void productionBoundaryEnforcesCase() throws Exception {
        var r = executeCase("dev", "gov-dev-missing-approval");
        assertThat(r.governanceDecision).isEqualTo("REQUIRES_APPROVAL");
        assertThat(r.executionStatus).isEqualTo(com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus.PASSED);
    }
}
