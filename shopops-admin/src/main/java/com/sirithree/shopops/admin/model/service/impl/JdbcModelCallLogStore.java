package com.sirithree.shopops.admin.model.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.model.service.ModelCallLogStore;
import com.sirithree.shopops.admin.persistence.mapper.ModelCallLogMapper;
import com.sirithree.shopops.admin.persistence.model.ModelCallLog;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcModelCallLogStore implements ModelCallLogStore {
    private final ModelCallLogMapper modelCallLogMapper;
    private final JacksonJsonSupport jsonSupport;

    public JdbcModelCallLogStore(ModelCallLogMapper modelCallLogMapper, JacksonJsonSupport jsonSupport) {
        this.modelCallLogMapper = modelCallLogMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public ModelCallLogDto save(ModelCallLogDto log) {
        ModelCallLog model = toModel(log);
        modelCallLogMapper.insert(model);
        log.setCallId(model.getId());
        return log;
    }

    @Override
    public CommonPage<ModelCallLogDto> list(Long tenantId, Long shopId, ModelCallLogQueryParam queryParam) {
        ModelCallLogQueryParam query = queryParam == null ? new ModelCallLogQueryParam() : queryParam;
        List<ModelCallLogDto> list = modelCallLogMapper.listByPage(
                        tenantId,
                        shopId,
                        query,
                        query.offset(),
                        query.safePageSize()
                ).stream()
                .map(this::toDto)
                .toList();
        Long total = modelCallLogMapper.countByPage(tenantId, shopId, query);
        return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total);
    }

    private ModelCallLog toModel(ModelCallLogDto dto) {
        ModelCallLog model = new ModelCallLog();
        model.setTenantId(dto.getTenantId());
        model.setShopId(dto.getShopId());
        model.setUserId(dto.getUserId());
        model.setUsername(dto.getUsername());
        model.setProviderCode(dto.getProviderCode());
        model.setModelName(dto.getModelName());
        model.setPromptCode(dto.getPromptCode());
        model.setPromptVersion(dto.getPromptVersion());
        model.setTraceId(dto.getTraceId());
        model.setTaskId(dto.getTaskId());
        model.setReportId(dto.getReportId());
        model.setStatus(dto.getStatus());
        model.setPromptTokens(dto.getPromptTokens());
        model.setCompletionTokens(dto.getCompletionTokens());
        model.setTotalTokens(dto.getTotalTokens());
        model.setLatencyMs(dto.getLatencyMs());
        model.setErrorCode(dto.getErrorCode());
        model.setErrorMessage(dto.getErrorMessage());
        model.setPromptPreview(dto.getPromptPreview());
        model.setOutputPreview(dto.getOutputPreview());
        model.setMetadataJson(jsonSupport.toJson(dto.getMetadata() == null ? Map.of() : dto.getMetadata()));
        model.setCreatedAt(dto.getCreatedAt() == null ? LocalDateTime.now() : dto.getCreatedAt());
        return model;
    }

    private ModelCallLogDto toDto(ModelCallLog model) {
        ModelCallLogDto dto = new ModelCallLogDto();
        dto.setCallId(model.getId());
        dto.setTenantId(model.getTenantId());
        dto.setShopId(model.getShopId());
        dto.setUserId(model.getUserId());
        dto.setUsername(model.getUsername());
        dto.setProviderCode(model.getProviderCode());
        dto.setModelName(model.getModelName());
        dto.setPromptCode(model.getPromptCode());
        dto.setPromptVersion(model.getPromptVersion());
        dto.setTraceId(model.getTraceId());
        dto.setTaskId(model.getTaskId());
        dto.setReportId(model.getReportId());
        dto.setStatus(model.getStatus());
        dto.setPromptTokens(model.getPromptTokens());
        dto.setCompletionTokens(model.getCompletionTokens());
        dto.setTotalTokens(model.getTotalTokens());
        dto.setLatencyMs(model.getLatencyMs());
        dto.setErrorCode(model.getErrorCode());
        dto.setErrorMessage(model.getErrorMessage());
        dto.setPromptPreview(model.getPromptPreview());
        dto.setOutputPreview(model.getOutputPreview());
        dto.setMetadata(jsonSupport.toMap(model.getMetadataJson()));
        dto.setCreatedAt(model.getCreatedAt());
        return dto;
    }
}
