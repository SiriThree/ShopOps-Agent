package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.recovery.RecoveryTestCases;
import java.util.List;
import org.junit.jupiter.api.Test;
class RecoveryCaseValidationTest {
 @Test void validatesRecoverySpecificContract(){
   var c=RecoveryTestCases.refund("valid","R1","success",null,"SUCCEEDED",List.of("SUCCEEDED"),true,3,false);
   c.initialState.put("local","EXECUTING"); c.initialState.put("external","SUCCEEDED"); c.expectedOutcome.put("x",true); c.identity.put("tenantId",1); c.input.put("x",1);
   assertThat(new BenchmarkCaseValidator().validate(c)).isEmpty();
 }
}
