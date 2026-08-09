package com.sirithree.shopops.admin.reliability.external;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SimulatedRefundExternalTransport implements RefundExternalTransport {
    private final Map<String, Result> accepted = new ConcurrentHashMap<>();
    private final Map<String, Result> acceptedByRequest = new ConcurrentHashMap<>();

    @Override
    public Result execute(String operationRequestId, String orderId, int amount, String simulation) {
        String reference = "REF-" + operationRequestId;
        if ("timeout_before_success".equalsIgnoreCase(simulation)) {
            return new Result("UNKNOWN", null, Map.of(), "外部超时，未受理退款");
        }
        if ("failure".equalsIgnoreCase(simulation)) {
            return new Result("FAILED", reference, Map.of(), "外部明确拒绝退款");
        }
        Result success = new Result("SUCCEEDED", reference, Map.of(
                "refundId", reference, "orderId", orderId, "refundAmount", amount, "status", "SUCCEEDED"), null);
        accepted.put(reference, success);
        acceptedByRequest.put(operationRequestId, success);
        if ("timeout_after_success".equalsIgnoreCase(simulation)) {
            return new Result("UNKNOWN", reference, Map.of(), "外部已受理但响应丢失，结果未知");
        }
        return success;
    }

    @Override
    public Result query(String externalReference) {
        Result result = accepted.get(externalReference);
        return result == null ? new Result("UNKNOWN", externalReference, Map.of(), "外部系统未找到该退款结果") : result;
    }

    @Override
    public Result queryByOperationRequestId(String operationRequestId) {
        Result result = acceptedByRequest.get(operationRequestId);
        return result == null
                ? new Result("NOT_ACCEPTED", null, Map.of(), "外部系统确认未受理该退款请求")
                : result;
    }
}
