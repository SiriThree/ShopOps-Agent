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
    public static final String NEEDS_MANUAL_ACTION = "NEEDS_MANUAL_ACTION";

    private static final Map<String, Set<String>> TRANSITIONS = Map.ofEntries(
            Map.entry(CREATED, Set.of(WAITING_APPROVAL, APPROVED, FAILED)),
            Map.entry(WAITING_APPROVAL, Set.of(APPROVED, FAILED)),
            Map.entry(APPROVED, Set.of(EXECUTING, FAILED)),
            Map.entry(EXECUTING, Set.of(EXTERNAL_UNKNOWN, EXTERNAL_SUCCEEDED, FAILED, NEEDS_RECONCILIATION, NEEDS_MANUAL_ACTION)),
            Map.entry(EXTERNAL_UNKNOWN, Set.of(EXTERNAL_SUCCEEDED, FAILED, NEEDS_RECONCILIATION, NEEDS_MANUAL_ACTION)),
            Map.entry(EXTERNAL_SUCCEEDED, Set.of(LOCAL_CONFIRMED, NEEDS_RECONCILIATION, NEEDS_MANUAL_ACTION)),
            Map.entry(LOCAL_CONFIRMED, Set.of(SUCCEEDED, NEEDS_RECONCILIATION, NEEDS_MANUAL_ACTION)),
            Map.entry(NEEDS_RECONCILIATION, Set.of(EXTERNAL_SUCCEEDED, LOCAL_CONFIRMED, FAILED, NEEDS_MANUAL_ACTION)),
            Map.entry(NEEDS_MANUAL_ACTION, Set.of()),
            Map.entry(FAILED, Set.of()),
            Map.entry(SUCCEEDED, Set.of())
    );

    public static void requireTransition(String from, String to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("非法写操作状态转换: " + from + " -> " + to);
        }
    }

    public static boolean terminal(String status) {
        return SUCCEEDED.equals(status) || FAILED.equals(status) || NEEDS_MANUAL_ACTION.equals(status);
    }

    private WriteOperationStatus() {}
}
