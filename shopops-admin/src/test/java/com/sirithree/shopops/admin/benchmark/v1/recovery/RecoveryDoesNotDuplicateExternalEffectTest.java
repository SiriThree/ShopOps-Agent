package com.sirithree.shopops.admin.benchmark.v1.recovery;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
class RecoveryDoesNotDuplicateExternalEffectTest extends AbstractRefundRecoveryIntegrationTestSupport { @Test void r1RecoveryQueriesInsteadOfReexecuting(){ var r=execute(RecoveryTestCases.refund("nodup","EXTERNAL_SUCCESS_LOCAL_FAILURE","success","AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM","SUCCEEDED",List.of("SUCCEEDED"),true,3,false)); assertThat(r.metricBreakdown.actualEffectiveSideEffects).isEqualTo(1); assertThat(r.metricBreakdown.duplicateSideEffects).isZero(); } }
