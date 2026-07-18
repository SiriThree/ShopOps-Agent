package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskStatus;

public class TaskStatusTransitionValidator {
    private TaskStatusTransitionValidator() {
    }

    public static AgentTaskStatus parse(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return AgentTaskStatus.valueOf(status);
    }

    public static void requireTransition(String fromStatus, AgentTaskStatus toStatus) {
        AgentTaskStatus from = parse(fromStatus);
        if (from == null || !from.canTransitTo(toStatus)) {
            throw new IllegalStateException("Illegal task status transition: " + fromStatus + " -> " + toStatus);
        }
    }
}
