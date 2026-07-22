package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.business.service.AdPerformanceService;
import com.sirithree.shopops.admin.business.support.ToolInputParser;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdQueryPerformanceExecutor implements ToolExecutor {
    private final AdPerformanceService adPerformanceService;

    public AdQueryPerformanceExecutor(AdPerformanceService adPerformanceService) {
        this.adPerformanceService = adPerformanceService;
    }

    @Override
    public String toolCode() {
        return "ad.query_performance";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> inputMap = ToolInputParser.asMap(input);
        Long shopId = ToolInputParser.longValue(inputMap, "shopId", context.getShopId());
        LocalDate startDate = ToolInputParser.dateValue(inputMap, "startDate");
        LocalDate endDate = ToolInputParser.dateValue(inputMap, "endDate");
        Map<String, Object> data = adPerformanceService.queryPerformance(context.getTenantId(), shopId, startDate, endDate);
        return ToolInvokeResult.success(data, null);
    }
}
