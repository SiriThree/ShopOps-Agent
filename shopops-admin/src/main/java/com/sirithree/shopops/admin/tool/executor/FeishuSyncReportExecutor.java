package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FeishuSyncReportExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "feishu.sync_report";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("documentId", "FEISHU-DEMO-DOC-001");
        data.put("documentUrl", "https://feishu.example.com/docx/shopops-demo");
        data.put("status", "SYNCED");
        data.put("mode", "demo-connector");
        return data;
    }
}
