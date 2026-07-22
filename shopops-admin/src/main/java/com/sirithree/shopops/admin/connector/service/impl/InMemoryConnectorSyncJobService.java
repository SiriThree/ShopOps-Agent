package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobCreateParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorSyncJobQueryParam;
import com.sirithree.shopops.admin.connector.service.ConnectorSyncJobService;
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
public class InMemoryConnectorSyncJobService implements ConnectorSyncJobService {
    private static final int MAX_ATTEMPTS = 3;

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<ConnectorSyncJobDto> jobs = new CopyOnWriteArrayList<>();
    private final ConnectorSyncJobExecutor executor;

    public InMemoryConnectorSyncJobService(ConnectorSyncJobExecutor executor) {
        this.executor = executor;
    }

    @Override
    public ConnectorSyncJobDto createAndRun(Long tenantId, Long shopId, Long userId, String requestId, ConnectorSyncJobCreateParam param) {
        LocalDateTime now = LocalDateTime.now();
        ConnectorSyncJobDto job = new ConnectorSyncJobDto();
        job.setJobId(idGenerator.getAndIncrement());
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
        run(job, requestId);
        jobs.add(job);
        return copy(job);
    }

    @Override
    public ConnectorSyncJobDto retry(Long tenantId, Long shopId, Long userId, String requestId, Long jobId) {
        ConnectorSyncJobDto job = jobs.stream()
                .filter(item -> tenantId.equals(item.getTenantId()) && shopId.equals(item.getShopId()) && jobId.equals(item.getJobId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("同步任务不存在: " + jobId));
        if (!"FAILED".equals(job.getStatus())) {
            throw new IllegalArgumentException("只有失败的同步任务可以重试");
        }
        if (job.getAttempt() >= job.getMaxAttempts()) {
            throw new IllegalArgumentException("同步任务已达到最大重试次数");
        }
        job.setAttempt(job.getAttempt() + 1);
        job.setTriggerType("RETRY");
        job.setRequestId(requestId);
        job.setStartedAt(LocalDateTime.now());
        run(job, requestId);
        return copy(job);
    }

    @Override
    public CommonPage<ConnectorSyncJobDto> list(Long tenantId, Long shopId, ConnectorSyncJobQueryParam param) {
        ConnectorSyncJobQueryParam query = param == null ? new ConnectorSyncJobQueryParam() : param;
        List<ConnectorSyncJobDto> filtered = jobs.stream()
                .filter(job -> tenantId.equals(job.getTenantId()) && shopId.equals(job.getShopId()))
                .filter(job -> query.getJobId() == null || query.getJobId().equals(job.getJobId()))
                .filter(job -> matches(query.getConnectorCode(), job.getConnectorCode()))
                .filter(job -> matches(query.getStatus(), job.getStatus()))
                .filter(job -> matches(query.getTriggerType(), job.getTriggerType()))
                .filter(job -> query.getCreatedStart() == null || !job.getCreatedAt().isBefore(query.getCreatedStart()))
                .filter(job -> query.getCreatedEnd() == null || !job.getCreatedAt().isAfter(query.getCreatedEnd()))
                .sorted(Comparator.comparing(ConnectorSyncJobDto::getJobId).reversed())
                .toList();
        List<ConnectorSyncJobDto> pageList = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .map(this::copy)
                .toList();
        return CommonPage.of(pageList, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    private void run(ConnectorSyncJobDto job, String requestId) {
        job.setStatus("RUNNING");
        ConnectorSyncJobExecutor.ConnectorSyncResult result = executor.run(
                job.getTenantId(), job.getShopId(), job.getJobId(), job.getConnectorCode(), requestId);
        job.setStatus(result.status());
        job.setMessage(result.message());
        job.setDetail(result.detail());
        job.setFinishedAt(result.finishedAt());
        job.setUpdatedAt(result.finishedAt());
        job.setRequestId(requestId);
    }

    private ConnectorSyncJobDto copy(ConnectorSyncJobDto source) {
        ConnectorSyncJobDto target = new ConnectorSyncJobDto();
        target.setJobId(source.getJobId());
        target.setTenantId(source.getTenantId());
        target.setShopId(source.getShopId());
        target.setConnectorCode(source.getConnectorCode());
        target.setStatus(source.getStatus());
        target.setAttempt(source.getAttempt());
        target.setMaxAttempts(source.getMaxAttempts());
        target.setTriggerType(source.getTriggerType());
        target.setCreatedBy(source.getCreatedBy());
        target.setRequestId(source.getRequestId());
        target.setMessage(source.getMessage());
        target.setDetail(source.getDetail() == null ? Map.of() : new LinkedHashMap<>(source.getDetail()));
        target.setStartedAt(source.getStartedAt());
        target.setFinishedAt(source.getFinishedAt());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
