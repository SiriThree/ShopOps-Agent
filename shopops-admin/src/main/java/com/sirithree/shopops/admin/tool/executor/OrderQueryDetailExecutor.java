package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderQueryDetailExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "order.query_detail";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("orders", List.of(
                Map.of("orderId", "OD-DEMO-1001", "amount", 329.9, "status", "PAID", "risk", "LOW"),
                Map.of("orderId", "OD-DEMO-1002", "amount", 1288.0, "status", "CANCELED", "risk", "HIGH")
        ));
        data.put("summary", "2 representative orders returned for operations investigation");
        return data;
    }
}
