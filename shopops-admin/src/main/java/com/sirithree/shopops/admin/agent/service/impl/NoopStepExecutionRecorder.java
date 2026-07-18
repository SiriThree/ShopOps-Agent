package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.StepExecutionRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class NoopStepExecutionRecorder implements StepExecutionRecorder {
    @Override
    public void running(AgentTaskContext context, Long stepId, Object input) {
    }

    @Override
    public void success(AgentTaskContext context, Long stepId, Object output) {
    }

    @Override
    public void failed(AgentTaskContext context, Long stepId, String errorCode, String errorMessage) {
    }
}
