package com.sirithree.shopops.admin.benchmark.v1;

import java.util.LinkedHashMap;
import java.util.Map;

/** Metadata that every benchmark execution report must carry. */
public class DatasetVersionContract {
    public String datasetName;
    public String datasetVersion;
    public String split;
    public int caseCount;
    public String gitCommit;
    public Map<String, Object> runtimeConfig = new LinkedHashMap<>();
    public Map<String, Object> modelConfig = new LinkedHashMap<>();
    public String toolMode;
    public String timestamp;
}
