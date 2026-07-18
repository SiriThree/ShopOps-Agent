package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.business.service.ProductOptimizationService;
import com.sirithree.shopops.admin.business.support.ToolInputParser;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductQueryCandidatesExecutor implements ToolExecutor {
    private final ProductOptimizationService productOptimizationService;

    public ProductQueryCandidatesExecutor(ProductOptimizationService productOptimizationService) {
        this.productOptimizationService = productOptimizationService;
    }

    @Override
    public String toolCode() {
        return "product.query_candidates";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> inputMap = ToolInputParser.asMap(input);
        Long shopId = ToolInputParser.longValue(inputMap, "shopId", context.getShopId());
        LocalDate startDate = ToolInputParser.dateValue(inputMap, "startDate");
        LocalDate endDate = ToolInputParser.dateValue(inputMap, "endDate");
        Integer limit = ToolInputParser.intValue(inputMap, "limit", 10);
        Map<String, Object> data = productOptimizationService.queryCandidates(context.getTenantId(), shopId, startDate, endDate, limit);
        return ToolInvokeResult.success(data, null);
    }
}
