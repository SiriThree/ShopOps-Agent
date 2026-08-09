package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.reliability.persistence.OutboxEventMapper;
import com.sirithree.shopops.admin.reliability.persistence.WriteOperationMapper;
import com.sirithree.shopops.admin.reliability.service.WriteOperationService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

class LogicalOperationIdentityTest {
    @Test
    void logicalIdentityMustBindToolScopeTargetAndOperationRequestId() {
        WriteOperationService service = service();
        ToolInvokeContext context = context();
        assertThat(service.idempotencyKey("order.refund_execute", context, "ORDER-1", "REQ-1"))
                .isEqualTo("order.refund_execute:1:1:ORDER-1:REQ-1");
    }

    @Test
    void semanticPayloadHashMustIgnoreApprovalExecutionMetadata() {
        WriteOperationService service = service();
        Map<String, Object> first = new LinkedHashMap<>(Map.of("orderId", "ORDER-1", "refundAmount", 100, "approvalId", 10));
        Map<String, Object> replay = new LinkedHashMap<>(Map.of("orderId", "ORDER-1", "refundAmount", 100, "approvalId", 20));
        assertThat(service.inputHash(first)).isEqualTo(service.inputHash(replay));
    }

    private WriteOperationService service() {
        @SuppressWarnings("unchecked")
        ObjectProvider<PlatformTransactionManager> provider = mock(ObjectProvider.class);
        return new WriteOperationService(mock(WriteOperationMapper.class), mock(OutboxEventMapper.class),
                new JacksonJsonSupport(new ObjectMapper()), provider, "memory");
    }

    private ToolInvokeContext context() {
        ToolInvokeContext c = new ToolInvokeContext();
        c.setTenantId(1L); c.setShopId(1L); c.setUserId(1L); c.setTraceId("trace");
        return c;
    }
}
