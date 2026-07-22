package com.sirithree.shopops.admin.tool.component;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${shopops.persistence:memory}' == 'jdbc' && '${shopops.bootstrap.legacy-enabled:false}' == 'true'")
public class P0ToolBootstrapRunner implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public P0ToolBootstrapRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTool("order.query_summary", "订单核心指标查询", "order", "order:read", "mock.mall",
                "{\"type\":\"object\",\"required\":[\"shopId\",\"startDate\",\"endDate\"],\"properties\":{\"shopId\":{\"type\":\"integer\"},\"startDate\":{\"type\":\"string\",\"format\":\"date\"},\"endDate\":{\"type\":\"string\",\"format\":\"date\"}}}",
                "{\"type\":\"object\",\"required\":[\"gmv\",\"orderCount\",\"refundAmount\",\"refundRate\",\"avgOrderAmount\"],\"properties\":{\"gmv\":{\"type\":\"number\"},\"orderCount\":{\"type\":\"integer\"},\"refundAmount\":{\"type\":\"number\"},\"refundRate\":{\"type\":\"number\"},\"avgOrderAmount\":{\"type\":\"number\"}}}");
        ensureTool("comment.query_negative", "差评风险查询", "comment", "comment:read", "mock.mall",
                "{\"type\":\"object\",\"required\":[\"shopId\",\"startDate\",\"endDate\"],\"properties\":{\"shopId\":{\"type\":\"integer\"},\"startDate\":{\"type\":\"string\",\"format\":\"date\"},\"endDate\":{\"type\":\"string\",\"format\":\"date\"},\"minStar\":{\"type\":\"integer\",\"default\":3}}}",
                "{\"type\":\"object\",\"required\":[\"negativeCount\",\"riskComments\",\"categoryStats\"],\"properties\":{\"negativeCount\":{\"type\":\"integer\"},\"riskComments\":{\"type\":\"array\"},\"categoryStats\":{\"type\":\"object\"}}}");
        ensureTool("product.query_candidates", "待优化商品查询", "product", "product:read", "mock.mall",
                "{\"type\":\"object\",\"required\":[\"shopId\",\"startDate\",\"endDate\"],\"properties\":{\"shopId\":{\"type\":\"integer\"},\"startDate\":{\"type\":\"string\",\"format\":\"date\"},\"endDate\":{\"type\":\"string\",\"format\":\"date\"},\"limit\":{\"type\":\"integer\",\"default\":10}}}",
                "{\"type\":\"object\",\"required\":[\"candidateCount\",\"products\"],\"properties\":{\"candidateCount\":{\"type\":\"integer\"},\"products\":{\"type\":\"array\"}}}");
        ensureTool("ad.query_performance", "广告投放指标查询", "ad", "ad:read", "file.ad-performance",
                "{\"type\":\"object\",\"required\":[\"shopId\",\"startDate\",\"endDate\"],\"properties\":{\"shopId\":{\"type\":\"integer\"},\"startDate\":{\"type\":\"string\",\"format\":\"date\"},\"endDate\":{\"type\":\"string\",\"format\":\"date\"}}}",
                "{\"type\":\"object\",\"required\":[\"spend\",\"impressions\",\"clicks\",\"ctr\",\"conversionRate\",\"roi\"],\"properties\":{\"spend\":{\"type\":\"number\"},\"impressions\":{\"type\":\"integer\"},\"clicks\":{\"type\":\"integer\"},\"ctr\":{\"type\":\"number\"},\"conversionRate\":{\"type\":\"number\"},\"roi\":{\"type\":\"number\"},\"campaigns\":{\"type\":\"array\"}}}");
        ensureTool("report.query_external_metrics", "外部报表指标查询", "report", "report:read", "file.external-reports",
                "{\"type\":\"object\",\"required\":[\"shopId\",\"startDate\",\"endDate\"],\"properties\":{\"shopId\":{\"type\":\"integer\"},\"startDate\":{\"type\":\"string\",\"format\":\"date\"},\"endDate\":{\"type\":\"string\",\"format\":\"date\"}}}",
                "{\"type\":\"object\",\"required\":[\"visitorCount\",\"conversionRate\",\"repeatPurchaseRate\"],\"properties\":{\"visitorCount\":{\"type\":\"integer\"},\"newVisitorCount\":{\"type\":\"integer\"},\"conversionRate\":{\"type\":\"number\"},\"repeatPurchaseRate\":{\"type\":\"number\"},\"favoriteCount\":{\"type\":\"integer\"},\"cartAddCount\":{\"type\":\"integer\"},\"topChannels\":{\"type\":\"array\"}}}");
        ensureTool("report.generate_daily_review", "每日经营复盘报告生成", "report", "report:generate", "internal.report",
                "{\"type\":\"object\",\"required\":[\"orderSummary\",\"negativeComments\",\"productCandidates\",\"adPerformance\",\"externalReportMetrics\",\"dateRange\"],\"properties\":{\"orderSummary\":{\"type\":\"object\"},\"negativeComments\":{\"type\":\"object\"},\"productCandidates\":{\"type\":\"object\"},\"adPerformance\":{\"type\":\"object\"},\"externalReportMetrics\":{\"type\":\"object\"},\"dateRange\":{\"type\":\"object\"}}}",
                "{\"type\":\"object\",\"required\":[\"title\",\"markdown\",\"summary\",\"evidence\"],\"properties\":{\"title\":{\"type\":\"string\"},\"markdown\":{\"type\":\"string\"},\"summary\":{\"type\":\"string\"},\"evidence\":{\"type\":\"object\"}}}");
    }

    private void ensureTool(String toolCode,
                            String toolName,
                            String category,
                            String permissionCode,
                            String connectorCode,
                            String inputSchema,
                            String outputSchema) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mcp_tool WHERE tool_code = ? AND version = '1.0.0'",
                Integer.class,
                toolCode
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO mcp_tool (
                  tenant_id, tool_code, tool_name, category, description,
                  input_schema, output_schema, permission_code, risk_level,
                  need_approval, idempotent, timeout_ms, retry_count,
                  connector_code, enabled, version, owner, created_at, updated_at
                ) VALUES (
                  NULL, ?, ?, ?, ?,
                  CAST(? AS JSON), CAST(? AS JSON), ?, 'low',
                  0, 1, 10000, 0,
                  ?, 1, '1.0.0', 'platform', NOW(), NOW()
                )
                """,
                toolCode,
                toolName,
                category,
                toolName,
                inputSchema,
                outputSchema,
                permissionCode,
                connectorCode
        );
    }
}
