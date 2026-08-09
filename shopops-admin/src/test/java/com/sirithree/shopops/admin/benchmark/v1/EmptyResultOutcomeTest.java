package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.AdAnalysisOutcomeEvaluator;
import org.junit.jupiter.api.Test;

class EmptyResultOutcomeTest {
    @Test void legitimateEmptyAdSourceIsNoDataNotZeroPerformanceRisk() {
        BenchmarkCase c = TaskEvaluationFixtures.benchmarkCase("ad_anomaly");
        c.expectedOutcome.put("resultClass", "NO_DATA");
        var result = new AdAnalysisOutcomeEvaluator().evaluate(c, TaskEvaluationFixtures.adNoDataEvidence());
        assertThat(result.metricValues.get("businessOutcomeCorrect")).isEqualTo(true);
        @SuppressWarnings("unchecked") var details = (java.util.Map<String,Object>) result.metricValues.get("businessOutcomeDetails");
        assertThat(details).containsEntry("actualResultClass", "NO_DATA");
    }
}
