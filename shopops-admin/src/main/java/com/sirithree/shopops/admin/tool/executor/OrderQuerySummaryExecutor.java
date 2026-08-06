package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.business.service.OrderMetricsService;
import com.sirithree.shopops.admin.business.support.ToolInputParser;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderQuerySummaryExecutor implements ToolExecutor {
    private final OrderMetricsService orderMetricsService;

    public OrderQuerySummaryExecutor(OrderMetricsService orderMetricsService) {
        this.orderMetricsService = orderMetricsService;
    }

    @Override
    public String toolCode() {
        return "order.query_summary";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> inputMap = ToolInputParser.asMap(input);
        Long shopId = ToolInputParser.longValue(inputMap, "shopId", context.getShopId());
        LocalDate startDate = ToolInputParser.dateValue(inputMap, "startDate");
        LocalDate endDate = ToolInputParser.dateValue(inputMap, "endDate");
        Map<String, Object> data = orderMetricsService.querySummary(context.getTenantId(), shopId, startDate, endDate);
        return ToolInvokeResult.success(data, null);
    }
}
