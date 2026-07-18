package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import java.util.List;
import java.util.Optional;

public interface AgentTaskService {
    AgentTaskCreateResult createTask(Long tenantId, Long shopId, Long userId, AgentTaskCreateParam param);

    Optional<AgentTaskDto> getTask(Long tenantId, Long shopId, Long taskId);

    List<AgentTaskStepDto> listSteps(Long tenantId, Long shopId, Long taskId);
}
