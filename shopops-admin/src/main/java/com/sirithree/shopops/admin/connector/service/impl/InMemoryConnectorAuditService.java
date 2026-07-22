package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorAuditEventQueryParam;
import com.sirithree.shopops.admin.connector.service.ConnectorAuditService;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryConnectorAuditService implements ConnectorAuditService {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<ConnectorAuditEventDto> events = new CopyOnWriteArrayList<>();

    @Override
    public void record(ConnectorAuditEventCreateCommand command) {
        if (command == null) {
            return;
        }
        ConnectorAuditEventDto event = new ConnectorAuditEventDto();
        event.setEventId(idGenerator.getAndIncrement());
        event.setTenantId(command.getTenantId());
        event.setShopId(command.getShopId());
        event.setUserId(command.getUserId());
        event.setUsername(command.getUsername());
        event.setConnectorCode(command.getConnectorCode());
        event.setEventType(command.getEventType());
        event.setEventStatus(command.getEventStatus());
        event.setRequestId(command.getRequestId());
        event.setMessage(command.getMessage());
        event.setDetail(command.getDetail());
        event.setCreatedAt(command.getCreatedAt() == null ? LocalDateTime.now() : command.getCreatedAt());
        events.add(event);
    }

    @Override
    public CommonPage<ConnectorAuditEventDto> listEvents(Long tenantId, Long shopId, ConnectorAuditEventQueryParam param) {
        ConnectorAuditEventQueryParam query = param == null ? new ConnectorAuditEventQueryParam() : param;
        List<ConnectorAuditEventDto> filtered = events.stream()
                .filter(event -> tenantId.equals(event.getTenantId()) && shopId.equals(event.getShopId()))
                .filter(event -> query.getEventId() == null || query.getEventId().equals(event.getEventId()))
                .filter(event -> matches(query.getConnectorCode(), event.getConnectorCode()))
                .filter(event -> matches(query.getEventType(), event.getEventType()))
                .filter(event -> matches(query.getEventStatus(), event.getEventStatus()))
                .filter(event -> query.getUserId() == null || query.getUserId().equals(event.getUserId()))
                .filter(event -> matches(query.getUsername(), event.getUsername()))
                .filter(event -> query.getCreatedStart() == null || !event.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(event -> query.getCreatedEnd() == null || !event.getCreatedAt().isAfter(query.getCreatedEnd()))
                .sorted(Comparator.comparing(ConnectorAuditEventDto::getEventId).reversed())
                .toList();
        List<ConnectorAuditEventDto> pageList = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(pageList, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }
}
