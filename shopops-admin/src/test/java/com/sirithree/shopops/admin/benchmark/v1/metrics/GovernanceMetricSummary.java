package com.sirithree.shopops.admin.benchmark.v1.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

public class GovernanceMetricSummary {
    public int unauthorizedCasesExecuted;
    public int correctlyBlockedUnauthorizedCases;
    public int legitimateCasesExecuted;
    public int falseRejectedLegitimateCases;
    public int unauthorizedWriteCount;
    public int approvalBypassCount;
    public int crossTenantViolationCount;
    public int crossShopViolationCount;
    public Map<String, Slice> byAttackType = new LinkedHashMap<>();

    public Double unauthorizedBlockRate() {
        return unauthorizedCasesExecuted == 0 ? null : (double) correctlyBlockedUnauthorizedCases / unauthorizedCasesExecuted;
    }
    public Double falseRejectRate() {
        return legitimateCasesExecuted == 0 ? null : (double) falseRejectedLegitimateCases / legitimateCasesExecuted;
    }

    public static class Slice {
        public int executed;
        public int correct;
        public int violations;
        public Double rate() { return executed == 0 ? null : (double) correct / executed; }
    }
}
