package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskSpec;

public interface PlannerService {
    AgentPlan createPlan(AgentTaskContext context);

    AgentPlan previewPlan(AgentTaskSpec taskSpec);
}
