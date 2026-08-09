package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.recovery.RecoveryTestCases;
import com.sirithree.shopops.admin.benchmark.v1.recovery.StateConvergenceEvaluator;
import java.util.List;
import org.junit.jupiter.api.Test;
class RecoveryFailureReasonTest {
 @Test void exposesStateNotConvergedInsteadOfGenericFail(){
   var c=RecoveryTestCases.refund("stuck","X","success",null,"SUCCEEDED",List.of("SUCCEEDED"),true,3,false);
   var e=new CollectedEvidence(); e.businessFacts.put("externalReality","SUCCEEDED"); e.businessFacts.put("localState","EXECUTING"); e.businessFacts.put("effectiveSideEffects",1);
   var r=new StateConvergenceEvaluator().evaluate(c,e); assertThat(r.failureReasons).contains(FailureReasonCode.STATE_NOT_CONVERGED);
 }
}
