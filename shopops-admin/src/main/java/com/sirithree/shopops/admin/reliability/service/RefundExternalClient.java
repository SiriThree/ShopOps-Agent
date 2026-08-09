package com.sirithree.shopops.admin.reliability.service;

import com.sirithree.shopops.admin.reliability.external.RefundExternalTransport;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultContext;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultController;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultPoint;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RefundExternalClient {
    private final RefundExternalTransport transport;
    private final ReliabilityFaultController faults;

    public RefundExternalClient(RefundExternalTransport transport, ReliabilityFaultController faults) {
        this.transport = transport;
        this.faults = faults;
    }

    public ExternalResult execute(String operationRequestId, String orderId, int amount, String simulation) {
        faults.hit(
                ReliabilityFaultPoint.BEFORE_EXTERNAL_CALL,
                new ReliabilityFaultContext("order.refund_execute", operationRequestId, orderId, null));
        RefundExternalTransport.Result result = transport.execute(operationRequestId, orderId, amount, simulation);
        return new ExternalResult(result.status(), result.reference(), result.data(), result.message());
    }

    public ExternalResult query(String reference) {
        RefundExternalTransport.Result result = transport.query(reference);
        return new ExternalResult(result.status(), result.reference(), result.data(), result.message());
    }

    public ExternalResult queryByOperationRequestId(String operationRequestId) {
        RefundExternalTransport.Result result = transport.queryByOperationRequestId(operationRequestId);
        return new ExternalResult(result.status(), result.reference(), result.data(), result.message());
    }

    public record ExternalResult(String status, String reference, Map<String, Object> data, String message) {
    }
}
