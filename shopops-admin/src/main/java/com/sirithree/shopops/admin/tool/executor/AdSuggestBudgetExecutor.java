package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdSuggestBudgetExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "ad.suggest_budget";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("status", "SUGGESTED");
        data.put("approvalId", context.getApprovalId());
        data.put("budgetChanges", List.of(
                Map.of("campaignId", "AD-LOW-001", "changePercent", -20, "reason", "ROI below threshold"),
                Map.of("campaignId", "AD-GOOD-003", "changePercent", 15, "reason", "stable conversion")
        ));
        return data;
    }
}
