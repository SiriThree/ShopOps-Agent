package com.sirithree.shopops.admin.benchmark.v1.runtime;

import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import java.util.LinkedHashMap;
import java.util.Map;

public class BenchmarkRuntimeResult {
    public CaseExecutionStatus executionStatus = CaseExecutionStatus.ERROR;
    public Long taskId;
    public String traceId;
    public String finalState;
    public Map<String, Object> observedInterpretation = new LinkedHashMap<>();
    public Map<String, Object> endpointPlanPreview = new LinkedHashMap<>();
    public String infrastructureError;
    public FailureReasonCode runtimeFailureReason;
}
