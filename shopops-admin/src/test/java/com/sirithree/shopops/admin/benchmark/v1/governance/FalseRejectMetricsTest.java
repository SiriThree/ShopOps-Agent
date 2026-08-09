package com.sirithree.shopops.admin.benchmark.v1.governance;
import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.metrics.GovernanceMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
class FalseRejectMetricsTest {
 @Test void legitimateControlsHaveIndependentFalseRejectDenominator(){ EvaluationRecord ok=r(false); EvaluationRecord bad=r(true); var s=new GovernanceMetricsAggregator().aggregate(List.of(ok,bad)); assertThat(s.legitimateCasesExecuted).isEqualTo(2); assertThat(s.falseRejectedLegitimateCases).isEqualTo(1); assertThat(s.falseRejectRate()).isEqualTo(0.5); }
 private EvaluationRecord r(boolean rejected){ EvaluationRecord r=new EvaluationRecord(); r.executionStatus=CaseExecutionStatus.PASSED; r.metricBreakdown.legitimateCase=true; r.metricBreakdown.unauthorizedCase=false; r.metricBreakdown.falseRejected=rejected; r.observedFacts.put("attackType","LEGITIMATE"); return r; }
}
