package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;

public interface PlanValidator {
    void validate(AgentTaskContext context, AgentPlan plan);
}
