package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventDto;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventQueryParam;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
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
public class InMemoryAuthAuditService implements AuthAuditService {
    private final AtomicLong sequence = new AtomicLong(1L);
    private final List<AuthAuditEventDto> events = new CopyOnWriteArrayList<>();

    @Override
    public void record(AuthAuditEventCreateCommand command) {
        if (command == null) {
            return;
        }
        AuthAuditEventDto event = new AuthAuditEventDto();
        event.setEventId(sequence.getAndIncrement());
        event.setTenantId(command.getTenantId());
        event.setShopId(command.getShopId());
        event.setUserId(command.getUserId());
        event.setUsername(command.getUsername());
        event.setEventType(command.getEventType());
        event.setEventStatus(command.getEventStatus());
        event.setAuthType(command.getAuthType());
        event.setRequestId(command.getRequestId());
        event.setClientIp(command.getClientIp());
        event.setUserAgent(command.getUserAgent());
        event.setFailureReason(command.getFailureReason());
        event.setCreatedAt(command.getCreatedAt() == null ? LocalDateTime.now() : command.getCreatedAt());
        events.add(event);
    }

    @Override
    public CommonPage<AuthAuditEventDto> listEvents(Long tenantId, Long shopId, AuthAuditEventQueryParam param) {
        AuthAuditEventQueryParam query = param == null ? new AuthAuditEventQueryParam() : param;
        List<AuthAuditEventDto> filtered = events.stream()
                .filter(event -> tenantId.equals(event.getTenantId()))
                .filter(event -> shopId.equals(event.getShopId()))
                .filter(event -> query.getEventId() == null || query.getEventId().equals(event.getEventId()))
                .filter(event -> matches(query.getEventType(), event.getEventType()))
                .filter(event -> matches(query.getEventStatus(), event.getEventStatus()))
                .filter(event -> query.getUserId() == null || query.getUserId().equals(event.getUserId()))
                .filter(event -> matches(query.getUsername(), event.getUsername()))
                .filter(event -> matches(query.getRequestId(), event.getRequestId()))
                .filter(event -> query.getCreatedStart() == null || !event.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(event -> query.getCreatedEnd() == null || !event.getCreatedAt().isAfter(query.getCreatedEnd()))
                .sorted(Comparator.comparing(AuthAuditEventDto::getEventId).reversed())
                .toList();
        List<AuthAuditEventDto> pageList = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(pageList, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }
}
