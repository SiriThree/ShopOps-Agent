package com.sirithree.shopops.admin.reliability.domain;

import java.util.Map;
import java.util.Set;

public final class WriteOperationStatus {
    public static final String CREATED = "CREATED";
    public static final String WAITING_APPROVAL = "WAITING_APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String EXECUTING = "EXECUTING";
    public static final String EXTERNAL_UNKNOWN = "EXTERNAL_UNKNOWN";
    public static final String EXTERNAL_SUCCEEDED = "EXTERNAL_SUCCEEDED";
    public static final String LOCAL_CONFIRMED = "LOCAL_CONFIRMED";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    public static final String NEEDS_RECONCILIATION = "NEEDS_RECONCILIATION";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            CREATED, Set.of(WAITING_APPROVAL, APPROVED, FAILED),
            WAITING_APPROVAL, Set.of(APPROVED, FAILED),
            APPROVED, Set.of(EXECUTING, FAILED),
            EXECUTING, Set.of(EXTERNAL_UNKNOWN, EXTERNAL_SUCCEEDED, FAILED),
            EXTERNAL_UNKNOWN, Set.of(EXTERNAL_SUCCEEDED, FAILED, NEEDS_RECONCILIATION),
            EXTERNAL_SUCCEEDED, Set.of(LOCAL_CONFIRMED, NEEDS_RECONCILIATION),
            LOCAL_CONFIRMED, Set.of(SUCCEEDED, NEEDS_RECONCILIATION),
            NEEDS_RECONCILIATION, Set.of(EXTERNAL_SUCCEEDED, LOCAL_CONFIRMED, FAILED),
            FAILED, Set.of(),
            SUCCEEDED, Set.of()
    );

    public static void requireTransition(String from, String to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("非法写操作状态转换: " + from + " -> " + to);
        }
    }

    private WriteOperationStatus() {}
}
