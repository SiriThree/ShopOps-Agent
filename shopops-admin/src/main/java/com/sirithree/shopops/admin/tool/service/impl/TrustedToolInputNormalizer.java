package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolGovernanceException;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TrustedToolInputNormalizer {
    private final JacksonJsonSupport jsonSupport;

    public TrustedToolInputNormalizer(JacksonJsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    public Object normalize(ToolInvokeContext context, McpToolDto tool, Object input) {
        Map<String, Object> values = input == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(jsonSupport.toMap(jsonSupport.toJson(input)));
        validateIdentityArguments(context, values);
        rejectForgedAuthorizationArguments(values);

        if (!CommerceMcpContracts.PROVIDER_MCP.equalsIgnoreCase(tool.getProviderType())) {
            return input;
        }
        values.put("shopId", requiredPositive(context.getShopId(), "trusted shopId"));
        return values;
    }

    private void validateIdentityArguments(ToolInvokeContext context, Map<String, Object> values) {
        validateIdentityField("tenantId", context.getTenantId(), values.get("tenantId"));
        validateIdentityField("shopId", context.getShopId(), values.get("shopId"));
        validateIdentityField("userId", context.getUserId(), values.get("userId"));
    }

    private void validateIdentityField(String field, Long trustedValue, Object candidate) {
        if (candidate == null) return;
        long provided = positiveLong(candidate, field);
        long trusted = requiredPositive(trustedValue, "trusted " + field);
        if (provided != trusted) {
            String code = "shopId".equals(field) ? "TOOL_SCOPE_MISMATCH" : "TOOL_IDENTITY_ARGUMENT_CONFLICT";
            throw new ToolGovernanceException(code,
                    "Tool arguments cannot override trusted " + field);
        }
    }

    private void rejectForgedAuthorizationArguments(Map<String, Object> values) {
        if (values.containsKey("permissions") || values.containsKey("roles")) {
            throw new ToolGovernanceException("TOOL_AUTHORIZATION_ARGUMENT_FORBIDDEN",
                    "Tool arguments cannot supply roles or permissions");
        }
    }

    private long positiveLong(Object candidate, String field) {
        long provided;
        if (candidate instanceof Number number) {
            provided = number.longValue();
        } else {
            try {
                provided = Long.parseLong(String.valueOf(candidate));
            } catch (NumberFormatException ex) {
                throw new ToolGovernanceException("TOOL_SCOPE_INVALID", field + " must be a positive integer");
            }
        }
        if (provided <= 0) {
            throw new ToolGovernanceException("TOOL_SCOPE_INVALID", field + " must be a positive integer");
        }
        return provided;
    }

    private long requiredPositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new ToolGovernanceException("TOOL_TRUSTED_CONTEXT_MISSING", field + " is required");
        }
        return value;
    }
}
