package com.sirithree.shopops.common.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure protocol contract shared by ShopOps Admin and the independent Commerce MCP server.
 * It intentionally contains no Admin service, persistence, workflow or approval types.
 */
public final class CommerceMcpContracts {
    public static final String SERVER_CODE = "commerce-default";
    public static final String COMMENT_QUERY_NEGATIVE = "comment.query_negative";
    public static final String PROVIDER_LOCAL = "LOCAL";
    public static final String PROVIDER_MCP = "MCP";
    public static final String DISCOVERY_READY = "READY";
    public static final String DISCOVERY_SCHEMA_DRIFT = "SCHEMA_DRIFT";
    public static final String HEADER_TENANT_ID = "X-ShopOps-Tenant-Id";
    public static final String HEADER_SHOP_ID = "X-ShopOps-Shop-Id";
    public static final String HEADER_USER_ID = "X-ShopOps-User-Id";
    public static final String HEADER_TASK_ID = "X-ShopOps-Task-Id";
    public static final String HEADER_STEP_ID = "X-ShopOps-Step-Id";
    public static final String HEADER_TRACE_ID = "X-ShopOps-Trace-Id";
    public static final String HEADER_APPROVAL_ID = "X-ShopOps-Approval-Id";

    private static final ObjectMapper CANONICAL_MAPPER = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private CommerceMcpContracts() {
    }

    public static Map<String, Object> commentQueryNegativeInputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("shopId", Map.of(
                "type", "integer",
                "minimum", 1,
                "description", "Trusted shop scope injected by ShopOps Tool Gateway"
        ));
        properties.put("startDate", Map.of(
                "type", "string",
                "format", "date"
        ));
        properties.put("endDate", Map.of(
                "type", "string",
                "format", "date"
        ));
        properties.put("minStar", Map.of(
                "type", "integer",
                "minimum", 1,
                "maximum", 5,
                "default", 3
        ));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("shopId", "startDate", "endDate"));
        schema.put("additionalProperties", false);
        return schema;
    }

    public static Map<String, Object> commentQueryNegativeOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "negativeCount", Map.of("type", "integer", "minimum", 0),
                "riskComments", Map.of("type", "array", "items", Map.of("type", "object")),
                "categoryStats", Map.of("type", "object", "additionalProperties", Map.of("type", "integer")),
                "scope", Map.of("type", "object")
        ));
        schema.put("required", List.of("negativeCount", "riskComments", "categoryStats", "scope"));
        schema.put("additionalProperties", false);
        return schema;
    }

    public static String commentQueryNegativeSchemaHash() {
        return sha256(canonicalJson(commentQueryNegativeInputSchema()));
    }

    public static String canonicalJson(Object value) {
        try {
            return CANONICAL_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Cannot serialize MCP contract", ex);
        }
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

}
