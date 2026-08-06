package com.sirithree.shopops.mcp.commerce.tool;

import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import com.sirithree.shopops.mcp.commerce.service.NegativeCommentQueryService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CommentQueryNegativeMcpTool {
    private final NegativeCommentQueryService queryService;

    public CommentQueryNegativeMcpTool(NegativeCommentQueryService queryService) {
        this.queryService = queryService;
    }

    public SyncToolSpecification specification() {
        Tool tool = Tool.builder(
                        CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                        CommerceMcpContracts.commentQueryNegativeInputSchema())
                .description("Query low-star and high-risk comments from the independent Commerce state")
                .annotations(ToolAnnotations.builder()
                        .readOnlyHint(true)
                        .destructiveHint(false)
                        .idempotentHint(true)
                        .openWorldHint(false)
                        .build())
                .outputSchema(CommerceMcpContracts.commentQueryNegativeOutputSchema())
                .build();

        return SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> arguments = request.arguments();
                        long trustedTenantId = requiredContextLong(exchange.transportContext().get("tenantId"), "tenantId");
                        long trustedShopId = requiredContextLong(exchange.transportContext().get("shopId"), "shopId");
                        long requestedShopId = requiredLong(arguments.get("shopId"), "shopId");
                        if (requestedShopId != trustedShopId) {
                            return businessError("MCP_SCOPE_MISMATCH", "shopId conflicts with trusted ShopOps scope");
                        }
                        LocalDate startDate = LocalDate.parse(requiredText(arguments.get("startDate"), "startDate"));
                        LocalDate endDate = LocalDate.parse(requiredText(arguments.get("endDate"), "endDate"));
                        int maxStar = optionalInt(arguments.get("minStar"), 3);
                        Map<String, Object> result = queryService.query(
                                trustedTenantId, trustedShopId, startDate, endDate, maxStar);
                        return CallToolResult.builder()
                                .content(List.of(TextContent.builder(CommerceMcpContracts.canonicalJson(result)).build()))
                                .structuredContent(result)
                                .build();
                    } catch (RuntimeException ex) {
                        return businessError("MCP_COMMENT_QUERY_INVALID", ex.getMessage());
                    }
                })
                .build();
    }

    private CallToolResult businessError(String code, String message) {
        return CallToolResult.builder()
                .content(List.of(TextContent.builder(code + ": " + message).build()))
                .isError(true)
                .structuredContent(Map.of("errorCode", code, "errorMessage", message))
                .build();
    }

    private long requiredContextLong(Object value, String field) {
        if (value instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Missing trusted context: " + field);
    }

    private long requiredLong(Object value, String field) {
        if (value instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        try {
            long parsed = Long.parseLong(String.valueOf(value));
            if (parsed > 0) {
                return parsed;
            }
        } catch (RuntimeException ignored) {
        }
        throw new IllegalArgumentException(field + " must be a positive integer");
    }

    private int optionalInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String requiredText(Object value, String field) {
        String text = value == null ? null : String.valueOf(value);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return text;
    }
}
