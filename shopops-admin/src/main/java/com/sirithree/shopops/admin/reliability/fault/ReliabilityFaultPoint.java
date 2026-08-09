package com.sirithree.shopops.admin.reliability.fault;

/** Production reliability boundaries that can be observed or deterministically faulted by integration tests. */
public enum ReliabilityFaultPoint {
    BEFORE_EXTERNAL_CALL,
    AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM,
    AFTER_LOCAL_CONFIRM_BEFORE_ACK,
    BEFORE_OUTBOX_MARK_PUBLISHED,
    BEFORE_RECONCILIATION_QUERY,
    AFTER_RECONCILIATION_RESULT,
    BEFORE_RECOVERY_STATE_UPDATE
}
