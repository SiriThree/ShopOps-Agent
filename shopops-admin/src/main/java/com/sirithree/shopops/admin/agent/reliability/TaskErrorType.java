package com.sirithree.shopops.admin.agent.reliability;

public enum TaskErrorType {
    VALIDATION_ERROR(false, 0, false, false),
    PERMISSION_DENIED(false, 0, false, true),
    BUSINESS_CONFLICT(false, 0, false, true),
    RATE_LIMITED(true, 5, false, false),
    NETWORK_TIMEOUT(true, 3, true, false),
    DEPENDENCY_UNAVAILABLE(true, 5, false, false),
    EXTERNAL_RESULT_UNKNOWN(false, 0, true, true),
    INTERNAL_ERROR(true, 2, false, true);

    private final boolean retryable;
    private final int maxAttempts;
    private final boolean requiresLookup;
    private final boolean manualAfterFailure;

    TaskErrorType(boolean retryable, int maxAttempts, boolean requiresLookup, boolean manualAfterFailure) {
        this.retryable = retryable;
        this.maxAttempts = maxAttempts;
        this.requiresLookup = requiresLookup;
        this.manualAfterFailure = manualAfterFailure;
    }
    public boolean retryable() { return retryable; }
    public int maxAttempts() { return maxAttempts; }
    public boolean requiresLookup() { return requiresLookup; }
    public boolean manualAfterFailure() { return manualAfterFailure; }
}
