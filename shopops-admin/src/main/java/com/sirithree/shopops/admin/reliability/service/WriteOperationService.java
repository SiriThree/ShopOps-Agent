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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WriteOperationService {
    private final WriteOperationMapper mapper;
    private final OutboxEventMapper outbox;
    private final JacksonJsonSupport json;
    private final boolean jdbcPersistence;
    private final AtomicLong memoryIds = new AtomicLong(1);
    private final Map<String, WriteOperation> memoryByKey = new ConcurrentHashMap<>();

    public WriteOperationService(WriteOperationMapper mapper,
                                 OutboxEventMapper outbox,
                                 JacksonJsonSupport json,
                                 @Value("${shopops.persistence:memory}") String persistence) {
        this.mapper = mapper;
        this.outbox = outbox;
        this.json = json;
        this.jdbcPersistence = "jdbc".equalsIgnoreCase(persistence);
    }

    public String inputHash(Object input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.toJson(input).getBytes(StandardCharsets.UTF_8)));
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

    @Transactional
    public WriteOperation prepare(ToolInvokeContext context, String toolCode, String objectId, String requestId, Object input) {
        if (!jdbcPersistence) {
            return prepareMemory(context, toolCode, objectId, requestId, input);
        }
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
            if (duplicated != null) {
                duplicated.setFreshExecution(false);
            }
            return duplicated;
        }
        transitionJdbc(operation, WriteOperationStatus.EXECUTING, null, null, null, null, "NO_RETRY");
        operation.setStatus(WriteOperationStatus.EXECUTING);
        operation.setVersion(1);
        operation.setFreshExecution(true);
        return operation;
    }

    @Transactional
    public WriteOperation externalSucceeded(WriteOperation operation, String externalReference, Object result) {
        if (!jdbcPersistence) {
            return externalSucceededMemory(operation, externalReference, result);
        }
        String resultJson = json.toJson(result);
        transitionJdbc(operation, WriteOperationStatus.EXTERNAL_SUCCEEDED, externalReference, resultJson, null, null, "CONFIRM_LOCAL");
        WriteOperation refreshed = mapper.findByKey(operation.getIdempotencyKey());
        transitionJdbc(refreshed, WriteOperationStatus.LOCAL_CONFIRMED, externalReference, resultJson, null, null, "NONE");
        refreshed = mapper.findByKey(operation.getIdempotencyKey());
        transitionJdbc(refreshed, WriteOperationStatus.SUCCEEDED, externalReference, resultJson, null, null, "NONE");
        outbox.insert(operation.getTenantId(), operation.getShopId(), "WRITE_OPERATION", String.valueOf(operation.getId()),
                "write.operation.succeeded", json.toJson(Map.of(
                        "operationId", operation.getId(),
                        "toolCode", operation.getToolCode(),
                        "externalReference", externalReference
                )), LocalDateTime.now());
        return mapper.findByKey(operation.getIdempotencyKey());
    }

    @Transactional
    public WriteOperation externalUnknown(WriteOperation operation, String externalReference, String message) {
        if (!jdbcPersistence) {
            return transitionMemory(operation, WriteOperationStatus.EXTERNAL_UNKNOWN, externalReference, null,
                    "EXTERNAL_RESULT_UNKNOWN", message, "QUERY_EXTERNAL");
        }
        transitionJdbc(operation, WriteOperationStatus.EXTERNAL_UNKNOWN, externalReference, null,
                "EXTERNAL_RESULT_UNKNOWN", message, "QUERY_EXTERNAL");
        outbox.insert(operation.getTenantId(), operation.getShopId(), "WRITE_OPERATION", String.valueOf(operation.getId()),
                "write.operation.reconciliation.required", json.toJson(Map.of(
                        "operationId", operation.getId(),
                        "reason", "EXTERNAL_RESULT_UNKNOWN"
                )), LocalDateTime.now());
        return mapper.findByKey(operation.getIdempotencyKey());
    }

    @Transactional
    public WriteOperation failed(WriteOperation operation, String code, String message) {
        if (!jdbcPersistence) {
            return transitionMemory(operation, WriteOperationStatus.FAILED, operation.getExternalReference(), null,
                    code, message, "MANUAL_REVIEW");
        }
        transitionJdbc(operation, WriteOperationStatus.FAILED, operation.getExternalReference(), null,
                code, message, "MANUAL_REVIEW");
        return mapper.findByKey(operation.getIdempotencyKey());
    }

    private WriteOperation prepareMemory(ToolInvokeContext context, String toolCode, String objectId, String requestId, Object input) {
        String key = idempotencyKey(toolCode, context, objectId, requestId);
        WriteOperation existing = memoryByKey.get(key);
        if (existing != null) {
            assertSameInput(existing, input);
            existing.setFreshExecution(false);
            return existing;
        }
        WriteOperation operation = newOperation(context, toolCode, objectId, requestId, input, key);
        operation.setId(memoryIds.getAndIncrement());
        memoryByKey.put(key, operation);
        return transitionMemory(operation, WriteOperationStatus.EXECUTING, null, null, null, null, "NO_RETRY", true);
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
        operation.setVersion(0);
        operation.setCreatedAt(now);
        operation.setUpdatedAt(now);
        return operation;
    }

    private void assertSameInput(WriteOperation existing, Object input) {
        if (!existing.getInputHash().equals(inputHash(input))) {
            throw new IllegalStateException("Input changed for the same idempotency key");
        }
    }

    private WriteOperation transitionMemory(WriteOperation operation, String to, String externalReference,
                                            String resultJson, String code, String message, String retryAction) {
        return transitionMemory(operation, to, externalReference, resultJson, code, message, retryAction, false);
    }

    private WriteOperation transitionMemory(WriteOperation operation, String to, String externalReference,
                                            String resultJson, String code, String message, String retryAction,
                                            boolean freshExecution) {
        WriteOperationStatus.requireTransition(operation.getStatus(), to);
        operation.setStatus(to);
        operation.setExternalReference(externalReference);
        operation.setResultJson(resultJson);
        operation.setLastErrorCode(code);
        operation.setLastErrorMessage(message);
        operation.setRetryAction(retryAction);
        operation.setVersion(operation.getVersion() == null ? 1 : operation.getVersion() + 1);
        operation.setUpdatedAt(LocalDateTime.now());
        operation.setFreshExecution(freshExecution);
        memoryByKey.put(operation.getIdempotencyKey(), operation);
        return operation;
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
