package com.sirithree.shopops.admin.benchmark.v1.metrics;

import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;

public class RecoveryMetricsAggregator {
    public RecoveryMetricSummary aggregate(List<EvaluationRecord> records) {
        RecoveryMetricSummary s = new RecoveryMetricSummary();
        int automaticConverged = 0;
        for (EvaluationRecord r : records) {
            if (r == null || r.metricBreakdown == null || r.metricBreakdown.terminalStateReached == null) continue;
            s.faultCases++;
            if (r.executionStatus == CaseExecutionStatus.PASSED || r.executionStatus == CaseExecutionStatus.FAILED || r.executionStatus == CaseExecutionStatus.EXECUTED) s.executedCases++;
            if (Boolean.TRUE.equals(r.metricBreakdown.terminalStateReached)) s.terminalReached++;
            if (Boolean.TRUE.equals(r.metricBreakdown.localStateConsistentWithExternalReality)) s.stateCorrect++;
            if (Boolean.TRUE.equals(r.metricBreakdown.converged)) {
                s.converged++;
                if (value(r.metricBreakdown.manualReviewCount) == 0) automaticConverged++;
            }
            s.permanentStuck += value(r.metricBreakdown.permanentStuckCount);
            s.incorrectTerminalState += value(r.metricBreakdown.incorrectTerminalStateCount);
            s.manualReview += value(r.metricBreakdown.manualReviewCount);
            s.duplicateSideEffects += value(r.metricBreakdown.duplicateSideEffects);
            s.totalRecoveryAttempts += value(r.metricBreakdown.recoveryAttempts);
        }
        if (s.executedCases > 0) {
            s.terminalConvergenceRate = (double) s.terminalReached / s.executedCases;
            s.stateCorrectnessRate = (double) s.stateCorrect / s.executedCases;
            s.permanentStuckRate = (double) s.permanentStuck / s.executedCases;
            s.automaticRecoveryRate = (double) automaticConverged / s.executedCases;
        }
        return s;
    }
    private int value(Integer v) { return v == null ? 0 : v; }
}
