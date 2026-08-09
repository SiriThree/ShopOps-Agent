package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome.CommentHandlingOutcomeEvaluator;
import org.junit.jupiter.api.Test;

class MissingParameterTaskTest {
    @Test void consistentSafeDefaultDateIsAcceptedWithoutRequiringClarification() {
        BenchmarkCase c = TaskEvaluationFixtures.benchmarkCase("comment_risk");
        c.input.remove("dateRange");
        c.expectedOutcome.put("parameterResolution", "SAFE_DEFAULT");
        var result = new CommentHandlingOutcomeEvaluator().evaluate(c, TaskEvaluationFixtures.evidence("comment_risk"));
        assertThat(result.metricValues.get("businessOutcomeCorrect")).isEqualTo(true);
    }
}
