package com.sirithree.shopops.admin.agent.domain;

import java.util.Set;

public enum AgentTaskStatus {
    CREATED,
    RUNNING,
    SUCCESS,
    FAILED,
    DEGRADED;

    public boolean canTransitTo(AgentTaskStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case CREATED -> Set.of(RUNNING, FAILED).contains(target);
            case RUNNING -> Set.of(SUCCESS, FAILED, DEGRADED).contains(target);
            case SUCCESS, FAILED, DEGRADED -> false;
        };
    }
}
