package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductUpdateTitleExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "product.update_title";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        String newTitle = String.valueOf(input.getOrDefault("newTitle", "Optimized demo title"));
        data.put("productId", String.valueOf(input.getOrDefault("productId", "PRD-LOW-001")));
        data.put("newTitle", newTitle);
        data.put("status", "UPDATED");
        data.put("approvalId", context.getApprovalId());
        data.put("titleLength", newTitle.length());
        return data;
    }
}
