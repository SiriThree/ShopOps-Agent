package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentDispatchResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;

public interface AgentTaskDispatcher {
    boolean isAsynchronous();

    AgentDispatchResult dispatch(AgentTaskContext context);
}
