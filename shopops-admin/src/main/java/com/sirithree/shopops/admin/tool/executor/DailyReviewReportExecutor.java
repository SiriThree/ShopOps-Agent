package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.util.List;
import java.util.Map;
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
        Map<String, Object> orderSummary = (Map<String, Object>) payload.get("orderSummary");
        Map<String, Object> negativeComments = (Map<String, Object>) payload.get("negativeComments");
        Map<String, Object> productCandidates = (Map<String, Object>) payload.get("productCandidates");

        String markdown = """
                # 店铺每日经营复盘

                ## 1. 核心指标

                - GMV：%s
                - 订单数：%s
                - 退款金额：%s
                - 退款率：%s
                - 客单价：%s

                ## 2. 异常发现

                - 新增差评数：%s
                - 待优化商品数：%s

                ## 3. 可能原因

                差评主要集中在物流慢、描述不符和包装破损；待优化商品存在库存压力、评论风险和标题关键词覆盖不足。

                ## 4. 优化建议

                - 优先处理高风险差评，补充订单和物流上下文后生成客服话术。
                - 对库存高销量低商品做标题和主图 AB 测试。
                - 复核描述不符商品详情页，避免继续放大退款风险。

                ## 5. 数据证据

                - 工具调用链：order.query_summary、comment.query_negative、product.query_candidates
                """.formatted(
                orderSummary.get("gmv"),
                orderSummary.get("orderCount"),
                orderSummary.get("refundAmount"),
                orderSummary.get("refundRate"),
                orderSummary.get("avgOrderAmount"),
                negativeComments.get("negativeCount"),
                productCandidates.get("candidateCount")
        );

        Map<String, Object> data = Map.of(
                "title", "店铺每日经营复盘",
                "markdown", markdown,
                "summary", "今日 GMV 表现稳定，差评和部分商品优化需要运营跟进。",
                "evidence", Map.of("toolCodes", List.of("order.query_summary", "comment.query_negative", "product.query_candidates"))
        );
        return ToolInvokeResult.success(data, null);
    }
}
