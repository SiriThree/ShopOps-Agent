package com.sirithree.shopops.admin.tool.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.model.config.ModelGatewayReportProperties;
import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelGatewayService;
import com.sirithree.shopops.admin.organization.service.ShopRuntimeConfigService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DailyReviewReportExecutorTest {
    private final DailyReviewReportExecutor executor = new DailyReviewReportExecutor();

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateReportFromToolEvidenceAndDateRangeObject() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTraceId("tr_test");

        ToolInvokeResult result = executor.execute(context, baseInput());

        assertThat(result.getSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        String markdown = (String) data.get("markdown");
        Map<String, Object> evidence = (Map<String, Object>) data.get("evidence");

        assertThat(markdown)
                .contains("复盘周期：2026-07-18 至 2026-07-18")
                .contains("GMV：840")
                .contains("退款率：7.02%")
                .contains("广告投放")
                .contains("平台报表")
                .contains("便携收纳箱")
                .contains("运动毛巾")
                .contains("Trace ID：tr_test");
        assertThat(evidence).containsEntry("generationMode", "RULE");
        assertThat((List<Object>) evidence.get("toolCodes"))
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates",
                        "ad.query_performance", "report.query_external_metrics");
        assertThat((List<Object>) evidence.get("riskCommentIds")).containsExactly(50102);
        assertThat((List<Object>) evidence.get("productIds")).containsExactly(1016);
        assertThat((List<Object>) evidence.get("campaignNames")).containsExactly("夏季补水主推");
        assertThat((List<Object>) evidence.get("channelNames")).containsExactly("自然搜索");
        Map<String, Object> dataSources = (Map<String, Object>) evidence.get("dataSources");
        Map<String, Object> orderSource = (Map<String, Object>) dataSources.get("orderSummary");
        Map<String, Object> orderMetrics = (Map<String, Object>) orderSource.get("metrics");
        assertThat(orderSource).containsEntry("connectorCode", "unknown");
        assertThat(orderMetrics)
                .containsEntry("gmv", 840.0)
                .containsEntry("orderCount", 5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldApplyShopConfigThresholdsToReportEvidenceAndActions() {
        DailyReviewReportExecutor configuredExecutor = new DailyReviewReportExecutor(
                new ModelGatewayReportProperties(),
                null,
                new FixedShopRuntimeConfigService(Map.of(
                        "refund_rate_warn_threshold", "0.06",
                        "negative_comment_warn_threshold", "1",
                        "agent_model_policy", "balanced"
                ))
        );
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setTraceId("tr_config_report");

        ToolInvokeResult result = configuredExecutor.execute(context, baseInput());

        assertThat(result.getSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        String markdown = (String) data.get("markdown");
        Map<String, Object> evidence = (Map<String, Object>) data.get("evidence");
        Map<String, Object> shopConfig = (Map<String, Object>) evidence.get("shopConfig");
        assertThat(shopConfig)
                .containsEntry("refundRateWarnThreshold", "0.06")
                .containsEntry("negativeCommentWarnThreshold", "1")
                .containsEntry("agentModelPolicy", "balanced");
        assertThat(markdown)
                .contains("退款率已达到配置阈值 6.00%")
                .contains("风险评价数已达到配置阈值 1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateSpecializedCommentRiskReportByIntent() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTraceId("tr_comment_risk");
        Map<String, Object> input = new LinkedHashMap<>(baseInput());
        input.put("intent", "comment_risk");
        input.put("executedToolCodes", List.of("order.query_summary", "comment.query_negative",
                "product.query_candidates"));

        ToolInvokeResult result = executor.execute(context, input);

        assertThat(result.getSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        Map<String, Object> evidence = (Map<String, Object>) data.get("evidence");
        assertThat(data)
                .containsEntry("title", "店铺差评风险专项分析");
        assertThat(data.get("summary").toString()).contains("差评风险专项");
        assertThat(data.get("markdown").toString())
                .contains("# 店铺差评风险专项分析")
                .contains("## 1. 差评风险结论")
                .contains("## 2. 典型差评样本")
                .contains("## 4. 处理建议")
                .contains("order.query_summary, comment.query_negative, product.query_candidates");
        assertThat(data.get("markdown").toString())
                .doesNotContain("## 2. 广告投放")
                .doesNotContain("## 3. 平台报表");
        assertThat(evidence)
                .containsEntry("intent", "comment_risk");
        assertThat((List<Object>) evidence.get("toolCodes"))
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateProductOptimizationReportByIntent() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTraceId("tr_product_optimization");
        Map<String, Object> input = new LinkedHashMap<>(baseInput());
        input.put("intent", "product_optimization");
        input.put("executedToolCodes", List.of("order.query_summary", "product.query_candidates",
                "comment.query_negative"));

        ToolInvokeResult result = executor.execute(context, input);

        assertThat(result.getSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data)
                .containsEntry("title", "店铺低点击商品优化专项");
        assertThat(data.get("summary").toString()).contains("商品优化专项");
        assertThat(data.get("markdown").toString())
                .contains("# 店铺低点击商品优化专项")
                .contains("## 1. 商品优化结论")
                .contains("## 2. 商品候选清单")
                .contains("## 4. 优化动作")
                .contains("运动毛巾")
                .doesNotContain("## 2. 广告投放");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateAdAnomalyReportByIntent() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTraceId("tr_ad_anomaly");
        Map<String, Object> input = new LinkedHashMap<>(baseInput());
        input.put("intent", "ad_anomaly");
        input.put("executedToolCodes", List.of("order.query_summary", "ad.query_performance",
                "report.query_external_metrics"));

        ToolInvokeResult result = executor.execute(context, input);

        assertThat(result.getSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data)
                .containsEntry("title", "店铺投放异常专项检查");
        assertThat(data.get("summary").toString()).contains("投放异常专项");
        assertThat(data.get("markdown").toString())
                .contains("# 店铺投放异常专项检查")
                .contains("## 1. 投放异常结论")
                .contains("## 2. 高消耗计划")
                .contains("## 4. 预算调整建议")
                .contains("夏季补水主推")
                .doesNotContain("## 5. 风险评价样本")
                .doesNotContain("## 6. 商品优化清单");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotTreatMissingAdDataAsZeroPerformanceRisk() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTraceId("tr_ad_no_data");
        Map<String, Object> input = new LinkedHashMap<>(baseInput());
        input.put("intent", "ad_anomaly");
        input.put("adPerformance", Map.of("connectorCode", "file.ad-performance"));
        input.put("executedToolCodes", List.of("order.query_summary", "ad.query_performance",
                "report.query_external_metrics"));

        ToolInvokeResult result = executor.execute(context, input);

        assertThat(result.getSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        Map<String, Object> evidence = (Map<String, Object>) data.get("evidence");
        assertThat(evidence).containsEntry("adDataStatus", "NO_DATA");
        assertThat(data.get("summary").toString())
                .contains("暂无广告投放数据")
                .doesNotContain("建议排查高消耗低转化计划");
        assertThat(data.get("markdown").toString())
                .contains("暂无广告投放指标")
                .contains("广告数据状态：NO_DATA")
                .doesNotContain("ROI：0")
                .doesNotContain("ROI 低于 3")
                .doesNotContain("当前整体 ROI 尚可");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateReportThroughModelGatewayWhenEnabled() {
        ModelGatewayReportProperties properties = new ModelGatewayReportProperties();
        properties.setEnabled(true);
        properties.setProviderCode("echo");
        properties.setPromptCode("daily_review.report");
        properties.setPromptVersion("v1");
        CapturingModelGatewayService modelGateway = new CapturingModelGatewayService();
        DailyReviewReportExecutor modelExecutor = new DailyReviewReportExecutor(properties, modelGateway);

        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(2L);
        context.setTaskId(10001L);
        context.setTraceId("tr_model_report");

        ToolInvokeResult result = modelExecutor.execute(context, baseInput());

        assertThat(result.getSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        Map<String, Object> evidence = (Map<String, Object>) data.get("evidence");
        assertThat(data.get("markdown")).isEqualTo("# 模型生成的经营复盘\n\n- 建议优先处理退款风险。");
        assertThat(evidence)
                .containsEntry("generationMode", "MODEL_GATEWAY")
                .containsEntry("modelCallId", 99L)
                .containsEntry("modelProviderCode", "echo");
        assertThat(modelGateway.lastParam)
                .extracting(ModelInvokeParam::getPromptCode, ModelInvokeParam::getPromptVersion, ModelInvokeParam::getTraceId, ModelInvokeParam::getTaskId)
                .containsExactly("daily_review.report", "v1", "tr_model_report", 10001L);
        assertThat(modelGateway.lastParam.getPrompt())
                .contains("订单概览")
                .contains("平台报表")
                .contains("规则版报告");
    }

    private Map<String, Object> baseInput() {
        DateRangeParam dateRange = new DateRangeParam();
        dateRange.setStart("2026-07-18");
        dateRange.setEnd("2026-07-18");
        return Map.of(
                "dateRange", dateRange,
                "orderSummary", Map.of(
                        "gmv", 840.0,
                        "orderCount", 5,
                        "refundAmount", 59.0,
                        "refundRate", 0.0702,
                        "avgOrderAmount", 168.0,
                        "compareYesterday", Map.of("gmvGrowth", 2.0435, "orderGrowth", 1.5),
                        "compareSevenDayAvg", Map.of("gmvGrowth", 2.8009, "refundRateDelta", -0.0037)
                ),
                "negativeComments", Map.of(
                        "negativeCount", 1,
                        "riskComments", List.of(Map.of(
                                "commentId", 50102,
                                "productId", 1008,
                                "productName", "便携收纳箱",
                                "star", 1,
                                "content", "商品描述不符，已经申请退款"
                        ))
                ),
                "productCandidates", Map.of(
                        "candidateCount", 1,
                        "products", List.of(Map.of(
                                "productId", 1016,
                                "productName", "运动毛巾",
                                "score", 74.0,
                                "stock", 540,
                                "salesQuantity", 1,
                                "negativeCount", 1,
                                "reason", "库存高但区间销量偏低"
                        ))
                ),
                "adPerformance", Map.of(
                        "spend", 18600,
                        "impressions", 420000,
                        "clicks", 18600,
                        "ctr", 0.0443,
                        "cpc", 1.0,
                        "conversionRate", 0.086,
                        "roi", 3.72,
                        "campaigns", List.of(Map.of(
                                "campaignName", "夏季补水主推",
                                "spend", 9600,
                                "roi", 4.18
                        ))
                ),
                "externalReportMetrics", Map.of(
                        "visitorCount", 36520,
                        "newVisitorCount", 12860,
                        "conversionRate", 0.031,
                        "repeatPurchaseRate", 0.184,
                        "favoriteCount", 4210,
                        "cartAddCount", 2980,
                        "topChannels", List.of(Map.of(
                                "channelName", "自然搜索",
                                "visitorCount", 14200,
                                "conversionRate", 0.038
                        ))
                )
        );
    }

    private static class CapturingModelGatewayService implements ModelGatewayService {
        private ModelInvokeParam lastParam;

        @Override
        public ModelInvokeResult invoke(Long tenantId, Long shopId, Long userId, String username, ModelInvokeParam param) {
            this.lastParam = param;
            ModelInvokeResult result = new ModelInvokeResult();
            result.setCallId(99L);
            result.setProviderCode("echo");
            result.setModelName("echo-001");
            result.setStatus(ModelCallStatus.SUCCESS);
            result.setOutputText("# 模型生成的经营复盘\n\n- 建议优先处理退款风险。");
            result.setPromptTokens(10);
            result.setCompletionTokens(8);
            result.setTotalTokens(18);
            return result;
        }

        @Override
        public CommonPage<ModelCallLogDto> listLogs(Long tenantId, Long shopId, ModelCallLogQueryParam queryParam) {
            return CommonPage.of(List.of(), 1, 20, 0L);
        }
    }

    private record FixedShopRuntimeConfigService(Map<String, String> configs) implements ShopRuntimeConfigService {
        @Override
        public Optional<String> value(Long tenantId, Long shopId, String configKey) {
            return Optional.ofNullable(configs.get(configKey));
        }
    }
}
