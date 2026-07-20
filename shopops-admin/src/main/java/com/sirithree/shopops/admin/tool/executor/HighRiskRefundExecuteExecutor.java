package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.business.support.ToolInputParser;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HighRiskRefundExecuteExecutor implements ToolExecutor {
    @Override
    public String toolCode() {
        return "order.refund_execute";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> inputMap = ToolInputParser.asMap(input);
        Long shopId = ToolInputParser.longValue(inputMap, "shopId", context.getShopId());
        Integer refundAmount = ToolInputParser.intValue(inputMap, "refundAmount", 0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("refundId", "RF" + LocalDateTime.now().toString().replace("-", "").replace(":", "").replace(".", ""));
        data.put("status", "EXECUTED");
        data.put("tenantId", context.getTenantId());
        data.put("shopId", shopId);
        data.put("refundAmount", refundAmount);
        data.put("approvalId", context.getApprovalId());
        return ToolInvokeResult.success(data, null);
    }
}
