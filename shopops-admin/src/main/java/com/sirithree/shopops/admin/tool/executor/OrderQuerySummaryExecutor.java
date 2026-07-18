package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderQuerySummaryExecutor implements ToolExecutor {
    @Override
    public String toolCode() {
        return "order.query_summary";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> data = Map.of(
                "gmv", 128936.50,
                "orderCount", 842,
                "refundAmount", 5360.00,
                "refundRate", 0.0416,
                "avgOrderAmount", 153.13,
                "compareYesterday", Map.of("gmvGrowth", 0.083, "orderGrowth", 0.057),
                "compareSevenDayAvg", Map.of("gmvGrowth", 0.026, "refundRateDelta", -0.004)
        );
        return ToolInvokeResult.success(data, null);
    }
}
