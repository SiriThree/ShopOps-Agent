package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobCreateParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobQueryParam;
import com.sirithree.shopops.admin.connector.service.ConnectorSyncJobService;
import com.sirithree.shopops.admin.persistence.mapper.ConnectorSyncJobMapper;
import com.sirithree.shopops.admin.persistence.model.ConnectorSyncJob;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcConnectorSyncJobService implements ConnectorSyncJobService {
    private static final int MAX_ATTEMPTS = 3;

    private final ConnectorSyncJobMapper mapper;
    private final ConnectorSyncJobExecutor executor;
    private final JacksonJsonSupport jsonSupport;

    public JdbcConnectorSyncJobService(ConnectorSyncJobMapper mapper,
                                       ConnectorSyncJobExecutor executor,
                                       JacksonJsonSupport jsonSupport) {
        this.mapper = mapper;
        this.executor = executor;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public ConnectorSyncJobDto createAndRun(Long tenantId, Long shopId, Long userId, String requestId, ConnectorSyncJobCreateParam param) {
        LocalDateTime now = LocalDateTime.now();
        ConnectorSyncJob job = new ConnectorSyncJob();
        job.setTenantId(tenantId);
        job.setShopId(shopId);
        job.setConnectorCode(normalize(param.getConnectorCode()));
        job.setStatus("RUNNING");
        job.setAttempt(1);
        job.setMaxAttempts(MAX_ATTEMPTS);
        job.setTriggerType("MANUAL");
        job.setCreatedBy(userId);
        job.setRequestId(requestId);
        job.setMessage(blankToDefault(param.getRemark(), "手动同步"));
        job.setStartedAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        mapper.insert(job);
        run(job, requestId);
        mapper.updateResult(job);
        return toDto(job);
    }

    @Override
    public ConnectorSyncJobDto retry(Long tenantId, Long shopId, Long userId, String requestId, Long jobId) {
        ConnectorSyncJob job = mapper.find(tenantId, shopId, jobId);
        if (job == null) {
            throw new IllegalArgumentException("同步任务不存在: " + jobId);
        }
        boolean resumablePage = "SUCCESS".equals(job.getStatus()) && job.getCursorValue() != null && !job.getCursorValue().isBlank();
        if (!"FAILED".equals(job.getStatus()) && !resumablePage) {
            throw new IllegalArgumentException("只有失败任务或存在下一页游标的任务可以恢复");
        }
        if (job.getAttempt() >= job.getMaxAttempts()) {
            throw new IllegalArgumentException("同步任务已达到最大重试次数");
        }
        job.setAttempt(job.getAttempt() + 1);
        job.setTriggerType("RETRY");
        job.setRequestId(requestId);
        job.setStartedAt(LocalDateTime.now());
        run(job, requestId);
        mapper.updateResult(job);
        return toDto(job);
    }

    @Override
    public CommonPage<ConnectorSyncJobDto> list(Long tenantId, Long shopId, ConnectorSyncJobQueryParam param) {
        ConnectorSyncJobQueryParam query = param == null ? new ConnectorSyncJobQueryParam() : param;
        return CommonPage.of(
                mapper.listByPage(tenantId, shopId, query, query.offset(), query.safePageSize()).stream()
                        .map(this::toDto)
                        .toList(),
                query.safePageNum(),
                query.safePageSize(),
                mapper.countByPage(tenantId, shopId, query)
        );
    }

    private void run(ConnectorSyncJob job, String requestId) {
        job.setStatus("RUNNING");
        ConnectorSyncJobExecutor.ConnectorSyncResult result = executor.run(
                job.getTenantId(), job.getShopId(), job.getId(), job.getConnectorCode(), requestId, job.getCursorValue());
        job.setStatus(result.status());
        job.setMessage(result.message());
        job.setDetailJson(jsonSupport.toJson(result.detail()));
        job.setCursorValue(result.nextCursor());
        Map<String, Object> checkpoint = new java.util.LinkedHashMap<>();
        checkpoint.put("cursor", result.nextCursor());
        checkpoint.put("detail", result.detail());
        job.setCheckpointJson(jsonSupport.toJson(checkpoint));
        job.setErrorType(result.errorType());
        job.setFinishedAt(result.finishedAt());
        job.setUpdatedAt(result.finishedAt());
        job.setRequestId(requestId);
    }

    private ConnectorSyncJobDto toDto(ConnectorSyncJob job) {
        ConnectorSyncJobDto dto = new ConnectorSyncJobDto();
        dto.setJobId(job.getId());
        dto.setTenantId(job.getTenantId());
        dto.setShopId(job.getShopId());
        dto.setConnectorCode(job.getConnectorCode());
        dto.setStatus(job.getStatus());
        dto.setAttempt(job.getAttempt());
        dto.setMaxAttempts(job.getMaxAttempts());
        dto.setTriggerType(job.getTriggerType());
        dto.setCreatedBy(job.getCreatedBy());
        dto.setRequestId(job.getRequestId());
        dto.setMessage(job.getMessage());
        dto.setDetail(jsonSupport.toMap(job.getDetailJson()));
        dto.setStartedAt(job.getStartedAt());
        dto.setFinishedAt(job.getFinishedAt());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
