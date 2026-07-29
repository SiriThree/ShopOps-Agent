package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskInterpretation;
import com.sirithree.shopops.admin.agent.domain.DateRangeParam;

public interface AgentTaskInterpreter {
    AgentTaskInterpretation interpret(String userInput, DateRangeParam dateRange);
}
