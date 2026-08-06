package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskRecoveryResult;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.List;
import java.util.Optional;

public interface AgentTaskService {
    AgentTaskCreateResult createTask(Long tenantId, Long shopId, Long userId, AgentTaskCreateParam param);

    AgentTaskDto cancelTask(Long tenantId, Long shopId, Long userId, Long taskId, String reason);

    AgentTaskCreateResult retryTask(Long tenantId, Long shopId, Long userId, Long taskId);

    AgentTaskRecoveryResult requeueStaleTasks(Long tenantId, Long shopId, Long userId, Integer queuedTimeoutMinutes, Integer runningTimeoutMinutes, Integer limit);

    CommonPage<AgentTaskDto> listTasks(Long tenantId, Long shopId, AgentTaskQueryParam param);

    Optional<AgentTaskDto> getTask(Long tenantId, Long shopId, Long taskId);

    List<AgentTaskStepDto> listSteps(Long tenantId, Long shopId, Long taskId);

    List<AgentTaskEventDto> listEvents(Long tenantId, Long shopId, Long taskId);
}
