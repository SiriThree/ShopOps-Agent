package com.sirithree.shopops.admin.benchmark.v1.metrics;

import com.sirithree.shopops.admin.benchmark.v1.EvaluationRecord;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;
import java.util.List;

public class IdempotencyMetricsAggregator {
    public IdempotencyMetricSummary aggregate(List<EvaluationRecord> records) {
        IdempotencyMetricSummary summary = new IdempotencyMetricSummary();
        if (records == null) return summary;
        for (EvaluationRecord record : records) {
            if (record.metricBreakdown.actualEffectiveSideEffects == null) continue;
            if (record.executionStatus != CaseExecutionStatus.PASSED && record.executionStatus != CaseExecutionStatus.FAILED) continue;
            summary.executedCases++;
            if (record.executionStatus == CaseExecutionStatus.PASSED) summary.passedCases++; else summary.failedCases++;
            long logical = value(record.metricBreakdown.logicalWriteRequests);
            long delivery = value(record.metricBreakdown.deliveryAttempts);
            long execution = value(record.metricBreakdown.executionAttempts);
            long external = value(record.metricBreakdown.externalAttempts);
            long expected = value(record.metricBreakdown.expectedLogicalSideEffects);
            long actual = value(record.metricBreakdown.actualEffectiveSideEffects);
            long duplicate = value(record.metricBreakdown.duplicateSideEffects);
            long missing = value(record.metricBreakdown.missingSideEffects);
            long intendedReplay = value(record.metricBreakdown.intendedReplayAttempts);
            long boundaryReached = value(record.metricBreakdown.idempotencyBoundaryReachedAttempts);
            long preBlocked = value(record.metricBreakdown.preIdempotencyBlockedAttempts);
            long tool = record.toolAttempts == null ? 0 : record.toolAttempts.size();
            summary.logicalWriteRequests += logical;
            summary.deliveryAttempts += delivery;
            summary.executionAttempts += execution;
            summary.toolAttempts += tool;
            summary.externalAttempts += external;
            summary.expectedEffectiveSideEffects += expected;
            summary.actualEffectiveSideEffects += actual;
            summary.duplicateSideEffects += duplicate;
            summary.missingSideEffects += missing;
            summary.intendedReplayAttempts += intendedReplay;
            summary.idempotencyBoundaryReachedAttempts += boundaryReached;
            summary.preIdempotencyBlockedAttempts += preBlocked;
            if (Boolean.TRUE.equals(record.metricBreakdown.attributionEligible)) summary.attributionEligibleCases++;
            if (Boolean.FALSE.equals(record.metricBreakdown.attributionEligible)) summary.attributionInvalidCases++;
            add(summary.byScenario.computeIfAbsent(record.scenario, ignored -> new IdempotencyMetricSummary.Slice()), record, logical, external, actual, duplicate, missing);
            String fault = string(record.runtimeMetadata.get("faultScenario"), "NONE");
            add(summary.byFault.computeIfAbsent(fault, ignored -> new IdempotencyMetricSummary.Slice()), record, logical, external, actual, duplicate, missing);
            String concurrency = string(record.runtimeMetadata.get("concurrencyMode"), "SEQUENTIAL");
            add(summary.byConcurrency.computeIfAbsent(concurrency, ignored -> new IdempotencyMetricSummary.Slice()), record, logical, external, actual, duplicate, missing);
        }
        return summary;
    }

    private void add(IdempotencyMetricSummary.Slice slice, EvaluationRecord record, long logical, long external,
                     long actual, long duplicate, long missing) {
        slice.cases++;
        if (record.executionStatus == CaseExecutionStatus.PASSED) slice.passed++;
        slice.logicalWrites += logical;
        slice.externalAttempts += external;
        slice.effects += actual;
        slice.duplicates += duplicate;
        slice.missing += missing;
    }

    private long value(Integer value) { return value == null ? 0 : value.longValue(); }
    private String string(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }
}
