package com.sirithree.shopops.admin.benchmark.v1.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

public class IdempotencyMetricSummary {
    public int executedCases;
    public int passedCases;
    public int failedCases;
    public long logicalWriteRequests;
    public long deliveryAttempts;
    public long executionAttempts;
    public long toolAttempts;
    public long externalAttempts;
    public long expectedEffectiveSideEffects;
    public long actualEffectiveSideEffects;
    public long duplicateSideEffects;
    public long missingSideEffects;
    public long intendedReplayAttempts;
    public long idempotencyBoundaryReachedAttempts;
    public long preIdempotencyBlockedAttempts;
    public int attributionEligibleCases;
    public int attributionInvalidCases;
    public Map<String, Slice> byScenario = new LinkedHashMap<>();
    public Map<String, Slice> byFault = new LinkedHashMap<>();
    public Map<String, Slice> byConcurrency = new LinkedHashMap<>();

    public Double duplicateSideEffectRate() {
        if (expectedEffectiveSideEffects == 0) return duplicateSideEffects == 0 ? 0.0 : 1.0;
        return duplicateSideEffects / (double) expectedEffectiveSideEffects;
    }

    public static class Slice {
        public int cases;
        public int passed;
        public long logicalWrites;
        public long externalAttempts;
        public long effects;
        public long duplicates;
        public long missing;
    }
}
