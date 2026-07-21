package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogQueryParam;
import com.sirithree.shopops.admin.connector.service.ConnectorApiCallLogService;
import com.sirithree.shopops.admin.persistence.mapper.ConnectorApiCallLogMapper;
import com.sirithree.shopops.admin.persistence.model.ConnectorApiCallLog;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcConnectorApiCallLogService implements ConnectorApiCallLogService {
    private final ConnectorApiCallLogMapper mapper;
    private final JacksonJsonSupport jsonSupport;

    public JdbcConnectorApiCallLogService(ConnectorApiCallLogMapper mapper, JacksonJsonSupport jsonSupport) {
        this.mapper = mapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public ConnectorApiCallLogDto record(ConnectorApiCallLogCreateCommand command) {
        ConnectorApiCallLog log = new ConnectorApiCallLog();
        log.setTenantId(command.getTenantId());
        log.setShopId(command.getShopId());
        log.setJobId(command.getJobId());
        log.setConnectorCode(command.getConnectorCode());
        log.setRequestMethod(command.getRequestMethod());
        log.setEndpoint(command.getEndpoint());
        log.setRequestTarget(command.getRequestTarget());
        log.setStatus(command.getStatus());
        log.setStatusCode(command.getStatusCode());
        log.setLatencyMs(command.getLatencyMs() == null ? 0L : command.getLatencyMs());
        log.setErrorCode(command.getErrorCode());
        log.setErrorMessage(command.getErrorMessage());
        log.setRequestId(command.getRequestId());
        log.setDetailJson(jsonSupport.toJson(command.getDetail()));
        log.setCreatedAt(command.getCreatedAt() == null ? LocalDateTime.now() : command.getCreatedAt());
        mapper.insert(log);
        return toDto(log);
    }

    @Override
    public CommonPage<ConnectorApiCallLogDto> list(Long tenantId, Long shopId, ConnectorApiCallLogQueryParam param) {
        ConnectorApiCallLogQueryParam query = param == null ? new ConnectorApiCallLogQueryParam() : param;
        return CommonPage.of(
                mapper.listByPage(tenantId, shopId, query, query.offset(), query.safePageSize()).stream()
                        .map(this::toDto)
                        .toList(),
                query.safePageNum(),
                query.safePageSize(),
                mapper.countByPage(tenantId, shopId, query)
        );
    }

    private ConnectorApiCallLogDto toDto(ConnectorApiCallLog log) {
        ConnectorApiCallLogDto dto = new ConnectorApiCallLogDto();
        dto.setLogId(log.getId());
        dto.setTenantId(log.getTenantId());
        dto.setShopId(log.getShopId());
        dto.setJobId(log.getJobId());
        dto.setConnectorCode(log.getConnectorCode());
        dto.setRequestMethod(log.getRequestMethod());
        dto.setEndpoint(log.getEndpoint());
        dto.setRequestTarget(log.getRequestTarget());
        dto.setStatus(log.getStatus());
        dto.setStatusCode(log.getStatusCode());
        dto.setLatencyMs(log.getLatencyMs());
        dto.setErrorCode(log.getErrorCode());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setRequestId(log.getRequestId());
        dto.setDetail(jsonSupport.toMap(log.getDetailJson()));
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
