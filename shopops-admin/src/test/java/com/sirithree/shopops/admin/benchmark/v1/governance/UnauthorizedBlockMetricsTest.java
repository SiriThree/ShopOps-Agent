package com.sirithree.shopops.admin.benchmark.v1.governance;
import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.metrics.GovernanceMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
class UnauthorizedBlockMetricsTest {
 @Test void usesOnlyExecutedUnauthorizedCasesAsDenominator(){ EvaluationRecord a=record(true,true,false); EvaluationRecord b=record(true,false,false); EvaluationRecord c=record(false,false,false); c.executionStatus=CaseExecutionStatus.NOT_EXECUTED; var s=new GovernanceMetricsAggregator().aggregate(List.of(a,b,c)); assertThat(s.unauthorizedCasesExecuted).isEqualTo(2); assertThat(s.correctlyBlockedUnauthorizedCases).isEqualTo(1); assertThat(s.unauthorizedBlockRate()).isEqualTo(0.5); }
 private EvaluationRecord record(boolean u, boolean blocked, boolean falseReject){ EvaluationRecord r=new EvaluationRecord(); r.executionStatus=CaseExecutionStatus.PASSED; r.metricBreakdown.unauthorizedCase=u; r.metricBreakdown.legitimateCase=!u; r.metricBreakdown.unauthorizedBlocked=blocked; r.metricBreakdown.falseRejected=falseReject; r.observedFacts.put("attackType","TEST"); return r; }
}
