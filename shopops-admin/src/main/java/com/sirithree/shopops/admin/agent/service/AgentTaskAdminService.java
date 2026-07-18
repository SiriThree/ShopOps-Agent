package com.sirithree.shopops.admin.agent.service;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDetailDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventQueryParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskMetricsDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.Optional;

public interface AgentTaskAdminService {
    CommonPage<AgentTaskDto> listTasks(Long tenantId, Long shopId, AgentTaskQueryParam param);

    CommonPage<AgentTaskEventDto> listEvents(Long tenantId, Long shopId, AgentTaskEventQueryParam param);

    Optional<AgentTaskDetailDto> getTaskDetail(Long tenantId, Long shopId, Long taskId);

    AgentTaskMetricsDto getTaskMetrics(Long tenantId, Long shopId);
}
