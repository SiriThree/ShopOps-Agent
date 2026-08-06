package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.business.service.ExternalReportMetricsService;
import com.sirithree.shopops.admin.business.support.ToolInputParser;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReportQueryExternalMetricsExecutor implements ToolExecutor {
    private final ExternalReportMetricsService externalReportMetricsService;

    public ReportQueryExternalMetricsExecutor(ExternalReportMetricsService externalReportMetricsService) {
        this.externalReportMetricsService = externalReportMetricsService;
    }

    @Override
    public String toolCode() {
        return "report.query_external_metrics";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> inputMap = ToolInputParser.asMap(input);
        Long shopId = ToolInputParser.longValue(inputMap, "shopId", context.getShopId());
        LocalDate startDate = ToolInputParser.dateValue(inputMap, "startDate");
        LocalDate endDate = ToolInputParser.dateValue(inputMap, "endDate");
        Map<String, Object> data = externalReportMetricsService.queryMetrics(context.getTenantId(), shopId, startDate, endDate);
        return ToolInvokeResult.success(data, null);
    }
}
