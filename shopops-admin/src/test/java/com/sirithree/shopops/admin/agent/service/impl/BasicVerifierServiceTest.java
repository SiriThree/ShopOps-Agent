package com.sirithree.shopops.admin.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskSpec;
import com.sirithree.shopops.admin.agent.domain.AgentVerificationResult;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BasicVerifierServiceTest {
    private final BasicVerifierService verifier = new BasicVerifierService();

    @Test
    void shouldPassWhenAllRequiredEvidenceIsAvailable() {
        AgentVerificationResult result = verifier.verify(context(true), executionResult(true));

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getScore()).isEqualTo(1.0);
        assertThat(result.getChecks()).allMatch(check -> "PASS".equals(check.getStatus()));
        assertThat(result.isRepairable()).isFalse();
        assertThat(result.getRepairToolCodes()).isEmpty();
    }

    @Test
    void shouldWarnWhenOptionalDegradedEvidenceIsMissing() {
        AgentVerificationResult result = verifier.verify(context(true), executionResult(false));

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getScore()).isLessThan(1.0);
        assertThat(result.isRepairable()).isTrue();
        assertThat(result.getMissingEvidence()).containsExactly("product_candidates");
        assertThat(result.getRepairToolCodes()).containsExactly("product.query_candidates");
        assertThat(result.getChecks())
                .anyMatch(check -> "evidence_product_candidates".equals(check.getCode()) && "WARN".equals(check.getStatus()));
    }

    @Test
    void shouldFailWhenRequiredEvidenceCannotDegrade() {
        AgentVerificationResult result = verifier.verify(context(false), executionResult(false));

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isRepairable()).isTrue();
        assertThat(result.getRepairToolCodes()).containsExactly("product.query_candidates");
        assertThat(result.getChecks())
                .anyMatch(check -> "evidence_product_candidates".equals(check.getCode()) && "FAIL".equals(check.getStatus()));
    }

    private AgentTaskContext context(boolean allowDegradedEvidence) {
        AgentTaskSpec spec = new AgentTaskSpec();
        spec.setIntent("product_optimization");
        spec.setRequiredEvidence(List.of("order_summary", "product_candidates", "negative_comments"));
        spec.setConstraints(Map.of("allowDegradedEvidence", allowDegradedEvidence));

        AgentTaskCreateParam param = new AgentTaskCreateParam();
        param.setTaskType("daily_review");
        param.setIntent("product_optimization");
        param.setTaskSpec(spec);

        AgentTaskContext context = new AgentTaskContext();
        context.setCreateParam(param);
        return context;
    }

    private AgentExecutionResult executionResult(boolean includeProducts) {
        Map<String, ToolInvokeResult> steps = new LinkedHashMap<>();
        steps.put("order.query_summary", ToolInvokeResult.success(Map.of("orderCount", 10), null));
        if (includeProducts) {
            steps.put("product.query_candidates", ToolInvokeResult.success(Map.of("candidateCount", 2), null));
        }
        steps.put("comment.query_negative", ToolInvokeResult.success(Map.of("negativeCount", 1), null));
        steps.put("report.generate_daily_review", ToolInvokeResult.success(Map.of("status", "generated"), null));

        AgentExecutionResult result = new AgentExecutionResult();
        result.setSuccess(true);
        result.setReportId(90001L);
        result.setStepResults(steps);
        return result;
    }
}
