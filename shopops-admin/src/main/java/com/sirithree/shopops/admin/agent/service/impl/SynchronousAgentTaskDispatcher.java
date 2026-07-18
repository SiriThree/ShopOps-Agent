package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.AgentEngineService;
import com.sirithree.shopops.admin.agent.service.AgentTaskDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.agent.dispatch-mode", havingValue = "sync", matchIfMissing = true)
public class SynchronousAgentTaskDispatcher implements AgentTaskDispatcher {
    private final AgentEngineService agentEngineService;

    public SynchronousAgentTaskDispatcher(AgentEngineService agentEngineService) {
        this.agentEngineService = agentEngineService;
    }

    @Override
    public AgentExecutionResult dispatch(AgentTaskContext context) {
        return agentEngineService.executeTask(context);
    }
}
