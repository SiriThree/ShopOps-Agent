package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.metrics.RecoveryMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
class RecoveryMetricsAggregatorTest {
 @Test void keepsRawRecoveryCounts(){
   EvaluationRecord r=new EvaluationRecord(); r.executionStatus=CaseExecutionStatus.PASSED; r.metricBreakdown.terminalStateReached=true; r.metricBreakdown.localStateConsistentWithExternalReality=true; r.metricBreakdown.converged=true; r.metricBreakdown.recoveryAttempts=2; r.metricBreakdown.manualReviewCount=0; r.metricBreakdown.permanentStuckCount=0; r.metricBreakdown.incorrectTerminalStateCount=0; r.metricBreakdown.duplicateSideEffects=0;
   var s=new RecoveryMetricsAggregator().aggregate(List.of(r)); assertThat(s.terminalReached).isEqualTo(1); assertThat(s.totalRecoveryAttempts).isEqualTo(2); assertThat(s.terminalConvergenceRate).isEqualTo(1.0); assertThat(s.automaticRecoveryRate).isEqualTo(1.0);
 }

 @Test void manualTerminalDoesNotProduceNegativeAutomaticRecoveryRate(){
   EvaluationRecord r=new EvaluationRecord(); r.executionStatus=CaseExecutionStatus.PASSED; r.metricBreakdown.terminalStateReached=true; r.metricBreakdown.localStateConsistentWithExternalReality=false; r.metricBreakdown.converged=false; r.metricBreakdown.recoveryAttempts=3; r.metricBreakdown.manualReviewCount=1; r.metricBreakdown.permanentStuckCount=0; r.metricBreakdown.incorrectTerminalStateCount=0; r.metricBreakdown.duplicateSideEffects=0;
   var s=new RecoveryMetricsAggregator().aggregate(List.of(r)); assertThat(s.manualReview).isEqualTo(1); assertThat(s.automaticRecoveryRate).isEqualTo(0.0);
 }
}
