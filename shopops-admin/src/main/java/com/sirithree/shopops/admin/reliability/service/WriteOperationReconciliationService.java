package com.sirithree.shopops.admin.reliability.service;

import com.sirithree.shopops.admin.reliability.domain.WriteOperation;
import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultContext;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultController;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultPoint;
import com.sirithree.shopops.admin.reliability.persistence.WriteOperationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WriteOperationReconciliationService {
    private final WriteOperationMapper mapper;
    private final RefundExternalClient external;
    private final WriteOperationService operations;
    private final ReliabilityFaultController faults;
    private int maxRecoveryAttempts = 3;

    public WriteOperationReconciliationService(WriteOperationMapper mapper, RefundExternalClient external,
                                                WriteOperationService operations, ReliabilityFaultController faults) {
        this.mapper = mapper;
        this.external = external;
        this.operations = operations;
        this.faults = faults;
    }

    @Value("${shopops.reliability.reconciliation-max-attempts:3}")
    public void setMaxRecoveryAttempts(int maxRecoveryAttempts) {
        this.maxRecoveryAttempts = Math.max(1, maxRecoveryAttempts);
    }

    public int reconcile(int staleMinutes, int limit) {
        List<WriteOperation> candidates = mapper.findForReconciliation(
                LocalDateTime.now().minusMinutes(Math.max(1, staleMinutes)), Math.max(1, Math.min(limit, 500)));
        int recovered = 0;
        for (WriteOperation operation : candidates) {
            RecoveryResult result = reconcileOperation(operation);
            if (result.converged()) recovered++;
        }
        return recovered;
    }

    /** Runs one production recovery attempt. It never writes a benchmark-supplied terminal state. */
    public RecoveryResult reconcileOperation(WriteOperation candidate) {
        if (candidate == null || !"order.refund_execute".equals(candidate.getToolCode())) {
            return new RecoveryResult(false, false, "UNSUPPORTED_OPERATION", null);
        }
        WriteOperation current = operations.findByKey(candidate.getIdempotencyKey());
        if (current == null) current = candidate;
        if (WriteOperationStatus.SUCCEEDED.equals(current.getStatus())) {
            return new RecoveryResult(true, true, "ALREADY_SUCCEEDED", current);
        }
        if (WriteOperationStatus.FAILED.equals(current.getStatus()) || WriteOperationStatus.NEEDS_MANUAL_ACTION.equals(current.getStatus())) {
            return new RecoveryResult(true, false, "TERMINAL_WITHOUT_AUTOMATIC_SUCCESS", current);
        }

        current = operations.recordRecoveryAttempt(current);
        if (WriteOperationStatus.SUCCEEDED.equals(current.getStatus())) {
            return new RecoveryResult(true, true, "CONCURRENT_RECOVERY_ALREADY_SUCCEEDED", current);
        }
        if (WriteOperationStatus.FAILED.equals(current.getStatus()) || WriteOperationStatus.NEEDS_MANUAL_ACTION.equals(current.getStatus())) {
            return new RecoveryResult(true, false, "CONCURRENT_RECOVERY_REACHED_TERMINAL_STATE", current);
        }
        int attempt = current.getRecoveryAttemptCount() == null ? 1 : current.getRecoveryAttemptCount();
        ReliabilityFaultContext faultContext = new ReliabilityFaultContext(
                current.getToolCode(), current.getOperationRequestId(), current.getBusinessObjectId(), current.getExternalReference());
        try {
            faults.hit(ReliabilityFaultPoint.BEFORE_RECONCILIATION_QUERY, faultContext);
            RefundExternalClient.ExternalResult result = current.getExternalReference() == null || current.getExternalReference().isBlank()
                    ? external.queryByOperationRequestId(current.getOperationRequestId())
                    : external.query(current.getExternalReference());
            faults.hit(ReliabilityFaultPoint.AFTER_RECONCILIATION_RESULT,
                    new ReliabilityFaultContext(current.getToolCode(), current.getOperationRequestId(),
                            current.getBusinessObjectId(), result.reference()));

            if ("SUCCEEDED".equals(result.status())) {
                faults.hit(ReliabilityFaultPoint.BEFORE_RECOVERY_STATE_UPDATE,
                        new ReliabilityFaultContext(current.getToolCode(), current.getOperationRequestId(),
                                current.getBusinessObjectId(), result.reference()));
                try {
                    WriteOperation completed = operations.recoverExternalSucceeded(current, result.reference(), result.data());
                    return new RecoveryResult(true, true, "EXTERNAL_SUCCESS_CONFIRMED", completed);
                } catch (IllegalStateException concurrentUpdate) {
                    WriteOperation refreshed = operations.findByKey(current.getIdempotencyKey());
                    if (refreshed != null && WriteOperationStatus.SUCCEEDED.equals(refreshed.getStatus())) {
                        return new RecoveryResult(true, true, "CONCURRENT_RECOVERY_ALREADY_SUCCEEDED", refreshed);
                    }
                    throw concurrentUpdate;
                }
            }
            if ("NOT_ACCEPTED".equals(result.status())) {
                WriteOperation failed = operations.failed(current, "EXTERNAL_NOT_ACCEPTED", result.message());
                return new RecoveryResult(true, true, "EXTERNAL_NOT_ACCEPTED_CONFIRMED", failed);
            }
            if ("FAILED".equals(result.status())) {
                WriteOperation failed = operations.failed(current, "EXTERNAL_CONFIRMED_FAILED", result.message());
                return new RecoveryResult(true, true, "EXTERNAL_FAILURE_CONFIRMED", failed);
            }
            if ("DUPLICATE".equals(result.status())) {
                WriteOperation manual = operations.needsManualAction(current, "EXTERNAL_DUPLICATE_EFFECT", result.message());
                return new RecoveryResult(true, false, "EXTERNAL_DUPLICATE_REQUIRES_MANUAL_REVIEW", manual);
            }
            return unresolved(current, attempt, "EXTERNAL_STATUS_UNAVAILABLE", result.message());
        } catch (RuntimeException ex) {
            WriteOperation refreshed = operations.findByKey(current.getIdempotencyKey());
            if (refreshed != null && WriteOperationStatus.SUCCEEDED.equals(refreshed.getStatus())) {
                return new RecoveryResult(true, true, "CONCURRENT_RECOVERY_ALREADY_SUCCEEDED", refreshed);
            }
            if (refreshed != null && (WriteOperationStatus.FAILED.equals(refreshed.getStatus())
                    || WriteOperationStatus.NEEDS_MANUAL_ACTION.equals(refreshed.getStatus()))) {
                return new RecoveryResult(true, false, "CONCURRENT_RECOVERY_REACHED_TERMINAL_STATE", refreshed);
            }
            return unresolved(refreshed == null ? current : refreshed, attempt, "RECONCILIATION_QUERY_FAILED", ex.getMessage());
        }
    }

    private RecoveryResult unresolved(WriteOperation current, int attempt, String code, String message) {
        WriteOperation latest = operations.findByKey(current.getIdempotencyKey());
        if (latest != null) current = latest;
        if (WriteOperationStatus.SUCCEEDED.equals(current.getStatus())) {
            return new RecoveryResult(true, true, "CONCURRENT_RECOVERY_ALREADY_SUCCEEDED", current);
        }
        if (WriteOperationStatus.FAILED.equals(current.getStatus()) || WriteOperationStatus.NEEDS_MANUAL_ACTION.equals(current.getStatus())) {
            return new RecoveryResult(true, false, "CONCURRENT_RECOVERY_REACHED_TERMINAL_STATE", current);
        }
        WriteOperation updated;
        if (attempt >= maxRecoveryAttempts) {
            updated = operations.needsManualAction(current, "RECOVERY_BUDGET_EXHAUSTED",
                    code + ": " + safe(message));
            if (WriteOperationStatus.SUCCEEDED.equals(updated.getStatus())) {
                return new RecoveryResult(true, true, "CONCURRENT_RECOVERY_ALREADY_SUCCEEDED", updated);
            }
            return new RecoveryResult(true, false, "RECOVERY_BUDGET_EXHAUSTED", updated);
        }
        updated = operations.needsReconciliation(current, code, safe(message));
        return new RecoveryResult(false, false, code, updated);
    }

    private String safe(String message) {
        return message == null || message.isBlank() ? "unavailable" : message;
    }

    public record RecoveryResult(boolean terminalStateReached, boolean stateCorrect, String reason, WriteOperation operation) {
        public boolean converged() {
            return terminalStateReached && stateCorrect;
        }
    }
}
