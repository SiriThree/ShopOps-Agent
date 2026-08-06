package com.sirithree.shopops.admin.agent.domain;

import java.util.Set;

public enum AgentTaskStatus {
    PENDING,
    QUEUED,
    RUNNING,
    WAITING_APPROVAL,
    RETRYING,
    SUCCEEDED,
    FAILED,
    CANCEL_REQUESTED,
    CANCELLED,
    NEEDS_MANUAL_ACTION;

    public boolean canTransitTo(AgentTaskStatus target) {
        if (target == null || target == this) return false;
        return switch (this) {
            case PENDING -> Set.of(QUEUED, RUNNING, CANCEL_REQUESTED, FAILED).contains(target);
            case QUEUED -> Set.of(RUNNING, RETRYING, CANCEL_REQUESTED, FAILED, NEEDS_MANUAL_ACTION).contains(target);
            case RUNNING -> Set.of(WAITING_APPROVAL, RETRYING, SUCCEEDED, FAILED, CANCEL_REQUESTED, NEEDS_MANUAL_ACTION).contains(target);
            case WAITING_APPROVAL -> Set.of(QUEUED, RUNNING, CANCEL_REQUESTED, FAILED, NEEDS_MANUAL_ACTION).contains(target);
            case RETRYING -> Set.of(QUEUED, RUNNING, FAILED, CANCEL_REQUESTED, NEEDS_MANUAL_ACTION).contains(target);
            case CANCEL_REQUESTED -> Set.of(CANCELLED, NEEDS_MANUAL_ACTION).contains(target);
            case SUCCEEDED, FAILED, CANCELLED, NEEDS_MANUAL_ACTION -> false;
        };
    }

    public boolean terminal() {
        return Set.of(SUCCEEDED, FAILED, CANCELLED, NEEDS_MANUAL_ACTION).contains(this);
    }
}
