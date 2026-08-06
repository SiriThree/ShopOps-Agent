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
        if (!CommerceMcpContracts.PROVIDER_MCP.equalsIgnoreCase(tool.getProviderType())) {
            validateProvidedShopScope(context, input);
            return input;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (input != null) {
            normalized.putAll(jsonSupport.toMap(jsonSupport.toJson(input)));
        }
        validateShopScope(context, normalized.get("shopId"));
        normalized.put("shopId", requiredPositive(context.getShopId(), "trusted shopId"));
        return normalized;
    }

    private void validateProvidedShopScope(ToolInvokeContext context, Object input) {
        if (input == null) {
            return;
        }
        Map<String, Object> values = jsonSupport.toMap(jsonSupport.toJson(input));
        validateShopScope(context, values.get("shopId"));
    }

    private void validateShopScope(ToolInvokeContext context, Object candidate) {
        if (candidate == null) {
            return;
        }
        long provided;
        if (candidate instanceof Number number) {
            provided = number.longValue();
        } else {
            try {
                provided = Long.parseLong(String.valueOf(candidate));
            } catch (NumberFormatException ex) {
                throw new ToolGovernanceException("TOOL_SCOPE_INVALID", "shopId must be a positive integer");
            }
        }
        long trusted = requiredPositive(context.getShopId(), "trusted shopId");
        if (provided != trusted) {
            throw new ToolGovernanceException("TOOL_SCOPE_MISMATCH",
                    "Tool arguments cannot override trusted shop scope");
        }
    }

    private long requiredPositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new ToolGovernanceException("TOOL_TRUSTED_CONTEXT_MISSING", field + " is required");
        }
        return value;
    }
}
