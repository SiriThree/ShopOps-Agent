package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.CompositeTaskBenchmarkEvaluator;
import org.junit.jupiter.api.Test;

class MultiToolTaskTest {
    @Test void dailyReviewRequiresBusinessCapabilitiesWithoutExactSequenceEquality() {
        BenchmarkCase c = TaskEvaluationFixtures.benchmarkCase("daily_review");
        var result = new CompositeTaskBenchmarkEvaluator().evaluate(c, TaskEvaluationFixtures.evidence("daily_review"));
        assertThat(result.metricValues.get("taskSuccess")).isEqualTo(true);
        assertThat(result.metricValues.get("executedToolCodes")).isNotNull();
    }
}
