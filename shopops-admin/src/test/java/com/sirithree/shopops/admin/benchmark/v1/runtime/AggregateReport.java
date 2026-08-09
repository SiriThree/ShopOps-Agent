package com.sirithree.shopops.admin.benchmark.v1.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class AggregateReport {
    public int totalCases;
    public int executedCases;
    public int passedCases;
    public int failedCases;
    public int notExecutedCases;
    public int infrastructureErrors;
    public Map<String, Integer> failureReasons = new LinkedHashMap<>();
}
