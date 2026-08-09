package com.sirithree.shopops.admin.reliability.service;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import com.sirithree.shopops.admin.reliability.persistence.OutboxEventMapper;
import com.sirithree.shopops.admin.reliability.persistence.WriteOperationMapper;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WriteOperationService {
    private final WriteOperationMapper mapper;
    private final OutboxEventMapper outbox;
    private final JacksonJsonSupport json;
    private final boolean jdbcPersistence;
    private final TransactionTemplate jdbcTransactionTemplate;
    private final AtomicLong memoryIds = new AtomicLong(1);
    private final Map<String, WriteOperation> memoryByKey = new ConcurrentHashMap<>();

    public WriteOperationService(WriteOperationMapper mapper,
                                 OutboxEventMapper outbox,
                                 JacksonJsonSupport json,
                                 ObjectProvider<PlatformTransactionManager> transactionManagerProvider,
                                 @Value("${shopops.persistence:memory}") String persistence) {
        this.mapper = mapper;
        this.outbox = outbox;
        this.json = json;
        this.jdbcPersistence = "jdbc".equalsIgnoreCase(persistence);
        this.jdbcTransactionTemplate = jdbcPersistence
                ? new TransactionTemplate(transactionManagerProvider.getIfAvailable(() -> {
                    throw new IllegalStateException("JDBC persistence requires a PlatformTransactionManager");
                }))
                : null;
    }

    public String inputHash(Object input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonicalSemanticInput(input).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate write operation input hash", ex);
        }
    }

    public String idempotencyKey(String toolCode, ToolInvokeContext context, String objectId, String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("operationRequestId is required for write operations");
        }
        return toolCode + ":" + context.getTenantId() + ":" + context.getShopId() + ":" + objectId + ":" + requestId.trim();
    }

    public WriteOperation prepare(ToolInvokeContext context, String toolCode, String objectId, String requestId, Object input) {
        if (!jdbcPersistence) {
            return prepareMemory(context, toolCode, objectId, requestId, input);
        }
        return inJdbcTransaction(() -> prepareJdbc(context, toolCode, objectId, requestId, input));
    }

    public WriteOperation externalSucceeded(WriteOperation operation, String externalReference, Object result) {
        if (!jdbcPersistence) {
            return externalSucceededMemory(operation, externalReference, result);
        }
        return inJdbcTransaction(() -> externalSucceededJdbc(operation, externalReference, result));
    }

    /**
     * Completes local recovery after external success has been independently confirmed. This method never calls the
     * external system; it only resumes the persisted local state machine from a recoverable checkpoint.
     */
    public WriteOperation recoverExternalSucceeded(WriteOperation operation, String externalReference, Object result) {
        WriteOperation current = findByKey(operation.getIdempotencyKey());
        if (current == null) current = operation;
        if (WriteOperationStatus.SUCCEEDED.equals(current.getStatus())) return current;
        if (WriteOperationStatus.EXECUTING.equals(current.getStatus())
                || WriteOperationStatus.EXTERNAL_UNKNOWN.equals(current.getStatus())
                || WriteOperationStatus.NEEDS_RECONCILIATION.equals(current.getStatus())) {
            return externalSucceeded(current, externalReference, result);
        }
        String resultJson = json.toJson(result);
        if (!jdbcPersistence) {
            if (WriteOperationStatus.EXTERNAL_SUCCEEDED.equals(current.getStatus())) {
                current = transitionMemory(current, WriteOperationStatus.LOCAL_CONFIRMED, externalReference,
                        resultJson, null, null, "NONE");
            }
            if (WriteOperationStatus.LOCAL_CONFIRMED.equals(current.getStatus())) {
                return transitionMemory(current, WriteOperationStatus.SUCCEEDED, externalReference,
                        resultJson, null, null, "NONE");
            }
            throw new IllegalStateException("Unsupported recovery state after external success: " + current.getStatus());
        }
        WriteOperation finalCurrent = current;
        return inJdbcTransaction(() -> {
            WriteOperation refreshed = requiredJdbcOperation(finalCurrent);
            if (WriteOperationStatus.EXTERNAL_SUCCEEDED.equals(refreshed.getStatus())) {
                transitionJdbc(refreshed, WriteOperationStatus.LOCAL_CONFIRMED, externalReference, resultJson, null, null, "NONE");
                refreshed = requiredJdbcOperation(refreshed);
            }
            if (WriteOperationStatus.LOCAL_CONFIRMED.equals(refreshed.getStatus())) {
                transitionJdbc(refreshed, WriteOperationStatus.SUCCEEDED, externalReference, resultJson, null, null, "NONE");
                refreshed = requiredJdbcOperation(refreshed);
                outbox.insert(refreshed.getTenantId(), refreshed.getShopId(), "WRITE_OPERATION", String.valueOf(refreshed.getId()),
                        "write.operation.succeeded", json.toJson(Map.of(
                                "operationId", refreshed.getId(),
                                "toolCode", refreshed.getToolCode(),
                                "externalReference", externalReference
                        )), LocalDateTime.now());
                return refreshed;
            }
            if (WriteOperationStatus.SUCCEEDED.equals(refreshed.getStatus())) return refreshed;
            throw new IllegalStateException("Unsupported JDBC recovery state after external success: " + refreshed.getStatus());
        });
    }

    public WriteOperation externalUnknown(WriteOperation operation, String externalReference, String message) {
        if (!jdbcPersistence) {
            return transitionMemory(operation, WriteOperationStatus.EXTERNAL_UNKNOWN, externalReference, null,
                    "EXTERNAL_RESULT_UNKNOWN", message, "QUERY_EXTERNAL");
        }
        return inJdbcTransaction(() -> externalUnknownJdbc(operation, externalReference, message));
    }

    public WriteOperation failed(WriteOperation operation, String code, String message) {
        if (!jdbcPersistence) {
            return transitionMemory(operation, WriteOperationStatus.FAILED, operation.getExternalReference(), null,
                    code, message, "MANUAL_REVIEW");
        }
        return inJdbcTransaction(() -> failedJdbc(operation, code, message));
    }

    /**
     * Read-only observability query used by audit/evaluation code. It does not mutate write state.
     */
    public List<WriteOperation> listByTaskId(Long tenantId, Long shopId, Long taskId) {
        if (tenantId == null || shopId == null || taskId == null) {
            return List.of();
        }
        if (jdbcPersistence) {
            return mapper.listByTaskId(tenantId, shopId, taskId);
        }
        return memoryByKey.values().stream()
                .filter(operation -> tenantId.equals(operation.getTenantId()))
                .filter(operation -> shopId.equals(operation.getShopId()))
                .filter(operation -> taskId.equals(operation.getTaskId()))
                .sorted(java.util.Comparator.comparing(WriteOperation::getId))
                .toList();
    }

    public WriteOperation findByKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;
        if (jdbcPersistence) return mapper.findByKey(idempotencyKey);
        WriteOperation operation = memoryByKey.get(idempotencyKey);
        return operation == null ? null : snapshot(operation, false);
    }

    public WriteOperation recordRecoveryAttempt(WriteOperation operation) {
        LocalDateTime now = LocalDateTime.now();
        if (jdbcPersistence) {
            int updated = mapper.recordRecoveryAttempt(operation.getId(), operation.getStatus(), now);
            if (updated != 1) return requiredJdbcOperation(operation);
            return requiredJdbcOperation(operation);
        }
        WriteOperation updated = memoryByKey.compute(operation.getIdempotencyKey(), (ignored, current) -> {
            if (current == null) throw new IllegalStateException("Write operation disappeared: " + operation.getIdempotencyKey());
            if (WriteOperationStatus.terminal(current.getStatus())) return current;
            current.setRecoveryAttemptCount((current.getRecoveryAttemptCount() == null ? 0 : current.getRecoveryAttemptCount()) + 1);
            current.setLastRecoveryAt(now);
            current.setUpdatedAt(now);
            return current;
        });
        return snapshot(updated, false);
    }

    public WriteOperation needsReconciliation(WriteOperation operation, String code, String message) {
        WriteOperation latest = findByKey(operation.getIdempotencyKey());
        final WriteOperation selected = latest == null ? operation : latest;
        if (WriteOperationStatus.terminal(selected.getStatus())) return selected;
        LocalDateTime now = LocalDateTime.now();
        if (!jdbcPersistence) {
            WriteOperation updated = memoryByKey.compute(selected.getIdempotencyKey(), (ignored, current) -> {
                if (current == null) throw new IllegalStateException("Write operation disappeared: " + selected.getIdempotencyKey());
                if (WriteOperationStatus.terminal(current.getStatus())) return current;
                if (!WriteOperationStatus.NEEDS_RECONCILIATION.equals(current.getStatus())) {
                    WriteOperationStatus.requireTransition(current.getStatus(), WriteOperationStatus.NEEDS_RECONCILIATION);
                    current.setStatus(WriteOperationStatus.NEEDS_RECONCILIATION);
                    current.setVersion(current.getVersion() == null ? 1 : current.getVersion() + 1);
                }
                current.setLastErrorCode(code);
                current.setLastErrorMessage(message);
                current.setRetryAction("QUERY_EXTERNAL");
                current.setUpdatedAt(now);
                return current;
            });
            return snapshot(updated, false);
        }
        if (WriteOperationStatus.NEEDS_RECONCILIATION.equals(selected.getStatus())) {
            mapper.updateRecoveryObservation(selected.getId(), selected.getStatus(), code, message, "QUERY_EXTERNAL", now);
            return requiredJdbcOperation(selected);
        }
        return inJdbcTransaction(() -> {
            transitionJdbc(selected, WriteOperationStatus.NEEDS_RECONCILIATION, selected.getExternalReference(), selected.getResultJson(), code, message, "QUERY_EXTERNAL");
            return requiredJdbcOperation(selected);
        });
    }

    public WriteOperation needsManualAction(WriteOperation operation, String code, String message) {
        WriteOperation latest = findByKey(operation.getIdempotencyKey());
        final WriteOperation selected = latest == null ? operation : latest;
        if (WriteOperationStatus.terminal(selected.getStatus())) return selected;
        if (!jdbcPersistence) {
            WriteOperation updated = memoryByKey.compute(selected.getIdempotencyKey(), (ignored, current) -> {
                if (current == null) throw new IllegalStateException("Write operation disappeared: " + selected.getIdempotencyKey());
                if (WriteOperationStatus.terminal(current.getStatus())) return current;
                WriteOperationStatus.requireTransition(current.getStatus(), WriteOperationStatus.NEEDS_MANUAL_ACTION);
                applyTransition(current, WriteOperationStatus.NEEDS_MANUAL_ACTION, current.getExternalReference(),
                        current.getResultJson(), code, message, "MANUAL_REVIEW");
                return current;
            });
            return snapshot(updated, false);
        }
        return inJdbcTransaction(() -> {
            WriteOperation current = requiredJdbcOperation(selected);
            if (WriteOperationStatus.terminal(current.getStatus())) return current;
            transitionJdbc(current, WriteOperationStatus.NEEDS_MANUAL_ACTION, current.getExternalReference(), current.getResultJson(), code, message, "MANUAL_REVIEW");
            return requiredJdbcOperation(current);
        });
    }

    private WriteOperation prepareJdbc(ToolInvokeContext context, String toolCode, String objectId,
                                       String requestId, Object input) {
        String key = idempotencyKey(toolCode, context, objectId, requestId);
        WriteOperation existing = mapper.findByKey(key);
        if (existing != null) {
            assertSameInput(existing, input);
            existing.setFreshExecution(false);
            return existing;
        }
        WriteOperation operation = newOperation(context, toolCode, objectId, requestId, input, key);
        try {
            mapper.insert(operation);
        } catch (DuplicateKeyException ex) {
            WriteOperation duplicated = mapper.findByKey(key);
            if (duplicated == null) {
                throw ex;
            }
            assertSameInput(duplicated, input);
            duplicated.setFreshExecution(false);
            return duplicated;
        }
        transitionJdbc(operation, WriteOperationStatus.EXECUTING, null, null, null, null, "NO_RETRY");
        operation.setStatus(WriteOperationStatus.EXECUTING);
        operation.setVersion(1);
        operation.setFreshExecution(true);
        return operation;
    }

    private WriteOperation externalSucceededJdbc(WriteOperation operation, String externalReference, Object result) {
        String resultJson = json.toJson(result);
        transitionJdbc(operation, WriteOperationStatus.EXTERNAL_SUCCEEDED, externalReference, resultJson, null, null, "CONFIRM_LOCAL");
        WriteOperation refreshed = requiredJdbcOperation(operation);
        transitionJdbc(refreshed, WriteOperationStatus.LOCAL_CONFIRMED, externalReference, resultJson, null, null, "NONE");
        refreshed = requiredJdbcOperation(operation);
        transitionJdbc(refreshed, WriteOperationStatus.SUCCEEDED, externalReference, resultJson, null, null, "NONE");
        outbox.insert(operation.getTenantId(), operation.getShopId(), "WRITE_OPERATION", String.valueOf(operation.getId()),
                "write.operation.succeeded", json.toJson(Map.of(
                        "operationId", operation.getId(),
                        "toolCode", operation.getToolCode(),
                        "externalReference", externalReference
                )), LocalDateTime.now());
        return requiredJdbcOperation(operation);
    }

    private WriteOperation externalUnknownJdbc(WriteOperation operation, String externalReference, String message) {
        transitionJdbc(operation, WriteOperationStatus.EXTERNAL_UNKNOWN, externalReference, null,
                "EXTERNAL_RESULT_UNKNOWN", message, "QUERY_EXTERNAL");
        outbox.insert(operation.getTenantId(), operation.getShopId(), "WRITE_OPERATION", String.valueOf(operation.getId()),
                "write.operation.reconciliation.required", json.toJson(Map.of(
                        "operationId", operation.getId(),
                        "reason", "EXTERNAL_RESULT_UNKNOWN"
                )), LocalDateTime.now());
        return requiredJdbcOperation(operation);
    }

    private WriteOperation failedJdbc(WriteOperation operation, String code, String message) {
        transitionJdbc(operation, WriteOperationStatus.FAILED, operation.getExternalReference(), null,
                code, message, "MANUAL_REVIEW");
        return requiredJdbcOperation(operation);
    }

    private WriteOperation requiredJdbcOperation(WriteOperation operation) {
        WriteOperation refreshed = mapper.findByKey(operation.getIdempotencyKey());
        if (refreshed == null) {
            throw new IllegalStateException("Write operation disappeared: " + operation.getIdempotencyKey());
        }
        return refreshed;
    }

    private <T> T inJdbcTransaction(Supplier<T> action) {
        if (jdbcTransactionTemplate == null) {
            throw new IllegalStateException("JDBC transaction template is not configured");
        }
        T result = jdbcTransactionTemplate.execute(status -> action.get());
        if (result == null) {
            throw new IllegalStateException("JDBC write operation transaction returned no result");
        }
        return result;
    }

    private WriteOperation prepareMemory(ToolInvokeContext context, String toolCode, String objectId, String requestId, Object input) {
        String key = idempotencyKey(toolCode, context, objectId, requestId);
        AtomicBoolean created = new AtomicBoolean(false);
        WriteOperation prepared = memoryByKey.compute(key, (ignored, current) -> {
            if (current != null) {
                assertSameInput(current, input);
                return current;
            }
            WriteOperation operation = newOperation(context, toolCode, objectId, requestId, input, key);
            operation.setId(memoryIds.getAndIncrement());
            WriteOperationStatus.requireTransition(operation.getStatus(), WriteOperationStatus.EXECUTING);
            applyTransition(operation, WriteOperationStatus.EXECUTING, null, null, null, null, "NO_RETRY");
            created.set(true);
            return operation;
        });
        return snapshot(prepared, created.get());
    }

    private WriteOperation externalSucceededMemory(WriteOperation operation, String externalReference, Object result) {
        String resultJson = json.toJson(result);
        WriteOperation updated = transitionMemory(operation, WriteOperationStatus.EXTERNAL_SUCCEEDED, externalReference,
                resultJson, null, null, "CONFIRM_LOCAL");
        updated = transitionMemory(updated, WriteOperationStatus.LOCAL_CONFIRMED, externalReference,
                resultJson, null, null, "NONE");
        return transitionMemory(updated, WriteOperationStatus.SUCCEEDED, externalReference,
                resultJson, null, null, "NONE");
    }

    private WriteOperation newOperation(ToolInvokeContext context, String toolCode, String objectId,
                                        String requestId, Object input, String key) {
        LocalDateTime now = LocalDateTime.now();
        WriteOperation operation = new WriteOperation();
        operation.setTenantId(context.getTenantId());
        operation.setShopId(context.getShopId());
        operation.setUserId(context.getUserId());
        operation.setTaskId(context.getTaskId());
        operation.setTraceId(context.getTraceId());
        operation.setToolCode(toolCode);
        operation.setBusinessObjectId(objectId);
        operation.setOperationRequestId(requestId.trim());
        operation.setIdempotencyKey(key);
        operation.setInputHash(inputHash(input));
        operation.setApprovalId(context.getApprovalId());
        operation.setStatus(WriteOperationStatus.APPROVED);
        operation.setRecoveryAttemptCount(0);
        operation.setVersion(0);
        operation.setCreatedAt(now);
        operation.setUpdatedAt(now);
        return operation;
    }

    private void assertSameInput(WriteOperation existing, Object input) {
        if (!existing.getInputHash().equals(inputHash(input))) {
            throw new IdempotencyConflictException("Input changed for the same idempotency key");
        }
    }

    private WriteOperation transitionMemory(WriteOperation operation, String to, String externalReference,
                                            String resultJson, String code, String message, String retryAction) {
        String key = operation.getIdempotencyKey();
        WriteOperation updated = memoryByKey.compute(key, (ignored, current) -> {
            if (current == null) {
                throw new IllegalStateException("Write operation disappeared: " + key);
            }
            WriteOperationStatus.requireTransition(current.getStatus(), to);
            applyTransition(current, to, externalReference, resultJson, code, message, retryAction);
            return current;
        });
        return snapshot(updated, false);
    }

    private void applyTransition(WriteOperation operation, String to, String externalReference,
                                 String resultJson, String code, String message, String retryAction) {
        operation.setStatus(to);
        operation.setExternalReference(externalReference);
        operation.setResultJson(resultJson);
        operation.setLastErrorCode(code);
        operation.setLastErrorMessage(message);
        operation.setRetryAction(retryAction);
        operation.setVersion(operation.getVersion() == null ? 1 : operation.getVersion() + 1);
        operation.setUpdatedAt(LocalDateTime.now());
        operation.setFreshExecution(false);
    }

    private String canonicalSemanticInput(Object input) {
        if (input == null) {
            return "null";
        }
        try {
            Map<String, Object> values = new TreeMap<>(json.toMap(json.toJson(input)));
            // approvalId is execution metadata. Binding it into the semantic input hash would make the same logical
            // write look different merely because it was replayed through a different approval execution record.
            values.remove("approvalId");
            return json.toJson(values);
        } catch (RuntimeException ex) {
            return json.toJson(input);
        }
    }

    private WriteOperation snapshot(WriteOperation source, boolean freshExecution) {
        WriteOperation copy = new WriteOperation();
        copy.setId(source.getId());
        copy.setTenantId(source.getTenantId());
        copy.setShopId(source.getShopId());
        copy.setUserId(source.getUserId());
        copy.setTaskId(source.getTaskId());
        copy.setTraceId(source.getTraceId());
        copy.setToolCode(source.getToolCode());
        copy.setBusinessObjectId(source.getBusinessObjectId());
        copy.setOperationRequestId(source.getOperationRequestId());
        copy.setIdempotencyKey(source.getIdempotencyKey());
        copy.setInputHash(source.getInputHash());
        copy.setApprovalId(source.getApprovalId());
        copy.setStatus(source.getStatus());
        copy.setExternalReference(source.getExternalReference());
        copy.setResultJson(source.getResultJson());
        copy.setLastErrorCode(source.getLastErrorCode());
        copy.setLastErrorMessage(source.getLastErrorMessage());
        copy.setRetryAction(source.getRetryAction());
        copy.setRecoveryAttemptCount(source.getRecoveryAttemptCount());
        copy.setLastRecoveryAt(source.getLastRecoveryAt());
        copy.setVersion(source.getVersion());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setFreshExecution(freshExecution);
        return copy;
    }

    private void transitionJdbc(WriteOperation operation, String to, String externalReference,
                                String resultJson, String code, String message, String retryAction) {
        WriteOperationStatus.requireTransition(operation.getStatus(), to);
        int updated = mapper.transition(operation.getId(), operation.getStatus(), to, externalReference, resultJson,
                code, message, retryAction, operation.getVersion() == null ? 0 : operation.getVersion(),
                LocalDateTime.now());
        if (updated != 1) {
            throw new IllegalStateException("Write operation was modified concurrently: " + operation.getId());
        }
    }
}
