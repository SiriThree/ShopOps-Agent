package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.CompositeTaskBenchmarkEvaluator;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.EvaluationResult;
import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BenchmarkEvaluatorTest {
    @Test
    void acceptsEquivalentLegalTraceWhenBusinessFactsAndStateAreCorrect() {
        BenchmarkCase c = baseCase();
        c.acceptableTools.addAll(List.of("order.query_summary", "report.generate_daily_review"));
        c.expectedOutcome.put("reportRequired", true);
        c.expectedOutcome.put("requiredEvidenceDomains", List.of("orders"));
        c.expectedOutcome.put("requiredTerminalTaskStates", List.of("SUCCESS"));

        CollectedEvidence e = evidence("SUCCESS");
        // Deliberately use a trace order different from any predeclared unique trace.
        e.toolLogs.add(Map.of("toolCode", "report.generate_daily_review", "status", "SUCCESS"));
        e.toolLogs.add(Map.of("toolCode", "order.query_summary", "status", "SUCCESS"));
        e.businessFacts.put("reportEvidenceDomains", List.of("orderSummary"));

        EvaluationResult result = new CompositeTaskBenchmarkEvaluator().evaluate(c, e);
        assertThat(result.passed).isTrue();
        assertThat(result.metricValues).containsEntry("taskSuccess", true);
    }

    @Test
    void rejectsForbiddenToolIndependentlyOfFinalTaskStatus() {
        BenchmarkCase c = baseCase();
        c.acceptableTools.add("order.query_summary");
        c.forbiddenTools.add("order.refund_execute");
        c.expectedOutcome.put("requiredTerminalTaskStates", List.of("SUCCESS"));
        CollectedEvidence e = evidence("SUCCESS");
        e.toolLogs.add(Map.of("toolCode", "order.refund_execute", "status", "SUCCESS"));

        EvaluationResult result = new CompositeTaskBenchmarkEvaluator().evaluate(c, e);
        assertThat(result.passed).isFalse();
        assertThat(result.failureReasons).contains(FailureReasonCode.FORBIDDEN_TOOL_USED);
    }

    private BenchmarkCase baseCase() {
        BenchmarkCase c = new BenchmarkCase();
        c.caseId = "eval"; c.benchmarkType = BenchmarkType.TASK;
        c.identity.put("tenantId", 1); c.identity.put("shopId", 1);
        return c;
    }

    private CollectedEvidence evidence(String status) {
        CollectedEvidence e = new CollectedEvidence();
        AgentTaskDto task = new AgentTaskDto(); task.setTaskId(1L); task.setStatus(status); e.task = task;
        OperationReportDto report = new OperationReportDto(); report.setReportId(2L); report.setStatus("READY"); e.report = report;
        e.businessFacts.put("reportExists", true);
        e.businessFacts.put("taskFinalState", status);
        return e;
    }
}
