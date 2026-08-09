package com.sirithree.shopops.admin.benchmark.v1.governance;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class InvalidSchemaIntegrationTest extends AbstractGovernanceIntegrationTestSupport {
    @Test void productionBoundaryEnforcesCase() throws Exception {
        var r = executeCase("validation", "gov-val-schema-missing-field");
        assertThat(r.governanceDecision).isEqualTo("BLOCKED");
        assertThat(r.executionStatus).isEqualTo(com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus.PASSED);
    }
}
