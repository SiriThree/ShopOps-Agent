package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryMcpToolService implements McpToolService {
    private final Map<String, McpToolDto> tools = new LinkedHashMap<>();

    public InMemoryMcpToolService() {
        register(new McpToolDto("order.query_summary", "订单核心指标查询", "order", "order:read", "low"));
        register(new McpToolDto("comment.query_negative", "差评风险查询", "comment", "comment:read", "low"));
        register(new McpToolDto("product.query_candidates", "待优化商品查询", "product", "product:read", "low"));
        register(new McpToolDto("ad.query_performance", "广告投放指标查询", "ad", "ad:read", "low"));
        register(new McpToolDto("report.query_external_metrics", "外部报表指标查询", "report", "report:read", "low"));
        register(new McpToolDto("report.generate_daily_review", "每日经营复盘报告生成", "report", "report:generate", "low"));
        register(approvalTool("order.refund_execute", "高风险退款执行", "order", "order:refund", "high"));
    }

    @Override
    public List<McpToolDto> listTools(Long tenantId) {
        return new ArrayList<>(tools.values());
    }

    @Override
    public McpToolDto getTool(Long tenantId, String toolCode) {
        return tools.get(toolCode);
    }

    private void register(McpToolDto tool) {
        tools.put(tool.getToolCode(), tool);
    }

    private McpToolDto approvalTool(String toolCode, String toolName, String category, String permissionCode, String riskLevel) {
        McpToolDto tool = new McpToolDto(toolCode, toolName, category, permissionCode, riskLevel);
        tool.setNeedApproval(true);
        return tool;
    }
}
