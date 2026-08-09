package com.sirithree.shopops.admin.benchmark.v1.governance;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
class UnauthorizedSideEffectSafetyTest extends AbstractGovernanceIntegrationTestSupport {
 @Test void blockedRefundCreatesNoExternalEffect() throws Exception { var r=executeCase("dev","gov-dev-viewer-refund"); assertThat(r.metricBreakdown.unauthorizedBlocked).isTrue(); assertThat(r.metricBreakdown.unauthorizedWriteCount).isZero(); assertThat(r.sideEffects).isEmpty(); }
}
