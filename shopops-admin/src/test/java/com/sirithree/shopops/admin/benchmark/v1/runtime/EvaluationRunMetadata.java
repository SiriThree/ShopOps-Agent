package com.sirithree.shopops.admin.benchmark.v1.runtime;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class EvaluationRunMetadata {
    public String runId;
    public String benchmarkVersion;
    public String datasetVersion;
    public String datasetSplit;
    public String gitCommit;
    public BenchmarkEnvironment environment;
    public BenchmarkExecutionLevel executionLevel;
    public String runtimeMode;
    public String interpreterMode;
    public String plannerMode;
    public String modelMode;
    public String modelProvider;
    public String modelName;
    public Double modelTemperature;
    public String toolProviderMode;
    public String databaseMode;
    public String queueMode;
    public String externalSystemMode;
    public String authorizationMode;
    public Long randomSeed;
    public Long faultSeed;
    public Instant startedAt;
    public Instant finishedAt;
    public Map<String, Object> unavailable = new LinkedHashMap<>();
}
