package com.sirithree.shopops.admin.benchmark.v1.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaskMetricSummary {
    public int executedCases;
    public int successCases;
    public int failedCases;
    public int notExecutedCases;
    public int infrastructureErrors;
    public int incorrectSuccessCount;
    public int plannerFallbackCount;
    public int modelPlanAcceptedCount;
    public int ruleBasedCount;
    public int modelFallbackCount;
    public int successWithoutFallback;
    public int successWithFallback;
    public Map<String, Slice> byScenario = new LinkedHashMap<>();
    public Map<String, Slice> byTag = new LinkedHashMap<>();

    public Double taskSuccessRate() {
        return executedCases == 0 ? null : ((double) successCases) / executedCases;
    }

    public Double incorrectSuccessRate() {
        return executedCases == 0 ? null : ((double) incorrectSuccessCount) / executedCases;
    }

    public Double plannerFallbackRate() {
        int modelPlannerObserved = modelPlanAcceptedCount + modelFallbackCount;
        return modelPlannerObserved == 0 ? null : ((double) modelFallbackCount) / modelPlannerObserved;
    }

    public static class Slice {
        public int executed;
        public int success;
        public int failed;

        public Double rate() {
            return executed == 0 ? null : ((double) success) / executed;
        }
    }
}
