package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DailyReviewReportExecutor implements ToolExecutor {
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
        DateRangeView dateRange = dateRange(payload.get("dateRange"));

        List<Map<String, Object>> riskComments = listOfMap(negativeComments.get("riskComments"));
        List<Map<String, Object>> products = listOfMap(productCandidates.get("products"));
        Map<String, Object> compareYesterday = mapValue(orderSummary.get("compareYesterday"));
        Map<String, Object> compareSevenDayAvg = mapValue(orderSummary.get("compareSevenDayAvg"));

        String markdown = """
                # 店铺每日经营复盘

                复盘周期：%s 至 %s

                ## 1. 核心指标

                - GMV：%s，较昨日%s，较近 7 日均值%s
                - 订单数：%s，较昨日%s
                - 退款金额：%s
                - 退款率：%s，较近 7 日%s
                - 客单价：%s

                ## 2. 异常发现

                - 风险评价数：%s
                - 待优化商品数：%s
                - 优先关注：%s

                ## 3. 风险评价样本

                %s

                ## 4. 商品优化清单

                %s

                ## 5. 运营动作建议

                %s

                ## 6. 数据证据

                - 工具调用链：order.query_summary、comment.query_negative、product.query_candidates
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
                number(negativeComments.get("negativeCount")),
                number(productCandidates.get("candidateCount")),
                focusLine(riskComments, products),
                renderRiskComments(riskComments),
                renderProductCandidates(products),
                renderActions(orderSummary, riskComments, products),
                context.getTraceId()
        );

        Map<String, Object> data = Map.of(
                "title", "店铺每日经营复盘",
                "markdown", markdown,
                "summary", summary(orderSummary, negativeComments, productCandidates),
                "evidence", Map.of(
                        "toolCodes", List.of("order.query_summary", "comment.query_negative", "product.query_candidates"),
                        "riskCommentIds", riskComments.stream().map(item -> item.get("commentId")).limit(10).toList(),
                        "productIds", products.stream().map(item -> item.get("productId")).limit(10).toList()
                )
        );
        return ToolInvokeResult.success(data, null);
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
                                 List<Map<String, Object>> products) {
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
        if (builder.isEmpty()) {
            builder.append("- 今日暂无明显异常，建议维持现有投放和客服节奏。");
        }
        return builder.toString().stripTrailing();
    }

    private String focusLine(List<Map<String, Object>> riskComments, List<Map<String, Object>> products) {
        String riskProduct = riskComments.isEmpty() ? "暂无高风险评价商品" : value(riskComments.get(0), "productName");
        String candidateProduct = products.isEmpty() ? "暂无待优化商品" : value(products.get(0), "productName");
        return "评价风险优先看「%s」，商品优化优先看「%s」。".formatted(riskProduct, candidateProduct);
    }

    private String summary(Map<String, Object> orderSummary,
                           Map<String, Object> negativeComments,
                           Map<String, Object> productCandidates) {
        return "GMV %s，订单数 %s，退款率 %s；发现风险评价 %s 条、待优化商品 %s 个。".formatted(
                number(orderSummary.get("gmv")),
                number(orderSummary.get("orderCount")),
                percent(orderSummary.get("refundRate")),
                number(negativeComments.get("negativeCount")),
                number(productCandidates.get("candidateCount"))
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

    private record DateRangeView(String start, String end) {
    }
}
