package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorStatusDto;
import com.sirithree.shopops.admin.connector.service.ConnectorApiCallLogService;
import com.sirithree.shopops.admin.connector.service.ConnectorStatusService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ConnectorSyncJobExecutor {
    private final ConnectorStatusService connectorStatusService;
    private final ConnectorApiCallLogService apiCallLogService;

    public ConnectorSyncJobExecutor(ConnectorStatusService connectorStatusService,
                                    ConnectorApiCallLogService apiCallLogService) {
        this.connectorStatusService = connectorStatusService;
        this.apiCallLogService = apiCallLogService;
    }

    public ConnectorSyncResult run(Long tenantId, Long shopId, Long jobId, String connectorCode, String requestId) {
        long started = System.nanoTime();
        ConnectorStatusDto status = connectorStatusService.listStatus(tenantId, shopId).stream()
                .filter(item -> connectorCode.equals(item.getConnectorCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("连接器不存在: " + connectorCode));
        long latencyMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
        Map<String, Object> detail = detail(status);
        String resultStatus = status.isAvailable() ? "SUCCESS" : "FAILED";
        String message = status.isAvailable() ? "连接器同步检查通过" : status.getMessage();
        recordApiCall(tenantId, shopId, jobId, connectorCode, requestId, status, resultStatus, message, latencyMs, detail);
        return new ConnectorSyncResult(resultStatus, message, detail, LocalDateTime.now());
    }

    private void recordApiCall(Long tenantId,
                               Long shopId,
                               Long jobId,
                               String connectorCode,
                               String requestId,
                               ConnectorStatusDto status,
                               String resultStatus,
                               String message,
                               long latencyMs,
                               Map<String, Object> detail) {
        ConnectorApiCallLogCreateCommand command = new ConnectorApiCallLogCreateCommand();
        command.setTenantId(tenantId);
        command.setShopId(shopId);
        command.setJobId(jobId);
        command.setConnectorCode(connectorCode);
        command.setRequestMethod("CHECK");
        command.setEndpoint("connector.status.check");
        command.setRequestTarget(status.getConfiguredPath() == null || status.getConfiguredPath().isBlank()
                ? status.getPropertyKey()
                : status.getConfiguredPath());
        command.setStatus(resultStatus);
        command.setStatusCode(status.isAvailable() ? 200 : 503);
        command.setLatencyMs(latencyMs);
        command.setErrorCode(status.isAvailable() ? null : status.getStatus());
        command.setErrorMessage(status.isAvailable() ? null : message);
        command.setRequestId(requestId);
        command.setDetail(detail);
        apiCallLogService.record(command);
    }

    private Map<String, Object> detail(ConnectorStatusDto status) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("connectorStatus", status.getStatus());
        detail.put("configured", status.isConfigured());
        detail.put("available", status.isAvailable());
        detail.put("propertyKey", status.getPropertyKey());
        detail.put("configuredPath", status.getConfiguredPath());
        detail.put("checkedAt", status.getLastCheckedAt());
        return detail;
    }

    public record ConnectorSyncResult(String status,
                                      String message,
                                      Map<String, Object> detail,
                                      LocalDateTime finishedAt) {
    }
}
