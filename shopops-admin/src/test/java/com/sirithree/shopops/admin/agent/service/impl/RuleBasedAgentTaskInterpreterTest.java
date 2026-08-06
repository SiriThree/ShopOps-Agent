package com.sirithree.shopops.admin.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.agent.domain.AgentTaskInterpretation;
import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import org.junit.jupiter.api.Test;

class RuleBasedAgentTaskInterpreterTest {
    private final RuleBasedAgentTaskInterpreter interpreter = new RuleBasedAgentTaskInterpreter();

    @Test
    void shouldBuildDifferentTaskSpecsForSupportedIntents() {
        AgentTaskInterpretation daily = interpreter.interpret("生成今天店铺运营日报", dateRange());
        AgentTaskInterpretation comments = interpreter.interpret("分析最近差评原因", dateRange());
        AgentTaskInterpretation products = interpreter.interpret("找出低点击商品并给优化建议", dateRange());

        assertThat(daily.getTaskSpec().getIntent()).isEqualTo("daily_review");
        assertThat(daily.getTaskSpec().getRequiredEvidence())
                .containsExactly("order_summary", "negative_comments", "product_candidates", "ad_performance", "external_metrics");

        assertThat(comments.getTaskSpec().getIntent()).isEqualTo("comment_risk");
        assertThat(comments.getTaskSpec().getRequiredEvidence())
                .containsExactly("order_summary", "negative_comments", "product_candidates");

        assertThat(products.getTaskSpec().getIntent()).isEqualTo("product_optimization");
        assertThat(products.getTaskSpec().getRequiredEvidence())
                .containsExactly("order_summary", "product_candidates", "negative_comments");
        assertThat(products.getTaskSpec().getConstraints())
                .containsEntry("readOnlyAnalysis", true)
                .containsEntry("requireTraceableEvidence", true);
    }

    private DateRangeParam dateRange() {
        DateRangeParam dateRange = new DateRangeParam();
        dateRange.setStart("2026-07-28");
        dateRange.setEnd("2026-07-28");
        return dateRange;
    }
}
