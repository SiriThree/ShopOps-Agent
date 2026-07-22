package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AgentNaturalLanguageTaskIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateDailyReviewTaskFromNaturalLanguage() {
        Map<String, Object> result = dataOf(post(
                "/api/agent/tasks/natural-language",
                Map.of(
                        "userInput", "Generate a daily operations report and analyze negative comments.",
                        "dateRange", Map.of("start", "2026-07-22", "end", "2026-07-22")
                )
        ));

        assertThat(result)
                .containsEntry("intent", "daily_review")
                .containsEntry("taskType", "daily_review");
        assertThat(((Number) result.get("confidence")).doubleValue()).isGreaterThan(0.8);
        assertThat((List<String>) result.get("focusAreas"))
                .contains("运营日报", "差评风险");
        assertThat((List<String>) result.get("dataSources"))
                .contains("订单汇总", "评价明细", "平台外部指标");

        Map<String, Object> taskResult = castMap(result.get("task"));
        Integer taskId = ((Number) taskResult.get("taskId")).intValue();
        Map<String, Object> task = dataOf(get("/api/agent/tasks/" + taskId));
        assertThat(task.get("status")).isEqualTo("SUCCESS");
        assertThat(task.get("reportId")).isNotNull();

        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps"));
        assertThat(steps).extracting(step -> step.get("toolCode"))
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates",
                        "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldClassifySpecializedNaturalLanguageIntent() {
        Map<String, Object> result = dataOf(post(
                "/api/agent/tasks/natural-language",
                Map.of(
                        "userInput", "Analyze recent negative comments and find the main risk reasons.",
                        "dateRange", Map.of("start", "2026-07-22", "end", "2026-07-22")
                )
        ));

        assertThat(result)
                .containsEntry("intent", "comment_risk")
                .containsEntry("taskType", "daily_review");
        assertThat(result.get("intentLabel")).isNotNull();
        assertThat((List<String>) result.get("recommendedActions")).isNotEmpty();

        Map<String, Object> taskResult = castMap(result.get("task"));
        Integer taskId = ((Number) taskResult.get("taskId")).intValue();
        Map<String, Object> task = dataOf(get("/api/agent/tasks/" + taskId));
        assertThat(task.get("status")).isEqualTo("SUCCESS");
        assertThat(task.get("resultSummary")).isEqualTo("Comment risk analysis report generated");

        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps"));
        assertThat(steps).extracting(step -> step.get("toolCode"))
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates", "report.generate_daily_review");

        Integer reportId = ((Number) task.get("reportId")).intValue();
        Map<String, Object> report = dataOf(get("/api/reports/" + reportId));
        assertThat(report.get("title")).isEqualTo("店铺差评风险专项分析");
        assertThat(report.get("summary").toString()).contains("差评风险专项");
        assertThat(castMap(report.get("evidence")))
                .containsEntry("intent", "comment_risk");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateOlistDailyReviewFromDefaultDemoConnectors() {
        Map<String, Object> result = dataOf(post(
                "/api/agent/tasks/natural-language",
                Map.of(
                        "userInput", "基于 Olist 真实订单和评价数据，生成 2018-08-07 店铺运营日报。",
                        "dateRange", Map.of("start", "2018-08-07", "end", "2018-08-07")
                )
        ));

        Map<String, Object> taskResult = castMap(result.get("task"));
        Integer taskId = ((Number) taskResult.get("taskId")).intValue();
        Map<String, Object> task = dataOf(get("/api/agent/tasks/" + taskId));
        assertThat(task.get("status")).isEqualTo("SUCCESS");

        Integer reportId = ((Number) task.get("reportId")).intValue();
        Map<String, Object> report = dataOf(get("/api/reports/" + reportId));
        Map<String, Object> evidence = castMap(report.get("evidence"));
        Map<String, Object> dataSources = castMap(evidence.get("dataSources"));
        Map<String, Object> orderSource = castMap(dataSources.get("orderSummary"));
        Map<String, Object> orderMetrics = castMap(orderSource.get("metrics"));
        Map<String, Object> commentSource = castMap(dataSources.get("negativeComments"));
        Map<String, Object> commentMetrics = castMap(commentSource.get("metrics"));
        Map<String, Object> productSource = castMap(dataSources.get("productCandidates"));
        Map<String, Object> productMetrics = castMap(productSource.get("metrics"));

        assertThat(orderSource).containsEntry("connectorCode", "file.order-summary");
        assertThat(orderMetrics)
                .containsEntry("gmv", 62057.77)
                .containsEntry("orderCount", 370)
                .containsEntry("refundRate", 0.0763);
        assertThat(commentSource).containsEntry("connectorCode", "file.negative-comments");
        assertThat(commentMetrics).containsEntry("negativeCount", 51);
        assertThat(productSource).containsEntry("connectorCode", "file.product-candidates");
        assertThat(productMetrics).containsEntry("candidateCount", 10);
    }

    @Test
    void shouldRoutePortfolioDemoPrompts() {
        assertDemoPromptIntent("帮我生成今天店铺运营日报，汇总订单、评价、商品、投放和平台指标。", "daily_review");
        assertDemoPromptIntent("帮我分析最近差评原因，识别需要优先处理的风险点和受影响商品。", "comment_risk");
        assertDemoPromptIntent("帮我找出低点击或待优化商品，并给出标题和运营优化建议。", "product_optimization");
        assertDemoPromptIntent("帮我检查高消耗低转化投放计划，并输出预算调整建议。", "ad_anomaly");
    }

    private void assertDemoPromptIntent(String userInput, String expectedIntent) {
        Map<String, Object> result = dataOf(post(
                "/api/agent/tasks/natural-language",
                Map.of(
                        "userInput", userInput,
                        "dateRange", Map.of("start", "2026-07-22", "end", "2026-07-22")
                )
        ));

        assertThat(result)
                .containsEntry("intent", expectedIntent)
                .containsEntry("taskType", "daily_review");
    }
}
