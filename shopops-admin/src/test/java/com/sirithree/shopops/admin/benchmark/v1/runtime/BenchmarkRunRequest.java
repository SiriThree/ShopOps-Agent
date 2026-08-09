package com.sirithree.shopops.admin.benchmark.v1.runtime;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

public class BenchmarkRunRequest {
    public String datasetSplit = "smoke";
    public String caseId;
    public String scenario;
    public String tag;
    public com.sirithree.shopops.admin.benchmark.v1.BenchmarkType benchmarkType = com.sirithree.shopops.admin.benchmark.v1.BenchmarkType.TASK;
    public BenchmarkEnvironment environment = BenchmarkEnvironment.DETERMINISTIC;
    public BenchmarkExecutionLevel executionLevel = BenchmarkExecutionLevel.HTTP;
    public Duration completionTimeout = Duration.ofSeconds(10);
    public Duration pollInterval = Duration.ofMillis(50);
    public Set<String> selectedTags = new LinkedHashSet<>();
}
