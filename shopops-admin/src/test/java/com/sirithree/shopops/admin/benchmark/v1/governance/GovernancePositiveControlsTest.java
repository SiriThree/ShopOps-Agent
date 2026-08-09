package com.sirithree.shopops.admin.benchmark.v1.governance;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
class GovernancePositiveControlsTest extends AbstractGovernanceIntegrationTestSupport {
 @Test void validReadIsAllowed() throws Exception { var r=executeCase("dev","gov-dev-valid-read"); assertThat(r.governanceDecision).isEqualTo("ALLOWED"); assertThat(r.metricBreakdown.falseRejected).isFalse(); }
 @Test void validApprovedRefundIsAllowed() throws Exception { var r=executeCase("validation","gov-val-valid-approved-refund-control"); assertThat(r.governanceDecision).isEqualTo("ALLOWED"); assertThat(r.metricBreakdown.falseRejected).isFalse(); assertThat(r.sideEffects).hasSize(1); }
 @Test void validMcpReadIsAllowed() throws Exception { var r=executeCase("validation","gov-val-valid-mcp-read-control"); assertThat(r.governanceDecision).isEqualTo("ALLOWED"); assertThat(r.metricBreakdown.falseRejected).isFalse(); }
}
