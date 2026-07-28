package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;

public interface StepExecutionRecorder {
    default Long ensureStep(AgentTaskContext context, AgentPlanStep step) {
        return context.resolveStepId(step.getStepNo());
    }

    void running(AgentTaskContext context, Long stepId, Object input);

    void success(AgentTaskContext context, Long stepId, Object output);

    void failed(AgentTaskContext context, Long stepId, String errorCode, String errorMessage);
}
