package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogCreateCommand;
import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorApiCallLogQueryParam;
import com.sirithree.shopops.admin.connector.service.ConnectorApiCallLogService;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryConnectorApiCallLogService implements ConnectorApiCallLogService {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<ConnectorApiCallLogDto> logs = new CopyOnWriteArrayList<>();

    @Override
    public ConnectorApiCallLogDto record(ConnectorApiCallLogCreateCommand command) {
        ConnectorApiCallLogDto log = new ConnectorApiCallLogDto();
        log.setLogId(idGenerator.getAndIncrement());
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
        log.setDetail(command.getDetail() == null ? Map.of() : new LinkedHashMap<>(command.getDetail()));
        log.setCreatedAt(command.getCreatedAt() == null ? LocalDateTime.now() : command.getCreatedAt());
        logs.add(log);
        return copy(log);
    }

    @Override
    public CommonPage<ConnectorApiCallLogDto> list(Long tenantId, Long shopId, ConnectorApiCallLogQueryParam param) {
        ConnectorApiCallLogQueryParam query = param == null ? new ConnectorApiCallLogQueryParam() : param;
        List<ConnectorApiCallLogDto> filtered = logs.stream()
                .filter(log -> tenantId.equals(log.getTenantId()) && shopId.equals(log.getShopId()))
                .filter(log -> query.getLogId() == null || query.getLogId().equals(log.getLogId()))
                .filter(log -> query.getJobId() == null || query.getJobId().equals(log.getJobId()))
                .filter(log -> matches(query.getConnectorCode(), log.getConnectorCode()))
                .filter(log -> matches(query.getEndpoint(), log.getEndpoint()))
                .filter(log -> matches(query.getStatus(), log.getStatus()))
                .filter(log -> query.getCreatedStart() == null || !log.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(log -> query.getCreatedEnd() == null || !log.getCreatedAt().isAfter(query.getCreatedEnd()))
                .sorted(Comparator.comparing(ConnectorApiCallLogDto::getLogId).reversed())
                .toList();
        return CommonPage.of(
                filtered.stream().skip(query.offset()).limit(query.safePageSize()).map(this::copy).toList(),
                query.safePageNum(),
                query.safePageSize(),
                (long) filtered.size()
        );
    }

    private ConnectorApiCallLogDto copy(ConnectorApiCallLogDto source) {
        ConnectorApiCallLogDto target = new ConnectorApiCallLogDto();
        target.setLogId(source.getLogId());
        target.setTenantId(source.getTenantId());
        target.setShopId(source.getShopId());
        target.setJobId(source.getJobId());
        target.setConnectorCode(source.getConnectorCode());
        target.setRequestMethod(source.getRequestMethod());
        target.setEndpoint(source.getEndpoint());
        target.setRequestTarget(source.getRequestTarget());
        target.setStatus(source.getStatus());
        target.setStatusCode(source.getStatusCode());
        target.setLatencyMs(source.getLatencyMs());
        target.setErrorCode(source.getErrorCode());
        target.setErrorMessage(source.getErrorMessage());
        target.setRequestId(source.getRequestId());
        target.setDetail(source.getDetail() == null ? Map.of() : new LinkedHashMap<>(source.getDetail()));
        target.setCreatedAt(source.getCreatedAt());
        return target;
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }
}
