package com.sirithree.shopops.admin.model.service.impl;

import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.model.service.ModelCallLogStore;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryModelCallLogStore implements ModelCallLogStore {
    private final AtomicLong callIdGenerator = new AtomicLong(1);
    private final Map<Long, ModelCallLogDto> logs = new ConcurrentHashMap<>();

    @Override
    public ModelCallLogDto save(ModelCallLogDto log) {
        if (log.getCallId() == null) {
            log.setCallId(callIdGenerator.getAndIncrement());
        }
        logs.put(log.getCallId(), log);
        return log;
    }

    @Override
    public CommonPage<ModelCallLogDto> list(Long tenantId, Long shopId, ModelCallLogQueryParam queryParam) {
        ModelCallLogQueryParam query = queryParam == null ? new ModelCallLogQueryParam() : queryParam;
        List<ModelCallLogDto> filtered = logs.values().stream()
                .filter(log -> tenantId.equals(log.getTenantId()) && shopId.equals(log.getShopId()))
                .filter(log -> blank(query.getProviderCode()) || query.getProviderCode().equalsIgnoreCase(log.getProviderCode()))
                .filter(log -> blank(query.getModelName()) || query.getModelName().equalsIgnoreCase(log.getModelName()))
                .filter(log -> blank(query.getStatus()) || query.getStatus().equalsIgnoreCase(log.getStatus()))
                .filter(log -> blank(query.getTraceId()) || query.getTraceId().equals(log.getTraceId()))
                .filter(log -> query.getTaskId() == null || query.getTaskId().equals(log.getTaskId()))
                .sorted(Comparator.comparing(ModelCallLogDto::getCallId).reversed())
                .toList();
        List<ModelCallLogDto> page = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(page, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
