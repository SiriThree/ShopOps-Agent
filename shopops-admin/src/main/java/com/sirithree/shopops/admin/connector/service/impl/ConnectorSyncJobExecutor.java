package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorStatusDto;
import com.sirithree.shopops.admin.connector.service.ConnectorStatusService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ConnectorSyncJobExecutor {
    private final ConnectorStatusService connectorStatusService;

    public ConnectorSyncJobExecutor(ConnectorStatusService connectorStatusService) {
        this.connectorStatusService = connectorStatusService;
    }

    public ConnectorSyncResult run(Long tenantId, Long shopId, String connectorCode) {
        ConnectorStatusDto status = connectorStatusService.listStatus(tenantId, shopId).stream()
                .filter(item -> connectorCode.equals(item.getConnectorCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("连接器不存在: " + connectorCode));
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("connectorStatus", status.getStatus());
        detail.put("configured", status.isConfigured());
        detail.put("available", status.isAvailable());
        detail.put("propertyKey", status.getPropertyKey());
        detail.put("configuredPath", status.getConfiguredPath());
        detail.put("checkedAt", status.getLastCheckedAt());
        if (status.isAvailable()) {
            return new ConnectorSyncResult("SUCCESS", "连接器同步检查通过", detail, LocalDateTime.now());
        }
        return new ConnectorSyncResult("FAILED", status.getMessage(), detail, LocalDateTime.now());
    }

    public record ConnectorSyncResult(String status,
                                      String message,
                                      Map<String, Object> detail,
                                      LocalDateTime finishedAt) {
    }
}
