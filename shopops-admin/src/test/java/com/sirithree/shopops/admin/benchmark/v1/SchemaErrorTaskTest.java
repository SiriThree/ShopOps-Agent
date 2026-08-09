package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.ToolLegalityEvaluator;
import org.junit.jupiter.api.Test;

class SchemaErrorTaskTest {
    @Test void invalidToolArgumentsAreAExecutionFailureNotBusinessEmptyResult() {
        BenchmarkCase c = TaskEvaluationFixtures.benchmarkCase("ad_anomaly");
        var evidence = TaskEvaluationFixtures.evidence("ad_anomaly");
        evidence.toolLogs.removeIf(log -> "ad.query_performance".equals(log.get("toolCode")));
        evidence.toolLogs.add(TaskEvaluationFixtures.toolLog("ad.query_performance", null, "FAILED", "MCP_INPUT_INVALID"));
        var result = new ToolLegalityEvaluator().evaluate(c, evidence);
        assertThat(result.metricValues.get("toolExecutionValid")).isEqualTo(false);
        assertThat(result.failureReasons).contains(FailureReasonCode.INVALID_TOOL_ARGUMENT);
    }
}
