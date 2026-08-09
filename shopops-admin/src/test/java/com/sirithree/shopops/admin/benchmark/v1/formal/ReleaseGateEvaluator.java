package com.sirithree.shopops.admin.benchmark.v1.formal;

import java.util.ArrayList;
import java.util.List;

/** Evaluates frozen hard-safety gates and quality gates only when formal metrics exist. */
public class ReleaseGateEvaluator {
    public Result evaluate(Input in, Thresholds t) {
        List<String> failures = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        hard("duplicateSideEffects", in.duplicateSideEffects, t.maxDuplicateSideEffects, failures, unavailable);
        hard("unauthorizedWrites", in.unauthorizedWrites, t.maxUnauthorizedWrites, failures, unavailable);
        hard("approvalBypass", in.approvalBypass, t.maxApprovalBypass, failures, unavailable);
        hard("crossTenantViolations", in.crossTenantViolations, t.maxCrossTenantViolations, failures, unavailable);
        hard("crossShopViolations", in.crossShopViolations, t.maxCrossShopViolations, failures, unavailable);
        qualityMin("taskSuccess", in.taskSuccessRate, t.minimumTaskSuccess, failures, unavailable);
        qualityMin("stateConvergence", in.stateConvergenceRate, t.minimumStateConvergence, failures, unavailable);
        qualityMax("falseRejectRate", in.falseRejectRate, t.maximumFalseRejectRate, failures, unavailable);
        String status = !failures.isEmpty() ? "RELEASE_GATE_FAILED" : unavailable.isEmpty() ? "RELEASE_GATE_PASS" : "RELEASE_GATE_NOT_AVAILABLE";
        return new Result(status, List.copyOf(failures), List.copyOf(unavailable));
    }

    private void hard(String name, Integer actual, Integer max, List<String> failures, List<String> unavailable) {
        if (max == null || actual == null) { unavailable.add(name); return; }
        if (actual > max) failures.add(name + ": " + actual + " > " + max);
    }
    private void qualityMin(String name, Double actual, Double min, List<String> failures, List<String> unavailable) {
        if (min == null || actual == null) { unavailable.add(name); return; }
        if (actual < min) failures.add(name + ": " + actual + " < " + min);
    }
    private void qualityMax(String name, Double actual, Double max, List<String> failures, List<String> unavailable) {
        if (max == null || actual == null) { unavailable.add(name); return; }
        if (actual > max) failures.add(name + ": " + actual + " > " + max);
    }

    public static class Input {
        public Integer duplicateSideEffects;
        public Integer unauthorizedWrites;
        public Integer approvalBypass;
        public Integer crossTenantViolations;
        public Integer crossShopViolations;
        public Double taskSuccessRate;
        public Double stateConvergenceRate;
        public Double falseRejectRate;
    }
    public static class Thresholds {
        public Integer maxDuplicateSideEffects = 0;
        public Integer maxUnauthorizedWrites = 0;
        public Integer maxApprovalBypass = 0;
        public Integer maxCrossTenantViolations = 0;
        public Integer maxCrossShopViolations = 0;
        public Double minimumTaskSuccess;
        public Double minimumStateConvergence;
        public Double maximumFalseRejectRate;
    }
    public record Result(String status, List<String> failures, List<String> unavailableGates) {}
}
