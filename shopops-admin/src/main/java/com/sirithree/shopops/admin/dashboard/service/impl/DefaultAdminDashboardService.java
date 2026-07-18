package com.sirithree.shopops.admin.dashboard.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventQueryParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskMetricsDto;
import com.sirithree.shopops.admin.agent.service.AgentTaskAdminService;
import com.sirithree.shopops.admin.dashboard.domain.AdminDashboardSummaryDto;
import com.sirithree.shopops.admin.dashboard.service.AdminDashboardService;
import com.sirithree.shopops.admin.report.domain.OperationReportQueryParam;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.admin.tool.domain.ToolCallLogQueryParam;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class DefaultAdminDashboardService implements AdminDashboardService {
    private final AgentTaskAdminService agentTaskAdminService;
    private final OperationReportService operationReportService;
    private final ToolCallLogService toolCallLogService;

    public DefaultAdminDashboardService(AgentTaskAdminService agentTaskAdminService,
                                        OperationReportService operationReportService,
                                        ToolCallLogService toolCallLogService) {
        this.agentTaskAdminService = agentTaskAdminService;
        this.operationReportService = operationReportService;
        this.toolCallLogService = toolCallLogService;
    }

    @Override
    public AdminDashboardSummaryDto getSummary(Long tenantId, Long shopId) {
        AgentTaskMetricsDto taskMetrics = agentTaskAdminService.getTaskMetrics(tenantId, shopId);
        AdminDashboardSummaryDto summary = new AdminDashboardSummaryDto();
        summary.setTaskMetrics(taskMetrics);
        summary.setReportTotal(reportTotal(tenantId, shopId));
        summary.setToolCallTotal(toolCallTotal(tenantId, shopId, null));
        summary.setToolCallFailed(toolCallTotal(tenantId, shopId, "FAILED"));
        summary.setRecentFailedEvents(recentFailedEvents(tenantId, shopId).getList());
        summary.setGeneratedAt(LocalDateTime.now());
        return summary;
    }

    private long reportTotal(Long tenantId, Long shopId) {
        OperationReportQueryParam query = new OperationReportQueryParam();
        query.setPageNum(1);
        query.setPageSize(1);
        return operationReportService.listReports(tenantId, shopId, query).getTotal();
    }

    private long toolCallTotal(Long tenantId, Long shopId, String status) {
        ToolCallLogQueryParam query = new ToolCallLogQueryParam();
        query.setStatus(status);
        query.setPageNum(1);
        query.setPageSize(1);
        return toolCallLogService.list(tenantId, shopId, query).getTotal();
    }

    private CommonPage<AgentTaskEventDto> recentFailedEvents(Long tenantId, Long shopId) {
        AgentTaskEventQueryParam query = new AgentTaskEventQueryParam();
        query.setEventType("TASK_FAILED");
        query.setPageNum(1);
        query.setPageSize(5);
        return agentTaskAdminService.listEvents(tenantId, shopId, query);
    }
}
