package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.StepExecutionRecorder;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.AgentTaskStepMapper;
import com.sirithree.shopops.admin.persistence.model.AgentTaskStep;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcStepExecutionRecorder implements StepExecutionRecorder {
    private final AgentTaskStepMapper agentTaskStepMapper;
    private final JacksonJsonSupport jsonSupport;

    public JdbcStepExecutionRecorder(AgentTaskStepMapper agentTaskStepMapper, JacksonJsonSupport jsonSupport) {
        this.agentTaskStepMapper = agentTaskStepMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public void running(AgentTaskContext context, Long stepId, Object input) {
        AgentTaskStep step = base(context, stepId);
        step.setStatus("RUNNING");
        step.setInputJson(jsonSupport.toJson(input));
        step.setStartedAt(LocalDateTime.now());
        agentTaskStepMapper.updateExecutionState(step);
    }

    @Override
    public void success(AgentTaskContext context, Long stepId, Object output) {
        AgentTaskStep step = base(context, stepId);
        step.setStatus("SUCCESS");
        step.setOutputJson(jsonSupport.toJson(output));
        step.setFinishedAt(LocalDateTime.now());
        agentTaskStepMapper.updateExecutionState(step);
    }

    @Override
    public void failed(AgentTaskContext context, Long stepId, String errorCode, String errorMessage) {
        AgentTaskStep step = base(context, stepId);
        step.setStatus("FAILED");
        step.setErrorCode(errorCode);
        step.setErrorMessage(errorMessage);
        step.setFinishedAt(LocalDateTime.now());
        agentTaskStepMapper.updateExecutionState(step);
    }

    private AgentTaskStep base(AgentTaskContext context, Long stepId) {
        AgentTaskStep step = new AgentTaskStep();
        step.setId(stepId);
        step.setTenantId(context.getTenantId());
        step.setShopId(context.getShopId());
        return step;
    }
}
