package com.sirithree.shopops.admin.benchmark.v1.runtime;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Explicit Gold-leakage boundary. Only runtime-visible fields are copied from BenchmarkCase.
 * No expectedOutcome/capability/tool/side-effect/approval/gold fields exist on this type.
 */
public class BenchmarkRuntimeRequest {
    public String caseId;
    public String scenario;
    public Map<String, Object> input = new LinkedHashMap<>();
    public Map<String, Object> identity = new LinkedHashMap<>();
    public Map<String, Object> initialState = new LinkedHashMap<>();

    public static BenchmarkRuntimeRequest from(BenchmarkCase benchmarkCase) {
        BenchmarkRuntimeRequest request = new BenchmarkRuntimeRequest();
        request.caseId = benchmarkCase.caseId;
        request.scenario = benchmarkCase.scenario;
        request.input.putAll(benchmarkCase.input == null ? Map.of() : benchmarkCase.input);
        request.identity.putAll(benchmarkCase.identity == null ? Map.of() : benchmarkCase.identity);
        request.initialState.putAll(benchmarkCase.initialState == null ? Map.of() : benchmarkCase.initialState);
        return request;
    }
}
