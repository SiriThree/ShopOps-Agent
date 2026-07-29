package com.sirithree.shopops.admin.mcp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpProtocolController {
    private static final String JSON_RPC_VERSION = "2.0";
    private static final String PROTOCOL_VERSION = "2026-07-28";
    private static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";

    private final McpToolService mcpToolService;
    private final ToolGatewayService toolGatewayService;
    private final ObjectMapper objectMapper;

    public McpProtocolController(McpToolService mcpToolService,
                                 ToolGatewayService toolGatewayService,
                                 ObjectMapper objectMapper) {
        this.mcpToolService = mcpToolService;
        this.toolGatewayService = toolGatewayService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/mcp")
    public ResponseEntity<Map<String, Object>> handle(@RequestBody Map<String, Object> request,
                                                      @RequestHeader HttpHeaders headers) {
        Object id = request.get("id");
        String method = stringValue(request.get("method"));
        try {
            Object result = switch (method) {
                case "server/discover" -> discover();
                case "initialize" -> initialize();
                case "tools/list" -> listTools();
                case "tools/call" -> callTool(params(request.get("params")));
                default -> throw new McpProtocolException(-32601, "Method not found: " + method);
            };
            return response(ok(id, result));
        } catch (McpProtocolException ex) {
            return response(error(id, ex.code(), ex.getMessage(), ex.data()));
        } catch (RuntimeException ex) {
            return response(error(id, -32603, "Internal error: " + ex.getMessage(), null));
        }
    }

    private ResponseEntity<Map<String, Object>> response(Map<String, Object> body) {
        return ResponseEntity.ok()
                .header(MCP_PROTOCOL_VERSION_HEADER, PROTOCOL_VERSION)
                .body(body);
    }

    private Map<String, Object> discover() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersions", List.of(PROTOCOL_VERSION, "2025-06-18"));
        result.put("serverInfo", serverInfo());
        result.put("capabilities", capabilities());
        return result;
    }

    private Map<String, Object> initialize() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("serverInfo", serverInfo());
        result.put("capabilities", capabilities());
        return result;
    }

    private Map<String, Object> serverInfo() {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("name", "shopops-mcp-server");
        server.put("title", "ShopOps MCP Server");
        server.put("version", "0.1.0");
        return server;
    }

    private Map<String, Object> capabilities() {
        Map<String, Object> tools = new LinkedHashMap<>();
        tools.put("listChanged", false);
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", tools);
        return capabilities;
    }

    private Map<String, Object> listTools() {
        RequestContext context = RequestContextHolder.current();
        List<Map<String, Object>> tools = mcpToolService.listTools(context.getTenantId()).stream()
                .map(this::toolDescriptor)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tools", tools);
        return result;
    }

    private Map<String, Object> toolDescriptor(McpToolDto tool) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("name", tool.getToolCode());
        descriptor.put("title", tool.getToolName());
        descriptor.put("description", toolDescription(tool));
        descriptor.put("inputSchema", inputSchema(tool));
        descriptor.put("annotations", toolAnnotations(tool));
        descriptor.put("_meta", Map.of(
                "shopops/category", valueOrDash(tool.getCategory()),
                "shopops/permissionCode", valueOrDash(tool.getPermissionCode()),
                "shopops/riskLevel", valueOrDash(tool.getRiskLevel()),
                "shopops/needApproval", Boolean.TRUE.equals(tool.getNeedApproval()),
                "shopops/version", valueOrDash(tool.getVersion())
        ));
        return descriptor;
    }

    private String toolDescription(McpToolDto tool) {
        return "%s. Category=%s, permission=%s, risk=%s, approvalRequired=%s."
                .formatted(
                        valueOrDash(tool.getToolName()),
                        valueOrDash(tool.getCategory()),
                        valueOrDash(tool.getPermissionCode()),
                        valueOrDash(tool.getRiskLevel()),
                        Boolean.TRUE.equals(tool.getNeedApproval())
                );
    }

    private Map<String, Object> inputSchema(McpToolDto tool) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("shopId", Map.of("type", "integer", "description", "Shop identifier. Defaults to request context shop."));
        properties.put("tenantId", Map.of("type", "integer", "description", "Tenant identifier. Defaults to request context tenant."));
        properties.put("approvalId", Map.of("type", "integer", "description", "Approval id for retrying approved high-risk tool calls."));
        properties.put("dateRange", Map.of(
                "type", "object",
                "description", "Optional date range for analytical tools.",
                "additionalProperties", true
        ));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("additionalProperties", true);
        schema.put("description", "Input object for ShopOps tool " + tool.getToolCode() + ".");
        return schema;
    }

    private Map<String, Object> toolAnnotations(McpToolDto tool) {
        String permissionCode = valueOrDash(tool.getPermissionCode()).toLowerCase();
        String riskLevel = valueOrDash(tool.getRiskLevel()).toLowerCase();
        boolean write = permissionCode.contains(":write") || riskLevel.equals("high");
        Map<String, Object> annotations = new LinkedHashMap<>();
        annotations.put("readOnlyHint", !write);
        annotations.put("destructiveHint", riskLevel.equals("high"));
        annotations.put("idempotentHint", !write);
        annotations.put("openWorldHint", false);
        return annotations;
    }

    private Map<String, Object> callTool(Map<String, Object> params) {
        String toolName = stringValue(params.get("name"));
        if (toolName == null || toolName.isBlank()) {
            throw new McpProtocolException(-32602, "Missing required params.name");
        }
        Map<String, Object> arguments = params(params.get("arguments"));
        RequestContext requestContext = RequestContextHolder.current();
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(requestContext.getTenantId());
        context.setShopId(requestContext.getShopId());
        context.setUserId(requestContext.getUserId());
        context.setTraceId("tr_mcp_" + UUID.randomUUID().toString().replace("-", ""));
        context.setApprovalId(longValue(arguments.get("approvalId")));
        context.setManualInvoke(true);

        ToolInvokeResult invokeResult = toolGatewayService.invoke(context, toolName, arguments);
        Map<String, Object> structured = objectMapper.convertValue(invokeResult, Map.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of(Map.of(
                "type", "text",
                "text", toolCallText(invokeResult)
        )));
        result.put("structuredContent", structured);
        result.put("isError", !Boolean.TRUE.equals(invokeResult.getSuccess()));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("shopops/toolCallLogId", invokeResult.getToolCallLogId());
        meta.put("shopops/approvalId", invokeResult.getApprovalId());
        meta.put("shopops/status", valueOrDash(invokeResult.getStatus()));
        result.put("_meta", meta);
        return result;
    }

    private String toolCallText(ToolInvokeResult result) {
        if (Boolean.TRUE.equals(result.getSuccess())) {
            try {
                return objectMapper.writeValueAsString(result.getData());
            } catch (JsonProcessingException ex) {
                return String.valueOf(result.getData());
            }
        }
        return valueOrDash(result.getErrorCode()) + ": " + valueOrDash(result.getErrorMessage());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> params(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new McpProtocolException(-32602, "params must be an object");
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> ok(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> error(Object id, int code, String message, Object data) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.put("data", data);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSON_RPC_VERSION);
        response.put("id", id);
        response.put("error", error);
        return response;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String valueOrDash(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value);
    }

    private static class McpProtocolException extends RuntimeException {
        private final int code;
        private final Object data;

        private McpProtocolException(int code, String message) {
            this(code, message, null);
        }

        private McpProtocolException(int code, String message, Object data) {
            super(message);
            this.code = code;
            this.data = data;
        }

        private int code() {
            return code;
        }

        private Object data() {
            return data;
        }
    }
}
