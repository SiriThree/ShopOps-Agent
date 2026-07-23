package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReportExportExcelExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "report.export_excel";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("exportId", "XLSX-DEMO-001");
        data.put("fileName", "shopops-operation-report-demo.xlsx");
        data.put("sheets", List.of("Daily Review", "Risk Comments", "Product Candidates", "Ad Campaigns"));
        data.put("status", "EXPORTED");
        return data;
    }
}
