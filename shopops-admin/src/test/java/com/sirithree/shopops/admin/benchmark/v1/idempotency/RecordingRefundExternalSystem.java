package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import com.sirithree.shopops.admin.reliability.external.RefundExternalTransport;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Independent external refund ground truth. It never reads WriteOperation or any ShopOps persistence state.
 * NON_IDEMPOTENT_EXTERNAL intentionally creates a new effect for every accepted external call, exposing whether
 * ShopOps itself prevented duplicate execution before the external boundary.
 */
public class RecordingRefundExternalSystem implements RefundExternalTransport {
    private final AtomicLong attemptIds = new AtomicLong();
    private final AtomicLong effectIds = new AtomicLong();
    private final List<ExternalAttempt> attempts = new CopyOnWriteArrayList<>();
    private final List<ExternalSideEffect> effects = new CopyOnWriteArrayList<>();
    private final Map<String, ExternalSideEffect> byReference = new ConcurrentHashMap<>();
    private final Map<String, ExternalSideEffect> byLogicalOperation = new ConcurrentHashMap<>();
    private final Map<String, String> realityByLogicalOperation = new ConcurrentHashMap<>();
    private volatile ExternalSystemMode mode = ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL;

    public void reset(ExternalSystemMode newMode) {
        mode = newMode == null ? ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL : newMode;
        attempts.clear();
        effects.clear();
        byReference.clear();
        byLogicalOperation.clear();
        realityByLogicalOperation.clear();
        attemptIds.set(0);
        effectIds.set(0);
    }

    @Override
    public Result execute(String operationRequestId, String orderId, int amount, String simulation) {
        long attemptNo = attemptIds.incrementAndGet();
        String payloadHash = sha256(orderId + "|" + amount);
        Instant now = Instant.now();
        if ("failure".equalsIgnoreCase(simulation)) {
            attempts.add(new ExternalAttempt(attemptNo, "order.refund_execute", operationRequestId, orderId,
                    payloadHash, simulation, "REJECTED", null, now));
            realityByLogicalOperation.put(operationRequestId, "FAILED");
            return new Result("FAILED", null, Map.of(), "recording external system rejected refund");
        }
        if ("timeout_before_success".equalsIgnoreCase(simulation)) {
            attempts.add(new ExternalAttempt(attemptNo, "order.refund_execute", operationRequestId, orderId,
                    payloadHash, simulation, "TIMEOUT_BEFORE_ACCEPT", null, now));
            realityByLogicalOperation.put(operationRequestId, "NOT_ACCEPTED");
            return new Result("UNKNOWN", null, Map.of(), "timeout before external acceptance");
        }

        ExternalSideEffect effect;
        if (mode == ExternalSystemMode.IDEMPOTENT_EXTERNAL) {
            effect = byLogicalOperation.computeIfAbsent(operationRequestId,
                    ignored -> createEffect(operationRequestId, orderId, payloadHash));
        } else {
            effect = createEffect(operationRequestId, orderId, payloadHash);
        }
        realityByLogicalOperation.put(operationRequestId, "SUCCEEDED");
        attempts.add(new ExternalAttempt(attemptNo, "order.refund_execute", operationRequestId, orderId,
                payloadHash, simulation,
                "timeout_after_success".equalsIgnoreCase(simulation) ? "ACCEPTED_RESPONSE_LOST" : "ACCEPTED",
                effect.externalEffectId(), now));

        if ("timeout_after_success".equalsIgnoreCase(simulation)) {
            return new Result("UNKNOWN", effect.externalEffectId(), Map.of(), "external accepted but response was lost");
        }
        return success(effect, amount);
    }

    @Override
    public Result query(String externalReference) {
        ExternalSideEffect effect = byReference.get(externalReference);
        if (effect == null) {
            return new Result("UNKNOWN", externalReference, Map.of(), "external effect not found");
        }
        return success(effect, null);
    }


    @Override
    public Result queryByOperationRequestId(String operationRequestId) {
        List<ExternalSideEffect> matches = effects.stream()
                .filter(effect -> operationRequestId != null && operationRequestId.equals(effect.logicalOperationId()))
                .toList();
        if (matches.isEmpty()) {
            String reality = realityByLogicalOperation.getOrDefault(operationRequestId, "NOT_ACCEPTED");
            if ("FAILED".equals(reality)) return new Result("FAILED", null, Map.of(), "external system confirms request failed");
            return new Result("NOT_ACCEPTED", null, Map.of(), "external system confirms request was not accepted");
        }
        if (matches.size() > 1) {
            return new Result("DUPLICATE", null, Map.of("effectCount", matches.size()),
                    "multiple external side effects exist for one logical operation");
        }
        return success(matches.get(0), null);
    }


    public String reality(String operationRequestId) {
        long count = effects.stream().filter(effect -> operationRequestId != null && operationRequestId.equals(effect.logicalOperationId())).count();
        if (count > 1) return "DUPLICATE";
        if (count == 1) return "SUCCEEDED";
        return realityByLogicalOperation.getOrDefault(operationRequestId, "NOT_ACCEPTED");
    }

    public List<ExternalAttempt> attempts() {
        return List.copyOf(attempts);
    }

    public List<ExternalSideEffect> effects() {
        return List.copyOf(effects);
    }

    public int effectiveEffectCount() {
        return effects.size();
    }

    private ExternalSideEffect createEffect(String logicalOperationId, String orderId, String payloadHash) {
        String effectId = "EXT-REFUND-" + String.format(java.util.Locale.ROOT, "%06d", effectIds.incrementAndGet());
        ExternalSideEffect effect = new ExternalSideEffect(
                effectId,
                "order.refund_execute",
                logicalOperationId,
                orderId,
                payloadHash,
                Instant.now(),
                "SUCCEEDED");
        effects.add(effect);
        byReference.put(effectId, effect);
        return effect;
    }

    private Result success(ExternalSideEffect effect, Integer amount) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("refundId", effect.externalEffectId());
        data.put("orderId", effect.businessTarget());
        if (amount != null) data.put("refundAmount", amount);
        data.put("status", "SUCCEEDED");
        return new Result("SUCCEEDED", effect.externalEffectId(), data, null);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
