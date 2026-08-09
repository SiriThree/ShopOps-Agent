package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.OrderReviewOutcomeEvaluator;
import org.junit.jupiter.api.Test;

class OrderReviewOutcomeEvaluatorTest {
    @Test void consistentSourceMetricsPass() {
        var result = new OrderReviewOutcomeEvaluator().evaluate(TaskEvaluationFixtures.benchmarkCase("daily_review"), TaskEvaluationFixtures.evidence("daily_review"));
        assertThat(result.metricValues.get("businessOutcomeCorrect")).isEqualTo(true);
    }
    @Test void reportMetricMismatchFailsEvenWhenTaskStatusIsSuccess() {
        var evidence = TaskEvaluationFixtures.evidence("daily_review");
        @SuppressWarnings("unchecked") var ev = (java.util.Map<String,Object>) evidence.report.getEvidence();
        @SuppressWarnings("unchecked") var sources = (java.util.Map<String,Object>) ev.get("dataSources");
        @SuppressWarnings("unchecked") var order = (java.util.Map<String,Object>) sources.get("orderSummary");
        @SuppressWarnings("unchecked") var metrics = (java.util.Map<String,Object>) order.get("metrics");
        metrics.put("gmv", -1);
        var result = new OrderReviewOutcomeEvaluator().evaluate(TaskEvaluationFixtures.benchmarkCase("daily_review"), evidence);
        assertThat(result.metricValues.get("businessOutcomeCorrect")).isEqualTo(false);
    }
}
