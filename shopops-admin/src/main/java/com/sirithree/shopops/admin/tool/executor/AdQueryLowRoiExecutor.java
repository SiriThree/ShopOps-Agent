package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdQueryLowRoiExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "ad.query_low_roi";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("campaigns", List.of(
                Map.of("campaignId", "AD-LOW-001", "spend", 860.0, "roi", 0.74, "issue", "high click cost"),
                Map.of("campaignId", "AD-LOW-002", "spend", 520.0, "roi", 0.81, "issue", "low conversion")
        ));
        data.put("recommendations", recommendations("Reduce budget for AD-LOW-001 by 20%", "Move budget to high ROI product group"));
        return data;
    }
}
