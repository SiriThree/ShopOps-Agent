package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskStatus;

public class TaskStatusTransitionValidator {
    private TaskStatusTransitionValidator() {
    }

    public static AgentTaskStatus parse(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return switch (status.trim().toUpperCase()) {
                case "CREATED" -> AgentTaskStatus.PENDING;
                case "SUCCESS" -> AgentTaskStatus.SUCCEEDED;
                case "DEGRADED" -> AgentTaskStatus.NEEDS_MANUAL_ACTION;
                default -> AgentTaskStatus.valueOf(status.trim().toUpperCase());
            };
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unknown task status: " + status, ex);
        }
    }

    public static void requireTransition(String fromStatus, AgentTaskStatus toStatus) {
        AgentTaskStatus from = parse(fromStatus);
        if (from == null || !from.canTransitTo(toStatus)) {
            throw new IllegalStateException("Illegal task status transition: " + fromStatus + " -> " + toStatus);
        }
    }
}
