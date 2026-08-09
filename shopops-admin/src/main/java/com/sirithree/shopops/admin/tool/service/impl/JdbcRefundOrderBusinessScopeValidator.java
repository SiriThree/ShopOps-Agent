package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.BusinessOrderMapper;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolGovernanceException;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.service.ToolBusinessScopeValidator;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** JDBC-only object ownership validation for the refund write boundary. */
@Component
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcRefundOrderBusinessScopeValidator implements ToolBusinessScopeValidator {
    private final BusinessOrderMapper orderMapper;
    private final JacksonJsonSupport jsonSupport;

    public JdbcRefundOrderBusinessScopeValidator(BusinessOrderMapper orderMapper, JacksonJsonSupport jsonSupport) {
        this.orderMapper = orderMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public boolean supports(String toolCode) {
        return "order.refund_execute".equals(toolCode);
    }

    @Override
    public void validate(ToolInvokeContext context, McpToolDto tool, Object normalizedInput) {
        Map<String, Object> values = jsonSupport.toMap(jsonSupport.toJson(normalizedInput));
        Object rawOrderId = values.get("orderId");
        String orderId = rawOrderId == null ? null : String.valueOf(rawOrderId).trim();
        if (orderId == null || orderId.isBlank()) {
            throw new ToolGovernanceException("BUSINESS_SCOPE_VIOLATION", "Refund target orderId is required");
        }
        if (orderMapper.countByOrderNoAndScope(context.getTenantId(), context.getShopId(), orderId) != 1) {
            throw new ToolGovernanceException("BUSINESS_SCOPE_VIOLATION",
                    "Refund target order is not owned by the trusted tenant/shop scope");
        }
        BigDecimal remaining = orderMapper.queryRemainingRefundableAmount(
                context.getTenantId(), context.getShopId(), orderId);
        BigDecimal requested = decimal(values.get("refundAmount"));
        if (remaining == null || requested == null || requested.signum() <= 0 || requested.compareTo(remaining) > 0) {
            throw new ToolGovernanceException("BUSINESS_SCOPE_VIOLATION",
                    "Refund amount exceeds the trusted order's remaining refundable amount");
        }
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
