package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderQueryRefundRiskExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "order.query_refund_risk";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("refundRiskRate", 0.0763);
        data.put("riskOrders", List.of(
                Map.of("orderId", "OD-DEMO-1002", "reason", "canceled high value order", "amount", 1288.0),
                Map.of("orderId", "OD-DEMO-1041", "reason", "late delivery complaint", "amount", 488.0)
        ));
        data.put("recommendations", recommendations("Review logistics delay orders", "Prepare refund retention script"));
        return data;
    }
}
