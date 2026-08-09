package com.sirithree.shopops.admin.benchmark.v1.metrics;

import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;
import java.util.Map;

/** Task-only metrics. NOT_EXECUTED/ERROR cases are never placed in Task Success denominator. */
public class TaskMetricsAggregator {
    public TaskMetricSummary aggregate(List<EvaluationRecord> records) {
        TaskMetricSummary summary = new TaskMetricSummary();
        if (records == null) return summary;
        for (EvaluationRecord record : records) {
            boolean executed = record.executionStatus == CaseExecutionStatus.PASSED || record.executionStatus == CaseExecutionStatus.FAILED;
            if (!executed) {
                summary.notExecutedCases++;
                if (record.executionStatus == CaseExecutionStatus.ERROR
                        && record.failureReasons.contains("INFRASTRUCTURE_ERROR")) {
                    summary.infrastructureErrors++;
                }
                continue;
            }
            summary.executedCases++;
            boolean success = Boolean.TRUE.equals(record.metricBreakdown.taskSuccess);
            if (success) summary.successCases++; else summary.failedCases++;
            if (Boolean.TRUE.equals(record.metricBreakdown.incorrectSuccess)) summary.incorrectSuccessCount++;

            String plannerMode = plannerMode(record);
            boolean fallback = "MODEL_FALLBACK".equals(plannerMode) || Boolean.TRUE.equals(record.metricBreakdown.plannerFallback);
            if ("MODEL".equals(plannerMode)) summary.modelPlanAcceptedCount++;
            else if ("MODEL_FALLBACK".equals(plannerMode)) summary.modelFallbackCount++;
            else if ("RULE_BASED".equals(plannerMode)) summary.ruleBasedCount++;
            if (fallback) summary.plannerFallbackCount++;
            if (success && fallback) summary.successWithFallback++;
            if (success && !fallback) summary.successWithoutFallback++;

            add(summary.byScenario, record.scenario == null ? "UNAVAILABLE" : record.scenario, success);
            if (record.tags != null) for (String tag : record.tags) add(summary.byTag, tag, success);
        }
        return summary;
    }

    private void add(Map<String, TaskMetricSummary.Slice> target, String key, boolean success) {
        TaskMetricSummary.Slice slice = target.computeIfAbsent(key, ignored -> new TaskMetricSummary.Slice());
        slice.executed++;
        if (success) slice.success++; else slice.failed++;
    }

    @SuppressWarnings("unchecked")
    private String plannerMode(EvaluationRecord record) {
        Object value = record.observedPlan.get("actualPlannerObservation");
        if (value instanceof Map<?, ?> map) {
            Object mode = map.get("plannerMode");
            return mode == null ? null : String.valueOf(mode);
        }
        return null;
    }
}
