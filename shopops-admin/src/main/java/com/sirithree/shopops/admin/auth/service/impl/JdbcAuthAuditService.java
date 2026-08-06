package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventDto;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventQueryParam;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.persistence.mapper.AuthAuditEventMapper;
import com.sirithree.shopops.admin.persistence.model.AuthAuditEvent;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcAuthAuditService implements AuthAuditService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAuthAuditService.class);

    private final AuthAuditEventMapper authAuditEventMapper;

    public JdbcAuthAuditService(AuthAuditEventMapper authAuditEventMapper) {
        this.authAuditEventMapper = authAuditEventMapper;
    }

    @Override
    public void record(AuthAuditEventCreateCommand command) {
        if (command == null) {
            return;
        }
        try {
            if (command.getCreatedAt() == null) {
                command.setCreatedAt(LocalDateTime.now());
            }
            authAuditEventMapper.insert(command);
        } catch (Exception ex) {
            LOGGER.warn("Failed to record auth audit event type={} status={} requestId={}",
                    command.getEventType(), command.getEventStatus(), command.getRequestId(), ex);
        }
    }

    @Override
    public CommonPage<AuthAuditEventDto> listEvents(Long tenantId, Long shopId, AuthAuditEventQueryParam param) {
        AuthAuditEventQueryParam query = param == null ? new AuthAuditEventQueryParam() : param;
        List<AuthAuditEventDto> list = authAuditEventMapper
                .listByPage(tenantId, shopId, query, query.offset(), query.safePageSize())
                .stream()
                .map(this::toDto)
                .toList();
        Long total = authAuditEventMapper.countByPage(tenantId, shopId, query);
        return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total);
    }

    private AuthAuditEventDto toDto(AuthAuditEvent event) {
        AuthAuditEventDto dto = new AuthAuditEventDto();
        dto.setEventId(event.getId());
        dto.setTenantId(event.getTenantId());
        dto.setShopId(event.getShopId());
        dto.setUserId(event.getUserId());
        dto.setUsername(event.getUsername());
        dto.setEventType(event.getEventType());
        dto.setEventStatus(event.getEventStatus());
        dto.setAuthType(event.getAuthType());
        dto.setRequestId(event.getRequestId());
        dto.setClientIp(event.getClientIp());
        dto.setUserAgent(event.getUserAgent());
        dto.setFailureReason(event.getFailureReason());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }
}
