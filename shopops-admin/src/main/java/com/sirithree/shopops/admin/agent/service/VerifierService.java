package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentVerificationResult;

public interface VerifierService {
    AgentVerificationResult verify(AgentTaskContext context, AgentExecutionResult result);
}
