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
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.List;
import java.util.Map;
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
}
