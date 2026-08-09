package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.metrics.IdempotencyMetricSummary;
import com.sirithree.shopops.admin.benchmark.v1.metrics.IdempotencyMetricsAggregator;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdempotencyReportAggregationTest {
    @Test
    void mustAggregateRawCountsInsteadOfOnlyPercentages() {
        EvaluationRecord first = record("a", CaseExecutionStatus.PASSED, 1, 3, 3, 1, 1, 0, 0);
        EvaluationRecord second = record("b", CaseExecutionStatus.FAILED, 1, 2, 2, 2, 1, 1, 0);
        IdempotencyMetricSummary summary = new IdempotencyMetricsAggregator().aggregate(List.of(first, second));
        assertThat(summary.logicalWriteRequests).isEqualTo(2);
        assertThat(summary.deliveryAttempts).isEqualTo(5);
        assertThat(summary.externalAttempts).isEqualTo(5);
        assertThat(summary.actualEffectiveSideEffects).isEqualTo(3);
        assertThat(summary.duplicateSideEffects).isEqualTo(1);
        assertThat(summary.missingSideEffects).isEqualTo(0);
    }

    private EvaluationRecord record(String id, CaseExecutionStatus status, int logical, int deliveries, int external,
                                    int actual, int expected, int duplicates, int missing) {
        EvaluationRecord r = new EvaluationRecord();
        r.caseId = id;
        r.scenario = "TEST";
        r.executionStatus = status;
        r.metricBreakdown.logicalWriteRequests = logical;
        r.metricBreakdown.deliveryAttempts = deliveries;
        r.metricBreakdown.executionAttempts = deliveries;
        r.metricBreakdown.externalAttempts = external;
        r.metricBreakdown.actualEffectiveSideEffects = actual;
        r.metricBreakdown.expectedLogicalSideEffects = expected;
        r.metricBreakdown.duplicateSideEffects = duplicates;
        r.metricBreakdown.missingSideEffects = missing;
        return r;
    }
}
