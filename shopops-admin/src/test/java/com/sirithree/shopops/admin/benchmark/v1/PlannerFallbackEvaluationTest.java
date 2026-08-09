package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.metrics.TaskMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlannerFallbackEvaluationTest {
    @Test void modelFallbackIsReportedSeparatelyFromFinalTaskSuccess() {
        EvaluationRecord record = new EvaluationRecord();
        record.caseId = "fallback";
        record.scenario = "daily_review";
        record.executionStatus = CaseExecutionStatus.PASSED;
        record.metricBreakdown.taskSuccess = true;
        record.observedPlan.put("actualPlannerObservation", Map.of("plannerMode", "MODEL_FALLBACK", "fallback", true));
        var summary = new TaskMetricsAggregator().aggregate(java.util.List.of(record));
        assertThat(summary.successCases).isEqualTo(1);
        assertThat(summary.modelFallbackCount).isEqualTo(1);
        assertThat(summary.successWithFallback).isEqualTo(1);
        assertThat(summary.plannerFallbackRate()).isEqualTo(1.0);
    }

    @Test void ruleBasedRunsDoNotDiluteModelFallbackRate() {
        EvaluationRecord fallback = new EvaluationRecord();
        fallback.executionStatus = CaseExecutionStatus.PASSED;
        fallback.metricBreakdown.taskSuccess = true;
        fallback.observedPlan.put("actualPlannerObservation", Map.of("plannerMode", "MODEL_FALLBACK", "fallback", true));
        EvaluationRecord rule = new EvaluationRecord();
        rule.executionStatus = CaseExecutionStatus.PASSED;
        rule.metricBreakdown.taskSuccess = true;
        rule.observedPlan.put("actualPlannerObservation", Map.of("plannerMode", "RULE_BASED", "fallback", false));
        var summary = new TaskMetricsAggregator().aggregate(java.util.List.of(fallback, rule));
        assertThat(summary.ruleBasedCount).isEqualTo(1);
        assertThat(summary.plannerFallbackRate()).isEqualTo(1.0);
    }
}
