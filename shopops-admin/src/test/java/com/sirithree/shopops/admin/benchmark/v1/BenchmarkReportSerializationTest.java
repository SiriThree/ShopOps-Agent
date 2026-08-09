package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.benchmark.v1.report.BenchmarkReportWriter;
import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkReportSerializationTest {
    @TempDir Path tempDir;

    @Test
    void writesMachineReadableJsonAndSmokeMarkedMarkdown() throws Exception {
        EvaluationRun run = new EvaluationRun();
        run.metadata = new EvaluationRunMetadata();
        run.metadata.runId = "eval_test";
        run.metadata.benchmarkVersion = "ShopOpsBench-v1";
        run.metadata.datasetVersion = "smoke-v1";
        run.metadata.datasetSplit = "smoke";
        run.metadata.environment = BenchmarkEnvironment.DETERMINISTIC;
        run.metadata.executionLevel = BenchmarkExecutionLevel.SERVICE;
        run.aggregate = new AggregateReport();
        EvaluationRecord record = new EvaluationRecord();
        record.caseId = "smoke-1"; record.executionStatus = CaseExecutionStatus.PASSED;
        record.metricBreakdown.taskSuccess = true;
        run.caseExecutions.add(record);
        run.aggregate.totalCases=1; run.aggregate.executedCases=1; run.aggregate.passedCases=1;

        BenchmarkReportWriter.ReportPaths paths = new BenchmarkReportWriter(new ObjectMapper()).write(run, tempDir);
        assertThat(paths.json()).exists();
        assertThat(paths.markdown()).exists();
        assertThat(Files.readString(paths.markdown())).contains("Smoke/dev runs are not formal benchmark scores");
        assertThat(Files.readString(paths.json())).contains("smoke-1", "taskSuccess");
    }
}
