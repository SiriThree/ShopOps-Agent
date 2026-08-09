package com.sirithree.shopops.admin.reliability.external;

import java.util.Map;

/** Infrastructure boundary for the external refund system. */
public interface RefundExternalTransport {
    Result execute(String operationRequestId, String orderId, int amount, String simulation);

    Result query(String externalReference);

    /**
     * Recovery correlation lookup using the durable request identity that ShopOps persists before the external call.
     * A production adapter must only implement this when its external API can genuinely query by that identity.
     */
    Result queryByOperationRequestId(String operationRequestId);

    record Result(String status, String reference, Map<String, Object> data, String message) {
    }
}
