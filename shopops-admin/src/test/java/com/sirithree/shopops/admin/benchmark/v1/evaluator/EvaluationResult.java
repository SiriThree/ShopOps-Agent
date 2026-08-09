package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import com.sirithree.shopops.admin.benchmark.v1.evidence.EvidenceRef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvaluationResult {
    public boolean passed = true;
    public Map<String, Object> metricValues = new LinkedHashMap<>();
    public List<FailureReasonCode> failureReasons = new ArrayList<>();
    public List<EvidenceRef> evidenceRefs = new ArrayList<>();

    public EvaluationResult fail(FailureReasonCode reason) {
        this.passed = false;
        if (!failureReasons.contains(reason)) failureReasons.add(reason);
        return this;
    }

    public EvaluationResult metric(String name, Object value) {
        metricValues.put(name, value);
        return this;
    }

    public EvaluationResult merge(EvaluationResult other) {
        if (other == null) return this;
        this.passed = this.passed && other.passed;
        this.metricValues.putAll(other.metricValues);
        for (FailureReasonCode reason : other.failureReasons) {
            if (!this.failureReasons.contains(reason)) this.failureReasons.add(reason);
        }
        for (EvidenceRef ref : other.evidenceRefs) {
            if (!this.evidenceRefs.contains(ref)) this.evidenceRefs.add(ref);
        }
        return this;
    }
}
