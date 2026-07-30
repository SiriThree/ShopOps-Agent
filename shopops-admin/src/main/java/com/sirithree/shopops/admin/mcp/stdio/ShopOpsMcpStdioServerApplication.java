package com.sirithree.shopops.admin.mcp.stdio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.ShopOpsAdminApplication;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.mcp.service.McpProtocolService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class ShopOpsMcpStdioServerApplication {
    private static final String JSON_RPC_VERSION = "2.0";

    public static void main(String[] args) throws IOException {
        PrintStream protocolOut = System.out;
        System.setOut(System.err);

        ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(ShopOpsAdminApplication.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .properties(defaultProperties())
                .run(args);
        try {
            McpProtocolService protocolService = applicationContext.getBean(McpProtocolService.class);
            ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
            new ShopOpsMcpStdioServerApplication(protocolService, objectMapper)
                    .run(System.in, protocolOut);
        } finally {
            applicationContext.close();
        }
    }

    private static Map<String, Object> defaultProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.main.web-application-type", "none");
        properties.put("spring.main.banner-mode", "off");
        properties.put("server.port", "0");
        properties.put("shopops.persistence", "memory");
        properties.put("shopops.flyway.enabled", "false");
        properties.put("spring.flyway.enabled", "false");
        return properties;
    }

    private final McpProtocolService protocolService;
    private final ObjectMapper objectMapper;

    public ShopOpsMcpStdioServerApplication(McpProtocolService protocolService, ObjectMapper objectMapper) {
        this.protocolService = protocolService;
        this.objectMapper = objectMapper;
    }

    public void run(InputStream inputStream, PrintStream outputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            Map<String, Object> response = handleLine(line);
            if (response != null) {
                writer.println(objectMapper.writeValueAsString(response));
                writer.flush();
            }
        }
    }

    private Map<String, Object> handleLine(String line) {
        Map<String, Object> request;
        try {
            request = readRequest(line);
        } catch (RuntimeException ex) {
            return error(null, -32700, "Parse error: " + ex.getMessage());
        }
        if (!request.containsKey("id")) {
            return null;
        }

        RequestContext context = requestContext(request);
        try {
            RequestContextHolder.set(context);
            MDC.put("tenantId", String.valueOf(context.getTenantId()));
            MDC.put("shopId", String.valueOf(context.getShopId()));
            MDC.put("userId", String.valueOf(context.getUserId()));
            MDC.put("requestId", context.getRequestId());
            return protocolService.handle(request);
        } finally {
            RequestContextHolder.clear();
            MDC.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readRequest(String line) {
        try {
            Object parsed = objectMapper.readValue(line, Object.class);
            if (parsed instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            throw new IllegalArgumentException("JSON-RPC message must be an object");
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(ex.getOriginalMessage());
        }
    }

    private RequestContext requestContext(Map<String, Object> request) {
        Map<String, Object> meta = requestMeta(request);
        Long tenantId = longValue(firstNonNull(meta.get("shopops/tenantId"), System.getenv("SHOPOPS_MCP_TENANT_ID")), 1L);
        Long shopId = longValue(firstNonNull(meta.get("shopops/shopId"), System.getenv("SHOPOPS_MCP_SHOP_ID")), 1L);
        Long userId = longValue(firstNonNull(meta.get("shopops/userId"), System.getenv("SHOPOPS_MCP_USER_ID")), 1L);
        String username = stringValue(firstNonNull(meta.get("shopops/username"), System.getenv("SHOPOPS_MCP_USERNAME")), "mcp-stdio");
        String rolesValue = stringValue(firstNonNull(meta.get("shopops/roles"), System.getenv("SHOPOPS_MCP_ROLES")), "ADMIN");
        List<String> roles = List.of(rolesValue.split(",")).stream()
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();
        String requestId = "req_mcp_stdio_" + UUID.randomUUID().toString().replace("-", "");
        return new RequestContext(tenantId, shopId, userId, requestId, username, roles, "MCP_STDIO", true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestMeta(Map<String, Object> request) {
        Object paramsValue = request.get("params");
        if (paramsValue instanceof Map<?, ?> params) {
            Object metaValue = params.get("_meta");
            if (metaValue instanceof Map<?, ?> meta) {
                return (Map<String, Object>) meta;
            }
        }
        return Map.of();
    }

    private Object firstNonNull(Object first, Object second) {
        return first == null ? second : first;
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private Long longValue(Object value, Long defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id);
        response.put("error", error);
        return response;
    }
}
