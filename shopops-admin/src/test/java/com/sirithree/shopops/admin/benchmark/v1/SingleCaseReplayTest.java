package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.benchmark.v1.evidence.*;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.*;
import com.sirithree.shopops.admin.benchmark.v1.runtime.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SingleCaseReplayTest {
    @Test
    void caseIdFilterExecutesOnlyRequestedCase() {
        AtomicInteger calls = new AtomicInteger();
        BenchmarkRuntimeGateway gateway = (runtimeRequest, runRequest) -> {
            calls.incrementAndGet();
            BenchmarkRuntimeResult r = new BenchmarkRuntimeResult();
            r.executionStatus = CaseExecutionStatus.EXECUTED;
            r.taskId = 42L;
            r.finalState = "SUCCESS";
            return r;
        };
        BenchmarkEvidenceCollector collector = (tenant, shop, task, trace) -> {
            CollectedEvidence e = new CollectedEvidence();
            e.businessFacts.put("reportExists", true);
            e.businessFacts.put("taskFinalState", "SUCCESS");
            return e;
        };
        BenchmarkEvaluator evaluator = (c, e) -> new EvaluationResult()
                .metric("businessOutcomeCorrect", true).metric("toolExecutionValid", true)
                .metric("governanceSatisfied", true).metric("noUnexpectedSideEffect", true)
                .metric("finalStateCorrect", true).metric("taskSuccess", true);
        ShopOpsBenchmarkRunner runner = new ShopOpsBenchmarkRunner(gateway, collector, evaluator);

        BenchmarkCase one = taskCase("one");
        BenchmarkCase two = taskCase("two");
        BenchmarkRunRequest request = new BenchmarkRunRequest();
        request.caseId = "two";
        request.benchmarkType = BenchmarkType.TASK;
        EvaluationRunMetadata metadata = metadata();

        EvaluationRun run = runner.run(List.of(one, two), request, metadata);
        assertThat(calls).hasValue(1);
        assertThat(run.caseExecutions).extracting(r -> r.caseId).containsExactly("two");
    }

    private BenchmarkCase taskCase(String id) {
        BenchmarkCase c = new BenchmarkCase(); c.caseId=id; c.benchmarkType=BenchmarkType.TASK;
        c.identity.put("tenantId",1); c.identity.put("shopId",1); return c;
    }
    private EvaluationRunMetadata metadata() {
        EvaluationRunMetadata m = new EvaluationRunMetadata(); m.runId="r"; m.datasetVersion="d";
        m.datasetSplit="dev"; m.environment=BenchmarkEnvironment.DETERMINISTIC; m.executionLevel=BenchmarkExecutionLevel.SERVICE;
        return m;
    }
}
