package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
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
        register(new McpToolDto("order.query_summary", "Order summary query", "order", "order:read", "low"));
        register(new McpToolDto("order.query_detail", "Order detail query", "order", "order:read", "low"));
        register(new McpToolDto("order.query_refund_risk", "Refund risk query", "order", "order:read", "medium"));
        register(approvalTool("order.refund_execute", "Refund execution", "order", "order:refund", "high"));

        McpToolDto negativeComment = new McpToolDto(
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                "Negative comment query",
                "comment",
                "comment:read",
                "low");
        negativeComment.setDescription("Query low-star and high-risk comments through the Commerce MCP server");
        negativeComment.setInputSchema(CommerceMcpContracts.canonicalJson(
                CommerceMcpContracts.commentQueryNegativeInputSchema()));
        negativeComment.setOutputSchema(CommerceMcpContracts.canonicalJson(
                CommerceMcpContracts.commentQueryNegativeOutputSchema()));
        negativeComment.setProviderType(CommerceMcpContracts.PROVIDER_MCP);
        negativeComment.setMcpServerCode(CommerceMcpContracts.SERVER_CODE);
        negativeComment.setRemoteToolName(CommerceMcpContracts.COMMENT_QUERY_NEGATIVE);
        negativeComment.setSchemaHash(CommerceMcpContracts.commentQueryNegativeSchemaHash());
        negativeComment.setRemoteVersion("1.0.0");
        negativeComment.setDiscoveryStatus(CommerceMcpContracts.DISCOVERY_READY);
        negativeComment.setIdempotent(true);
        register(negativeComment);
        register(new McpToolDto("comment.analyze_sentiment", "Comment sentiment analysis", "comment", "comment:read", "low"));
        register(new McpToolDto("comment.create_reply_draft", "Comment reply draft", "comment", "comment:write", "medium"));

        register(new McpToolDto("product.query_candidates", "Product optimization candidates", "product", "product:read", "low"));
        register(new McpToolDto("product.query_low_click", "Low click product query", "product", "product:read", "low"));
        register(new McpToolDto("product.optimize_title", "Product title optimization", "product", "product:write", "medium"));
        register(approvalTool("product.update_title", "Product title update", "product", "product:write", "high"));

        register(new McpToolDto("ad.query_performance", "Ad performance query", "ad", "ad:read", "low"));
        register(new McpToolDto("ad.query_low_roi", "Low ROI campaign query", "ad", "ad:read", "low"));
        register(approvalTool("ad.suggest_budget", "Ad budget adjustment suggestion", "ad", "ad:write", "high"));

        register(new McpToolDto("report.query_external_metrics", "External report metrics query", "report", "report:read", "low"));
        register(new McpToolDto("report.generate_daily_review", "Daily operation report generation", "report", "report:generate", "low"));
        register(new McpToolDto("report.export_excel", "Operation report Excel export", "report", "report:export", "medium"));
        register(new McpToolDto("feishu.sync_report", "Feishu report sync", "collaboration", "feishu:write", "medium"));
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
