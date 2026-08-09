package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.reliability.persistence.OutboxEventMapper;
import com.sirithree.shopops.admin.reliability.persistence.WriteOperationMapper;
import com.sirithree.shopops.admin.reliability.service.IdempotencyConflictException;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

class IdempotencyPayloadConflictTest {
    @Test
    void sameKeyDifferentSemanticPayloadMustBeRejected() {
        @SuppressWarnings("unchecked")
        ObjectProvider<PlatformTransactionManager> provider = mock(ObjectProvider.class);
        WriteOperationService service = new WriteOperationService(mock(WriteOperationMapper.class), mock(OutboxEventMapper.class),
                new JacksonJsonSupport(new ObjectMapper()), provider, "memory");
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L); context.setShopId(1L); context.setUserId(1L);
        service.prepare(context, "order.refund_execute", "ORDER-1", "REQ-1",
                Map.of("orderId", "ORDER-1", "refundAmount", 100, "approvalId", 10));
        assertThatThrownBy(() -> service.prepare(context, "order.refund_execute", "ORDER-1", "REQ-1",
                Map.of("orderId", "ORDER-1", "refundAmount", 101, "approvalId", 20)))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("Input changed for the same idempotency key");
    }
}
