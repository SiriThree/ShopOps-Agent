package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductQueryLowClickExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "product.query_low_click";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("products", List.of(
                Map.of("productId", "PRD-LOW-001", "ctr", 0.006, "exposure", 8200, "reason", "weak title keywords"),
                Map.of("productId", "PRD-LOW-002", "ctr", 0.008, "exposure", 5100, "reason", "main image mismatch")
        ));
        data.put("recommendations", recommendations("Rewrite title keyword order", "Replace first image with scenario image"));
        return data;
    }
}
