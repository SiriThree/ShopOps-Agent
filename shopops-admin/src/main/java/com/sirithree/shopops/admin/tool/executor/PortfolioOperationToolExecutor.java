package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.business.support.ToolInputParser;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class PortfolioOperationToolExecutor implements ToolExecutor {
    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> payload = ToolInputParser.asMap(input);
        return ToolInvokeResult.success(output(context, payload), null);
    }

    protected abstract Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input);

    protected Map<String, Object> base(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tenantId", context.getTenantId());
        data.put("shopId", ToolInputParser.longValue(input, "shopId", context.getShopId()));
        data.put("generatedAt", LocalDateTime.now().toString());
        return data;
    }

    protected List<Map<String, Object>> recommendations(String primaryAction, String secondaryAction) {
        return List.of(
                Map.of("priority", "P0", "action", primaryAction),
                Map.of("priority", "P1", "action", secondaryAction)
        );
    }
}
