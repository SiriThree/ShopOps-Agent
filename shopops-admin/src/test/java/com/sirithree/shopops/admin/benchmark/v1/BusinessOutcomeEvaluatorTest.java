package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.BusinessOutcomeEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import org.junit.jupiter.api.Test;

class BusinessOutcomeEvaluatorTest {
    @Test
    void unsupportedBusinessScenarioFailsRatherThanFallingBackToReportExists() {
        BenchmarkCase c = TaskEvaluationFixtures.benchmarkCase("daily_review");
        c.scenario = "report_sync";
        var result = new BusinessOutcomeEvaluator().evaluate(c, TaskEvaluationFixtures.evidence("daily_review"));
        assertThat(result.metricValues.get("businessOutcomeCorrect")).isEqualTo(false);
        assertThat(result.failureReasons).contains(FailureReasonCode.REQUIRED_CAPABILITY_MISSING, FailureReasonCode.BUSINESS_OUTCOME_INCORRECT);
    }
}
