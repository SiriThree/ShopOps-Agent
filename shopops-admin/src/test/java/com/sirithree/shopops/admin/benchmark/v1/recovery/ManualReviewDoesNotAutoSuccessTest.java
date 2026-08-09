package com.sirithree.shopops.admin.benchmark.v1.recovery;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
class ManualReviewDoesNotAutoSuccessTest extends AbstractRefundRecoveryIntegrationTestSupport { @Test void budgetExhaustionDoesNotSilentlyBecomeSuccess(){ var c=RecoveryTestCases.refund("manual","RECOVERY_BUDGET_EXHAUSTED","success","AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM","SUCCEEDED",List.of("NEEDS_MANUAL_ACTION"),false,3,true); var r=execute(c); assertThat(r.finalState).isEqualTo("NEEDS_MANUAL_ACTION"); assertThat(r.metricBreakdown.converged).isFalse(); } }
