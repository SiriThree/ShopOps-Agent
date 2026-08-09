package com.sirithree.shopops.admin.benchmark.v1.formal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.BusinessOrderMapper;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolGovernanceException;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.service.impl.JdbcRefundOrderBusinessScopeValidator;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JdbcRefundOrderBusinessScopeValidatorTest {
    @Test void rejectsOrderOutsideTrustedTenantShopScope() {
        BusinessOrderMapper mapper=mock(BusinessOrderMapper.class);
        when(mapper.countByOrderNoAndScope(1L,1L,"SO-OTHER-SHOP")).thenReturn(0);
        JdbcRefundOrderBusinessScopeValidator validator=validator(mapper);
        assertThatThrownBy(() -> validator.validate(context(),tool(),Map.of("orderId","SO-OTHER-SHOP","refundAmount",100)))
                .isInstanceOf(ToolGovernanceException.class)
                .hasMessageContaining("not owned by the trusted tenant/shop scope");
    }

    @Test void rejectsRefundAmountAboveRemainingOrderValue() {
        BusinessOrderMapper mapper=mock(BusinessOrderMapper.class);
        when(mapper.countByOrderNoAndScope(1L,1L,"SO-1")).thenReturn(1);
        when(mapper.queryRemainingRefundableAmount(1L,1L,"SO-1")).thenReturn(new BigDecimal("78.00"));
        JdbcRefundOrderBusinessScopeValidator validator=validator(mapper);
        assertThatThrownBy(() -> validator.validate(context(),tool(),Map.of("orderId","SO-1","refundAmount",100)))
                .isInstanceOf(ToolGovernanceException.class)
                .hasMessageContaining("remaining refundable amount");
    }

    @Test void allowsOwnedOrderWithinRemainingRefundableValue() {
        BusinessOrderMapper mapper=mock(BusinessOrderMapper.class);
        when(mapper.countByOrderNoAndScope(1L,1L,"SO-1")).thenReturn(1);
        when(mapper.queryRemainingRefundableAmount(1L,1L,"SO-1")).thenReturn(new BigDecimal("178.00"));
        JdbcRefundOrderBusinessScopeValidator validator=validator(mapper);
        assertThatCode(() -> validator.validate(context(),tool(),Map.of("orderId","SO-1","refundAmount",100)))
                .doesNotThrowAnyException();
    }

    private JdbcRefundOrderBusinessScopeValidator validator(BusinessOrderMapper mapper) {
        return new JdbcRefundOrderBusinessScopeValidator(mapper,
                new JacksonJsonSupport(new com.fasterxml.jackson.databind.ObjectMapper()));
    }

    private ToolInvokeContext context() {
        ToolInvokeContext context=new ToolInvokeContext();
        context.setTenantId(1L); context.setShopId(1L); context.setUserId(2L);
        return context;
    }

    private McpToolDto tool() {
        return new McpToolDto("order.refund_execute", "Refund Execute", "order", "order:refund", "HIGH");
    }
}
