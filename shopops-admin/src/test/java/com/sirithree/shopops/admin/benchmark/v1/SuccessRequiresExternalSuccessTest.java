package com.sirithree.shopops.admin.benchmark.v1;
import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.recovery.RecoveryTestCases;
import com.sirithree.shopops.admin.benchmark.v1.recovery.StateConvergenceEvaluator;
import java.util.List;
import org.junit.jupiter.api.Test;
class SuccessRequiresExternalSuccessTest { @Test void localSuccessCannotOverrideNotAcceptedExternalTruth(){ var c=RecoveryTestCases.refund("inv","X","success",null,"NOT_ACCEPTED",List.of("FAILED"),true,3,false); var e=new CollectedEvidence(); e.businessFacts.put("externalReality","NOT_ACCEPTED"); e.businessFacts.put("localState","SUCCEEDED"); e.businessFacts.put("effectiveSideEffects",0); var r=new StateConvergenceEvaluator().evaluate(c,e); assertThat(r.passed).isFalse(); } }
