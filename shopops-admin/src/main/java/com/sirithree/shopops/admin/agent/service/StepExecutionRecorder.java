package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;

public interface StepExecutionRecorder {
    void running(AgentTaskContext context, Long stepId, Object input);

    void success(AgentTaskContext context, Long stepId, Object output);

    void failed(AgentTaskContext context, Long stepId, String errorCode, String errorMessage);
}
