package com.sirithree.shopops.admin.agent.governance;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionMode;
import java.util.Set;

public record WorkflowTemplate(
        String workflowType,
        Set<String> allowedTools,
        int maxSteps,
        String maxRiskLevel,
        Set<AgentExecutionMode> allowedModes,
        int maxRepairAttempts,
        long totalTimeoutMs) {
}
