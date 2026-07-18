package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDetailDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskMetricsDto;
import java.util.Optional;

public interface AgentTaskAdminService {
    Optional<AgentTaskDetailDto> getTaskDetail(Long tenantId, Long shopId, Long taskId);

    AgentTaskMetricsDto getTaskMetrics(Long tenantId, Long shopId);
}
