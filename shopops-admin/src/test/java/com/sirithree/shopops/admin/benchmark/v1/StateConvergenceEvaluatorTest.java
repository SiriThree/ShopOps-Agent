package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.recovery.RecoveryTestCases;
import com.sirithree.shopops.admin.benchmark.v1.recovery.StateConvergenceEvaluator;
import java.util.List;
import org.junit.jupiter.api.Test;

class StateConvergenceEvaluatorTest {
    @Test void requiresTerminalAndExternalConsistencyAndNoDuplicateEffect() {
        BenchmarkCase c=RecoveryTestCases.refund("eval","EXTERNAL_SUCCESS_LOCAL_FAILURE","success",null,"SUCCEEDED", List.of("SUCCEEDED"),true,3,false);
        CollectedEvidence e=new CollectedEvidence(); e.businessFacts.put("externalReality","SUCCEEDED"); e.businessFacts.put("localState","SUCCEEDED"); e.businessFacts.put("effectiveSideEffects",1); e.businessFacts.put("recoveryAttempts",1);
        var r=new StateConvergenceEvaluator().evaluate(c,e);
        assertThat(r.metricValues.get("converged")).isEqualTo(true); assertThat(r.passed).isTrue();
    }
}
