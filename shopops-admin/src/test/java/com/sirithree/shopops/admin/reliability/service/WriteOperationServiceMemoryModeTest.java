package com.sirithree.shopops.admin.reliability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import com.sirithree.shopops.admin.reliability.persistence.OutboxEventMapper;
import com.sirithree.shopops.admin.reliability.persistence.WriteOperationMapper;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

class WriteOperationServiceMemoryModeTest {
    @Test
    void shouldCompleteMemoryWriteStateMachineWithoutCreatingJdbcTransaction() {
        WriteOperationMapper mapper = mock(WriteOperationMapper.class);
        OutboxEventMapper outbox = mock(OutboxEventMapper.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<PlatformTransactionManager> transactionManagers = mock(ObjectProvider.class);
        WriteOperationService service = new WriteOperationService(
                mapper,
                outbox,
                new JacksonJsonSupport(new ObjectMapper()),
                transactionManagers,
                "memory");

        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(1L);
        context.setTaskId(10L);
        context.setTraceId("trace-memory-write");

        Map<String, Object> input = Map.of(
                "shopId", 1,
                "orderId", "ORDER-1",
                "refundAmount", 1288,
                "operationRequestId", "REQ-1");
        WriteOperation prepared = service.prepare(
                context,
                "order.refund_execute",
                "ORDER-1",
                "REQ-1",
                input);
        String preparedStatus = prepared.getStatus();
        Integer preparedVersion = prepared.getVersion();
        WriteOperation completed = service.externalSucceeded(
                prepared,
                "REF-REQ-1",
                Map.of("status", "SUCCEEDED"));
        WriteOperation replayed = service.prepare(
                context,
                "order.refund_execute",
                "ORDER-1",
                "REQ-1",
                input);

        assertThat(preparedStatus).isEqualTo(WriteOperationStatus.EXECUTING);
        assertThat(preparedVersion).isEqualTo(1);
        assertThat(completed.getStatus()).isEqualTo(WriteOperationStatus.SUCCEEDED);
        assertThat(completed.getVersion()).isEqualTo(4);
        assertThat(completed.getExternalReference()).isEqualTo("REF-REQ-1");
        assertThat(completed.isFreshExecution()).isFalse();
        assertThat(replayed).isNotSameAs(completed);
        assertThat(replayed.getId()).isEqualTo(completed.getId());
        assertThat(replayed.getIdempotencyKey()).isEqualTo(completed.getIdempotencyKey());
        assertThat(replayed.isFreshExecution()).isFalse();
        verifyNoInteractions(transactionManagers, mapper, outbox);
    }

    @Test
    void shouldRejectChangedInputForSameMemoryIdempotencyKeyWithoutJdbcAccess() {
        WriteOperationMapper mapper = mock(WriteOperationMapper.class);
        OutboxEventMapper outbox = mock(OutboxEventMapper.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<PlatformTransactionManager> transactionManagers = mock(ObjectProvider.class);
        WriteOperationService service = new WriteOperationService(
                mapper,
                outbox,
                new JacksonJsonSupport(new ObjectMapper()),
                transactionManagers,
                "memory");

        ToolInvokeContext context = trustedContext();
        service.prepare(
                context,
                "order.refund_execute",
                "ORDER-1",
                "REQ-1",
                Map.of(
                        "shopId", 1,
                        "orderId", "ORDER-1",
                        "refundAmount", 1288,
                        "operationRequestId", "REQ-1"));

        assertThatThrownBy(() -> service.prepare(
                context,
                "order.refund_execute",
                "ORDER-1",
                "REQ-1",
                Map.of(
                        "shopId", 1,
                        "orderId", "ORDER-1",
                        "refundAmount", 9999,
                        "operationRequestId", "REQ-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Input changed for the same idempotency key");

        verifyNoInteractions(transactionManagers, mapper, outbox);
    }

    @Test
    void shouldAllowOnlyOneFreshMemoryExecutionUnderConcurrentPrepare() throws Exception {
        WriteOperationMapper mapper = mock(WriteOperationMapper.class);
        OutboxEventMapper outbox = mock(OutboxEventMapper.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<PlatformTransactionManager> transactionManagers = mock(ObjectProvider.class);
        WriteOperationService service = new WriteOperationService(
                mapper,
                outbox,
                new JacksonJsonSupport(new ObjectMapper()),
                transactionManagers,
                "memory");

        ToolInvokeContext context = trustedContext();
        Map<String, Object> input = Map.of(
                "shopId", 1,
                "orderId", "ORDER-CONCURRENT",
                "refundAmount", 1288,
                "operationRequestId", "REQ-CONCURRENT");
        int workers = 5;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<WriteOperation>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return service.prepare(context, "order.refund_execute", "ORDER-CONCURRENT", "REQ-CONCURRENT", input);
                }));
            }
            ready.await();
            start.countDown();
            List<WriteOperation> results = new ArrayList<>();
            for (Future<WriteOperation> future : futures) results.add(future.get());

            assertThat(results).filteredOn(WriteOperation::isFreshExecution).hasSize(1);
            assertThat(results).extracting(WriteOperation::getId).containsOnly(results.get(0).getId());
            assertThat(service.listByTaskId(1L, 1L, 10L)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        verifyNoInteractions(transactionManagers, mapper, outbox);
    }

    @Test
    void shouldIgnoreApprovalIdWhenBindingSemanticIdempotencyPayload() {
        WriteOperationMapper mapper = mock(WriteOperationMapper.class);
        OutboxEventMapper outbox = mock(OutboxEventMapper.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<PlatformTransactionManager> transactionManagers = mock(ObjectProvider.class);
        WriteOperationService service = new WriteOperationService(
                mapper, outbox, new JacksonJsonSupport(new ObjectMapper()), transactionManagers, "memory");
        ToolInvokeContext context = trustedContext();

        Map<String, Object> first = Map.of(
                "shopId", 1, "orderId", "ORDER-1", "refundAmount", 1288,
                "operationRequestId", "REQ-APPROVAL", "approvalId", 100L);
        Map<String, Object> replay = Map.of(
                "shopId", 1, "orderId", "ORDER-1", "refundAmount", 1288,
                "operationRequestId", "REQ-APPROVAL", "approvalId", 200L);

        WriteOperation created = service.prepare(context, "order.refund_execute", "ORDER-1", "REQ-APPROVAL", first);
        WriteOperation repeated = service.prepare(context, "order.refund_execute", "ORDER-1", "REQ-APPROVAL", replay);

        assertThat(created.isFreshExecution()).isTrue();
        assertThat(repeated.isFreshExecution()).isFalse();
        assertThat(repeated.getId()).isEqualTo(created.getId());
    }

    private ToolInvokeContext trustedContext() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(1L);
        context.setTaskId(10L);
        context.setTraceId("trace-memory-write");
        return context;
    }
}
