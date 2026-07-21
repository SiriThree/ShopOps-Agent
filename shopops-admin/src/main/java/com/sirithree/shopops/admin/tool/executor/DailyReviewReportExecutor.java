package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.model.config.ModelGatewayReportProperties;
import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelGatewayService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DailyReviewReportExecutor implements ToolExecutor {
    private final ModelGatewayReportProperties reportProperties;
    private final ModelGatewayService modelGatewayService;

    public DailyReviewReportExecutor() {
        this(new ModelGatewayReportProperties(), null);
    }

    @Autowired
    public DailyReviewReportExecutor(ModelGatewayReportProperties reportProperties,
                                     ModelGatewayService modelGatewayService) {
        this.reportProperties = reportProperties;
        this.modelGatewayService = modelGatewayService;
    }

    @Override
    public String toolCode() {
        return "report.generate_daily_review";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> payload = (Map<String, Object>) input;
        Map<String, Object> orderSummary = (Map<String, Object>) payload.getOrDefault("orderSummary", Map.of());
        Map<String, Object> negativeComments = (Map<String, Object>) payload.getOrDefault("negativeComments", Map.of());
        Map<String, Object> productCandidates = (Map<String, Object>) payload.getOrDefault("productCandidates", Map.of());
        Map<String, Object> adPerformance = (Map<String, Object>) payload.getOrDefault("adPerformance", Map.of());
        Map<String, Object> externalReportMetrics = (Map<String, Object>) payload.getOrDefault("externalReportMetrics", Map.of());
        DateRangeView dateRange = dateRange(payload.get("dateRange"));

        List<Map<String, Object>> riskComments = listOfMap(negativeComments.get("riskComments"));
        List<Map<String, Object>> products = listOfMap(productCandidates.get("products"));
        List<Map<String, Object>> campaigns = listOfMap(adPerformance.get("campaigns"));
        List<Map<String, Object>> channels = listOfMap(externalReportMetrics.get("topChannels"));
        Map<String, Object> compareYesterday = mapValue(orderSummary.get("compareYesterday"));
        Map<String, Object> compareSevenDayAvg = mapValue(orderSummary.get("compareSevenDayAvg"));

        String ruleMarkdown = """
                # 店铺每日经营复盘

                复盘周期：%s 至 %s

                ## 1. 核心指标

                - GMV：%s，较昨日%s，较近 7 日均值%s
                - 订单数：%s，较昨日%s
                - 退款金额：%s
                - 退款率：%s，较近 7 日%s
                - 客单价：%s

                ## 2. 广告投放

                - 广告消耗：%s
                - 曝光 / 点击：%s / %s
                - CTR：%s，CPC：%s
                - 转化率：%s，ROI：%s
                - 重点计划：%s

                ## 3. 平台报表

                - 访客数：%s，其中新访客 %s
                - 报表转化率：%s，复购率：%s
                - 收藏 / 加购：%s / %s
                - 重点渠道：%s

                ## 4. 异常发现

                - 风险评价数：%s
                - 待优化商品数：%s
                - 优先关注：%s

                ## 5. 风险评价样本

                %s

                ## 6. 商品优化清单

                %s

                ## 7. 运营动作建议

                %s

                ## 8. 数据证据

                - 工具调用链：order.query_summary、comment.query_negative、product.query_candidates、ad.query_performance、report.query_external_metrics
                - Trace ID：%s
                """.formatted(
                dateRange.start(),
                dateRange.end(),
                number(orderSummary.get("gmv")),
                growthText(compareYesterday.get("gmvGrowth")),
                growthText(compareSevenDayAvg.get("gmvGrowth")),
                number(orderSummary.get("orderCount")),
                growthText(compareYesterday.get("orderGrowth")),
                number(orderSummary.get("refundAmount")),
                percent(orderSummary.get("refundRate")),
                deltaText(compareSevenDayAvg.get("refundRateDelta")),
                number(orderSummary.get("avgOrderAmount")),
                number(adPerformance.get("spend")),
                number(adPerformance.get("impressions")),
                number(adPerformance.get("clicks")),
                percent(adPerformance.get("ctr")),
                number(adPerformance.get("cpc")),
                percent(adPerformance.get("conversionRate")),
                number(adPerformance.get("roi")),
                campaignFocus(campaigns),
                number(externalReportMetrics.get("visitorCount")),
                number(externalReportMetrics.get("newVisitorCount")),
                percent(externalReportMetrics.get("conversionRate")),
                percent(externalReportMetrics.get("repeatPurchaseRate")),
                number(externalReportMetrics.get("favoriteCount")),
                number(externalReportMetrics.get("cartAddCount")),
                channelFocus(channels),
                number(negativeComments.get("negativeCount")),
                number(productCandidates.get("candidateCount")),
                focusLine(riskComments, products),
                renderRiskComments(riskComments),
                renderProductCandidates(products),
                renderActions(orderSummary, riskComments, products, adPerformance, externalReportMetrics),
                context.getTraceId()
        );
        ModelReportView modelReport = modelReport(context, payload, orderSummary, negativeComments, productCandidates,
                adPerformance, externalReportMetrics, ruleMarkdown);
        String markdown = modelReport.markdown();

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("generationMode", modelReport.generationMode());
        evidence.put("toolCodes", List.of("order.query_summary", "comment.query_negative", "product.query_candidates",
                "ad.query_performance", "report.query_external_metrics"));
        evidence.put("riskCommentIds", riskComments.stream().map(item -> item.get("commentId")).limit(10).toList());
        evidence.put("productIds", products.stream().map(item -> item.get("productId")).limit(10).toList());
        evidence.put("campaignNames", campaigns.stream().map(item -> item.get("campaignName")).limit(10).toList());
        evidence.put("channelNames", channels.stream().map(item -> item.get("channelName")).limit(10).toList());
        evidence.put("modelCallId", modelReport.callId());
        evidence.put("modelProviderCode", modelReport.providerCode());

        Map<String, Object> data = Map.of(
                "title", "店铺每日经营复盘",
                "markdown", markdown,
                "summary", summary(orderSummary, negativeComments, productCandidates, adPerformance, externalReportMetrics),
                "evidence", evidence
        );
        return ToolInvokeResult.success(data, null);
    }

    private ModelReportView modelReport(ToolInvokeContext context,
                                        Map<String, Object> payload,
                                        Map<String, Object> orderSummary,
                                        Map<String, Object> negativeComments,
                                        Map<String, Object> productCandidates,
                                        Map<String, Object> adPerformance,
                                        Map<String, Object> externalReportMetrics,
                                        String fallbackMarkdown) {
        if (modelGatewayService == null || !reportProperties.isEnabled()) {
            return new ModelReportView(fallbackMarkdown, "RULE", null, null);
        }
        try {
            ModelInvokeParam param = new ModelInvokeParam();
            param.setProviderCode(reportProperties.getProviderCode());
            param.setModelName(blank(reportProperties.getModelName()) ? null : reportProperties.getModelName());
            param.setPromptCode(reportProperties.getPromptCode());
            param.setPromptVersion(blank(reportProperties.getPromptVersion()) ? null : reportProperties.getPromptVersion());
            param.setTraceId(context.getTraceId());
            param.setTaskId(context.getTaskId());
            param.setTimeoutMs(reportProperties.getTimeoutMs());
            param.setPrompt(modelPrompt(payload, orderSummary, negativeComments, productCandidates, adPerformance,
                    externalReportMetrics, fallbackMarkdown));
            param.setMetadata(Map.of(
                    "systemPrompt", "你是电商经营分析助手，请输出结构清晰、可直接阅读的中文 Markdown 经营复盘报告。",
                    "orderSummary", orderSummary,
                    "negativeComments", negativeComments,
                    "productCandidates", productCandidates,
                    "adPerformance", adPerformance,
                    "externalReportMetrics", externalReportMetrics,
                    "dateRange", payload.getOrDefault("dateRange", Map.of()),
                    "traceId", context.getTraceId()
            ));
            ModelInvokeResult result = modelGatewayService.invoke(
                    context.getTenantId(), context.getShopId(), context.getUserId(), "agent-report", param);
            if (ModelCallStatus.SUCCESS.equals(result.getStatus()) && !blank(result.getOutputText())) {
                return new ModelReportView(result.getOutputText(), "MODEL_GATEWAY", result.getCallId(), result.getProviderCode());
            }
        } catch (RuntimeException ignored) {
            // 报告生成不能阻塞 P0 复盘链路，失败时使用确定性的规则版报告。
        }
        return new ModelReportView(fallbackMarkdown, "RULE_FALLBACK", null, null);
    }

    private String modelPrompt(Map<String, Object> payload,
                               Map<String, Object> orderSummary,
                               Map<String, Object> negativeComments,
                               Map<String, Object> productCandidates,
                               Map<String, Object> adPerformance,
                               Map<String, Object> externalReportMetrics,
                               String fallbackMarkdown) {
        return """
                请根据以下结构化经营数据生成一份中文 Markdown 每日经营复盘报告。
                要求：保留核心指标、广告投放、平台报表、异常发现、风险评价样本、商品优化清单、运营动作建议和数据证据。
                日期范围：%s
                订单概览：%s
                风险评价：%s
                商品候选：%s
                广告投放：%s
                平台报表：%s

                可参考的规则版报告：
                %s
                """.formatted(payload.get("dateRange"), orderSummary, negativeComments, productCandidates,
                adPerformance, externalReportMetrics, fallbackMarkdown);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMap(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String renderRiskComments(List<Map<String, Object>> riskComments) {
        if (riskComments.isEmpty()) {
            return "暂无高风险评价样本。";
        }
        return riskComments.stream()
                .limit(3)
                .map(item -> "- [%s星] %s：%s".formatted(
                        value(item, "star"),
                        value(item, "productName"),
                        value(item, "content")
                ))
                .collect(Collectors.joining("\n"));
    }

    @SuppressWarnings("unchecked")
    private DateRangeView dateRange(Object value) {
        if (value instanceof DateRangeParam param) {
            return new DateRangeView(param.getStart(), param.getEnd());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> dateRange = (Map<String, Object>) map;
            return new DateRangeView(value(dateRange, "start"), value(dateRange, "end"));
        }
        return new DateRangeView("", "");
    }

    private String renderProductCandidates(List<Map<String, Object>> products) {
        if (products.isEmpty()) {
            return "暂无需要优先优化的商品。";
        }
        return products.stream()
                .limit(5)
                .map(item -> "- %s：评分 %s，库存 %s，区间销量 %s，风险评价 %s。原因：%s".formatted(
                        value(item, "productName"),
                        number(item.get("score")),
                        number(item.get("stock")),
                        number(item.get("salesQuantity")),
                        number(item.get("negativeCount")),
                        value(item, "reason")
                ))
                .collect(Collectors.joining("\n"));
    }

    private String renderActions(Map<String, Object> orderSummary,
                                 List<Map<String, Object>> riskComments,
                                 List<Map<String, Object>> products,
                                 Map<String, Object> adPerformance,
                                 Map<String, Object> externalReportMetrics) {
        StringBuilder builder = new StringBuilder();
        if (decimal(orderSummary.get("refundRate")).compareTo(new BigDecimal("0.05")) >= 0) {
            builder.append("- 退款率已高于 5%，优先复核退款订单和关联商品详情页。\n");
        }
        if (!riskComments.isEmpty()) {
            builder.append("- 对低星评价按商品聚类，先处理描述不符、破损、物流慢等可归因问题。\n");
        }
        if (!products.isEmpty()) {
            builder.append("- 对高分候选商品执行标题优化、主图 AB 测试和客服话术补强。\n");
        }
        if (decimal(adPerformance.get("roi")).compareTo(new BigDecimal("3.0")) < 0) {
            builder.append("- 广告 ROI 低于 3，建议收缩低转化计划预算，并把预算迁移到高 ROI 人群包。\n");
        }
        if (decimal(externalReportMetrics.get("conversionRate")).compareTo(new BigDecimal("0.03")) < 0) {
            builder.append("- 平台报表转化率低于 3%，建议检查商品详情页承接、优惠露出和首屏卖点。\n");
        }
        if (builder.isEmpty()) {
            builder.append("- 今日暂无明显异常，建议维持现有投放和客服节奏。");
        }
        return builder.toString().stripTrailing();
    }

    private String campaignFocus(List<Map<String, Object>> campaigns) {
        if (campaigns.isEmpty()) {
            return "暂无广告计划数据";
        }
        return campaigns.stream()
                .limit(3)
                .map(item -> "%s（消耗 %s，ROI %s）".formatted(
                        value(item, "campaignName"),
                        number(item.get("spend")),
                        number(item.get("roi"))
                ))
                .collect(Collectors.joining("；"));
    }

    private String channelFocus(List<Map<String, Object>> channels) {
        if (channels.isEmpty()) {
            return "暂无渠道报表数据";
        }
        return channels.stream()
                .limit(3)
                .map(item -> "%s（访客 %s，转化率 %s）".formatted(
                        value(item, "channelName"),
                        number(item.get("visitorCount")),
                        percent(item.get("conversionRate"))
                ))
                .collect(Collectors.joining("；"));
    }

    private String focusLine(List<Map<String, Object>> riskComments, List<Map<String, Object>> products) {
        String riskProduct = riskComments.isEmpty() ? "暂无高风险评价商品" : value(riskComments.get(0), "productName");
        String candidateProduct = products.isEmpty() ? "暂无待优化商品" : value(products.get(0), "productName");
        return "评价风险优先看「%s」，商品优化优先看「%s」。".formatted(riskProduct, candidateProduct);
    }

    private String summary(Map<String, Object> orderSummary,
                           Map<String, Object> negativeComments,
                           Map<String, Object> productCandidates,
                           Map<String, Object> adPerformance,
                           Map<String, Object> externalReportMetrics) {
        return "GMV %s，订单数 %s，退款率 %s；发现风险评价 %s 条、待优化商品 %s 个；广告消耗 %s，ROI %s；访客 %s，报表转化率 %s。".formatted(
                number(orderSummary.get("gmv")),
                number(orderSummary.get("orderCount")),
                percent(orderSummary.get("refundRate")),
                number(negativeComments.get("negativeCount")),
                number(productCandidates.get("candidateCount")),
                number(adPerformance.get("spend")),
                number(adPerformance.get("roi")),
                number(externalReportMetrics.get("visitorCount")),
                percent(externalReportMetrics.get("conversionRate"))
        );
    }

    private String growthText(Object value) {
        BigDecimal growth = decimal(value);
        if (growth.compareTo(BigDecimal.ZERO) == 0) {
            return "持平";
        }
        return growth.compareTo(BigDecimal.ZERO) > 0
                ? "增长 " + percent(growth)
                : "下降 " + percent(growth.abs());
    }

    private String deltaText(Object value) {
        BigDecimal delta = decimal(value);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return "持平";
        }
        return delta.compareTo(BigDecimal.ZERO) > 0
                ? "上升 " + percent(delta)
                : "下降 " + percent(delta.abs());
    }

    private String percent(Object value) {
        return decimal(value).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private String number(Object value) {
        if (value == null) {
            return "0";
        }
        if (value instanceof Number number) {
            BigDecimal decimal = new BigDecimal(number.toString());
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private String value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record DateRangeView(String start, String end) {
    }

    private record ModelReportView(String markdown, String generationMode, Long callId, String providerCode) {
    }
}
