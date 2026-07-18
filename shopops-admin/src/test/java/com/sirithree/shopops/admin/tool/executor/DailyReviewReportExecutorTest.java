package com.sirithree.shopops.admin.tool.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DailyReviewReportExecutorTest {
    private final DailyReviewReportExecutor executor = new DailyReviewReportExecutor();

    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateReportFromToolEvidenceAndDateRangeObject() {
        DateRangeParam dateRange = new DateRangeParam();
        dateRange.setStart("2026-07-18");
        dateRange.setEnd("2026-07-18");

        ToolInvokeContext context = new ToolInvokeContext();
        context.setTraceId("tr_test");

        Map<String, Object> input = Map.of(
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
                )
        );

        ToolInvokeResult result = executor.execute(context, input);

        assertThat(result.getSuccess()).isTrue();
        Map<String, Object> data = (Map<String, Object>) result.getData();
        String markdown = (String) data.get("markdown");
        Map<String, Object> evidence = (Map<String, Object>) data.get("evidence");

        assertThat(markdown)
                .contains("复盘周期：2026-07-18 至 2026-07-18")
                .contains("GMV：840")
                .contains("退款率：7.02%")
                .contains("便携收纳箱")
                .contains("运动毛巾")
                .contains("Trace ID：tr_test");
        assertThat((List<Object>) evidence.get("riskCommentIds")).containsExactly(50102);
        assertThat((List<Object>) evidence.get("productIds")).containsExactly(1016);
    }
}
