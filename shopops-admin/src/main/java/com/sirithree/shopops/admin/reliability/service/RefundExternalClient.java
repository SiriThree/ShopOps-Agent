package com.sirithree.shopops.admin.reliability.service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RefundExternalClient {
    public ExternalResult execute(String operationRequestId, String orderId, int amount, String simulation) {
        String reference = "REF-" + operationRequestId;
        if ("timeout_after_success".equalsIgnoreCase(simulation)) return new ExternalResult("UNKNOWN", reference, Map.of(), "外部超时，结果未知");
        if ("failure".equalsIgnoreCase(simulation)) return new ExternalResult("FAILED", reference, Map.of(), "外部明确拒绝退款");
        return new ExternalResult("SUCCEEDED", reference, Map.of("refundId", reference, "orderId", orderId, "refundAmount", amount, "status", "SUCCEEDED"), null);
    }
    public ExternalResult query(String reference) {
        return new ExternalResult("SUCCEEDED", reference, Map.of("refundId", reference, "status", "SUCCEEDED"), null);
    }
    public record ExternalResult(String status, String reference, Map<String,Object> data, String message) {}
}
