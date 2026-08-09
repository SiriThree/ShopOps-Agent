package com.sirithree.shopops.admin.benchmark.v1.runtime;

import java.time.Instant;
import java.util.UUID;

public class EvaluationRunMetadataFactory {
    public EvaluationRunMetadata create(String datasetVersion,
                                        String split,
                                        BenchmarkEnvironment environment,
                                        BenchmarkExecutionLevel executionLevel,
                                        String runtimeMode,
                                        String interpreterMode,
                                        String plannerMode,
                                        String modelMode,
                                        String modelProvider,
                                        String modelName,
                                        Double modelTemperature,
                                        String toolProviderMode,
                                        String databaseMode,
                                        String queueMode,
                                        Long randomSeed,
                                        Long faultSeed) {
        EvaluationRunMetadata metadata = new EvaluationRunMetadata();
        metadata.runId = "eval_" + UUID.randomUUID().toString().replace("-", "");
        metadata.benchmarkVersion = "ShopOpsBench-v1";
        metadata.datasetVersion = datasetVersion;
        metadata.datasetSplit = split;
        metadata.gitCommit = firstNonBlank(System.getenv("GIT_COMMIT"), System.getProperty("git.commit"));
        if (metadata.gitCommit == null) metadata.unavailable.put("gitCommit", "unavailable");
        metadata.environment = environment;
        metadata.executionLevel = executionLevel;
        metadata.runtimeMode = runtimeMode;
        metadata.interpreterMode = interpreterMode;
        metadata.plannerMode = plannerMode;
        metadata.modelMode = modelMode;
        metadata.modelProvider = modelProvider;
        metadata.modelName = modelName;
        metadata.modelTemperature = modelTemperature;
        metadata.toolProviderMode = toolProviderMode;
        metadata.databaseMode = databaseMode;
        metadata.queueMode = queueMode;
        metadata.randomSeed = randomSeed;
        metadata.faultSeed = faultSeed;
        metadata.startedAt = Instant.now();
        return metadata;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
