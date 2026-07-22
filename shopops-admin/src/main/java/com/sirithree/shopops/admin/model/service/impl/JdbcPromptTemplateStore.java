package com.sirithree.shopops.admin.model.service.impl;

import com.sirithree.shopops.admin.model.domain.PromptTemplateDto;
import com.sirithree.shopops.admin.model.domain.PromptTemplateQueryParam;
import com.sirithree.shopops.admin.model.service.PromptTemplateStore;
import com.sirithree.shopops.admin.persistence.mapper.PromptTemplateMapper;
import com.sirithree.shopops.admin.persistence.model.PromptTemplate;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcPromptTemplateStore implements PromptTemplateStore {
    private final PromptTemplateMapper promptTemplateMapper;

    public JdbcPromptTemplateStore(PromptTemplateMapper promptTemplateMapper) {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @Override
    public PromptTemplateDto save(PromptTemplateDto template) {
        PromptTemplate model = toModel(template);
        promptTemplateMapper.insert(model);
        template.setPromptId(model.getId());
        return template;
    }

    @Override
    public CommonPage<PromptTemplateDto> list(Long tenantId, PromptTemplateQueryParam queryParam) {
        PromptTemplateQueryParam query = queryParam == null ? new PromptTemplateQueryParam() : queryParam;
        List<PromptTemplateDto> list = promptTemplateMapper.listByPage(
                tenantId, query, query.offset(), query.safePageSize()).stream()
                .map(this::toDto)
                .toList();
        long total = promptTemplateMapper.countByPage(tenantId, query);
        return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total);
    }

    @Override
    public Optional<PromptTemplateDto> findByCodeAndVersion(Long tenantId, String promptCode, String version) {
        return Optional.ofNullable(promptTemplateMapper.findByCodeAndVersion(tenantId, promptCode, version))
                .map(this::toDto);
    }

    @Override
    public Optional<PromptTemplateDto> findActive(Long tenantId, String promptCode) {
        return Optional.ofNullable(promptTemplateMapper.findActive(tenantId, promptCode))
                .map(this::toDto);
    }

    @Override
    public int deactivateCode(Long tenantId, String promptCode) {
        return promptTemplateMapper.deactivateCode(tenantId, promptCode);
    }

    @Override
    @Transactional
    public int activateVersion(Long tenantId, String promptCode, String version) {
        deactivateCode(tenantId, promptCode);
        int updated = promptTemplateMapper.activateVersion(tenantId, promptCode, version);
        if (updated == 0) {
            throw new IllegalArgumentException("Prompt 版本不存在: " + promptCode + "@" + version);
        }
        return updated;
    }

    private PromptTemplate toModel(PromptTemplateDto dto) {
        PromptTemplate model = new PromptTemplate();
        model.setId(dto.getPromptId());
        model.setTenantId(dto.getTenantId());
        model.setPromptCode(dto.getPromptCode());
        model.setPromptName(dto.getPromptName());
        model.setTaskType(dto.getTaskType());
        model.setTemplateContent(dto.getTemplateContent());
        model.setVersion(dto.getVersion());
        model.setStatus(dto.getStatus());
        model.setCreatedBy(dto.getCreatedBy());
        model.setCreatedAt(dto.getCreatedAt());
        model.setUpdatedAt(dto.getUpdatedAt());
        return model;
    }

    private PromptTemplateDto toDto(PromptTemplate model) {
        PromptTemplateDto dto = new PromptTemplateDto();
        dto.setPromptId(model.getId());
        dto.setTenantId(model.getTenantId());
        dto.setPromptCode(model.getPromptCode());
        dto.setPromptName(model.getPromptName());
        dto.setTaskType(model.getTaskType());
        dto.setTemplateContent(model.getTemplateContent());
        dto.setVersion(model.getVersion());
        dto.setStatus(model.getStatus());
        dto.setCreatedBy(model.getCreatedBy());
        dto.setCreatedAt(model.getCreatedAt());
        dto.setUpdatedAt(model.getUpdatedAt());
        return dto;
    }
}
