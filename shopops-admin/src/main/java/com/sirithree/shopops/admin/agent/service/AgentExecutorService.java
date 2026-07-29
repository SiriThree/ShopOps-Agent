package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;

public interface AgentExecutorService {
    AgentExecutionResult execute(AgentTaskContext context, AgentPlan plan);

    default AgentExecutionResult execute(AgentTaskContext context, AgentPlan plan, AgentExecutionResult baseResult) {
        return execute(context, plan);
    }
}
