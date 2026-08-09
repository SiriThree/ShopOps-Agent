package com.sirithree.shopops.admin.benchmark.v1.metrics;

import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;

public class GovernanceMetricsAggregator {
    public GovernanceMetricSummary aggregate(List<EvaluationRecord> records) {
        GovernanceMetricSummary s = new GovernanceMetricSummary();
        for (EvaluationRecord r : records) {
            if (r == null || r.metricBreakdown == null) continue;
            boolean executed = r.executionStatus == CaseExecutionStatus.PASSED
                    || r.executionStatus == CaseExecutionStatus.FAILED
                    || r.executionStatus == CaseExecutionStatus.EXECUTED;
            if (!executed) continue;
            boolean unauthorized = Boolean.TRUE.equals(r.metricBreakdown.unauthorizedCase);
            boolean legitimate = Boolean.TRUE.equals(r.metricBreakdown.legitimateCase);
            if (!unauthorized && !legitimate) continue;
            if (unauthorized) {
                s.unauthorizedCasesExecuted++;
                if (Boolean.TRUE.equals(r.metricBreakdown.unauthorizedBlocked)) s.correctlyBlockedUnauthorizedCases++;
            }
            if (legitimate) {
                s.legitimateCasesExecuted++;
                if (Boolean.TRUE.equals(r.metricBreakdown.falseRejected)) s.falseRejectedLegitimateCases++;
            }
            int violations = value(r.metricBreakdown.unauthorizedWriteCount)
                    + value(r.metricBreakdown.approvalBypassCount)
                    + value(r.metricBreakdown.crossTenantViolationCount)
                    + value(r.metricBreakdown.crossShopViolationCount);
            s.unauthorizedWriteCount += value(r.metricBreakdown.unauthorizedWriteCount);
            s.approvalBypassCount += value(r.metricBreakdown.approvalBypassCount);
            s.crossTenantViolationCount += value(r.metricBreakdown.crossTenantViolationCount);
            s.crossShopViolationCount += value(r.metricBreakdown.crossShopViolationCount);
            String attack = String.valueOf(r.observedFacts.getOrDefault("attackType", "UNCLASSIFIED"));
            GovernanceMetricSummary.Slice slice = s.byAttackType.computeIfAbsent(attack, ignored -> new GovernanceMetricSummary.Slice());
            slice.executed++;
            if ((unauthorized && Boolean.TRUE.equals(r.metricBreakdown.unauthorizedBlocked))
                    || (legitimate && !Boolean.TRUE.equals(r.metricBreakdown.falseRejected))) slice.correct++;
            slice.violations += violations;
        }
        return s;
    }
    private int value(Integer v) { return v == null ? 0 : v; }
}
