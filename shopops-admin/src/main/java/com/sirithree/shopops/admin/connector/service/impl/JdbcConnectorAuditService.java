package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventQueryParam;
import com.sirithree.shopops.admin.connector.service.ConnectorAuditService;
import com.sirithree.shopops.admin.persistence.mapper.ConnectorAuditEventMapper;
import com.sirithree.shopops.admin.persistence.model.ConnectorAuditEvent;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcConnectorAuditService implements ConnectorAuditService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectorAuditService.class);

    private final ConnectorAuditEventMapper connectorAuditEventMapper;
    private final JacksonJsonSupport jsonSupport;

    public JdbcConnectorAuditService(ConnectorAuditEventMapper connectorAuditEventMapper,
                                     JacksonJsonSupport jsonSupport) {
        this.connectorAuditEventMapper = connectorAuditEventMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public void record(ConnectorAuditEventCreateCommand command) {
        if (command == null) {
            return;
        }
        try {
            ConnectorAuditEvent event = new ConnectorAuditEvent();
            event.setTenantId(command.getTenantId());
            event.setShopId(command.getShopId());
            event.setUserId(command.getUserId());
            event.setUsername(command.getUsername());
            event.setConnectorCode(command.getConnectorCode());
            event.setEventType(command.getEventType());
            event.setEventStatus(command.getEventStatus());
            event.setRequestId(command.getRequestId());
            event.setMessage(command.getMessage());
            event.setDetailJson(jsonSupport.toJson(command.getDetail() == null ? Map.of() : command.getDetail()));
            event.setCreatedAt(command.getCreatedAt() == null ? LocalDateTime.now() : command.getCreatedAt());
            connectorAuditEventMapper.insert(event);
        } catch (Exception ex) {
            LOGGER.warn("Failed to record connector audit event type={} connectorCode={} requestId={}",
                    command.getEventType(), command.getConnectorCode(), command.getRequestId(), ex);
        }
    }

    @Override
    public CommonPage<ConnectorAuditEventDto> listEvents(Long tenantId, Long shopId, ConnectorAuditEventQueryParam param) {
        ConnectorAuditEventQueryParam query = param == null ? new ConnectorAuditEventQueryParam() : param;
        var list = connectorAuditEventMapper.listByPage(tenantId, shopId, query, query.offset(), query.safePageSize()).stream()
                .map(this::toDto)
                .toList();
        long total = connectorAuditEventMapper.countByPage(tenantId, shopId, query);
        return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total);
    }

    private ConnectorAuditEventDto toDto(ConnectorAuditEvent event) {
        ConnectorAuditEventDto dto = new ConnectorAuditEventDto();
        dto.setEventId(event.getId());
        dto.setTenantId(event.getTenantId());
        dto.setShopId(event.getShopId());
        dto.setUserId(event.getUserId());
        dto.setUsername(event.getUsername());
        dto.setConnectorCode(event.getConnectorCode());
        dto.setEventType(event.getEventType());
        dto.setEventStatus(event.getEventStatus());
        dto.setRequestId(event.getRequestId());
        dto.setMessage(event.getMessage());
        dto.setDetail(jsonSupport.toMap(event.getDetailJson()));
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }
}
