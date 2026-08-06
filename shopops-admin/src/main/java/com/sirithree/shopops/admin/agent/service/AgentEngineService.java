package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;

public interface AgentEngineService {
    AgentExecutionResult executeTask(AgentTaskContext context);
}
