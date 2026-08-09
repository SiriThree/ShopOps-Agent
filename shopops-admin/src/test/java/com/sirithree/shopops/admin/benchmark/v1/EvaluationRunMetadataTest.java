package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import org.junit.jupiter.api.Test;

class EvaluationRunMetadataTest {
    @Test
    void recordsEnvironmentAndMarksUnavailableCommitInsteadOfInventingOne() {
        EvaluationRunMetadata metadata = new EvaluationRunMetadataFactory().create(
                "shopopsbench-v1-smoke", "smoke", BenchmarkEnvironment.DETERMINISTIC,
                BenchmarkExecutionLevel.HTTP, "memory-sync", "RULE_BASED", "RULE_BASED",
                "DISABLED", null, null, null, "LOCAL+MCP_TEST_ADAPTER", "MEMORY", "SYNC", 7L, 11L);

        assertThat(metadata.runId).startsWith("eval_");
        assertThat(metadata.benchmarkVersion).isEqualTo("ShopOpsBench-v1");
        assertThat(metadata.environment).isEqualTo(BenchmarkEnvironment.DETERMINISTIC);
        assertThat(metadata.executionLevel).isEqualTo(BenchmarkExecutionLevel.HTTP);
        if (metadata.gitCommit == null) assertThat(metadata.unavailable).containsKey("gitCommit");
    }
}
