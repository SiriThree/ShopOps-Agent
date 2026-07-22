package com.sirithree.shopops.admin.model.service.impl;

import com.sirithree.shopops.admin.model.domain.PromptTemplateDto;
import com.sirithree.shopops.admin.model.domain.PromptTemplateQueryParam;
import com.sirithree.shopops.admin.model.domain.PromptTemplateStatus;
import com.sirithree.shopops.admin.model.service.PromptTemplateStore;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryPromptTemplateStore implements PromptTemplateStore {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, PromptTemplateDto> templates = new ConcurrentHashMap<>();

    @Override
    public PromptTemplateDto save(PromptTemplateDto template) {
        if (template.getPromptId() == null) {
            template.setPromptId(idGenerator.getAndIncrement());
        }
        templates.put(template.getPromptId(), template);
        return template;
    }

    @Override
    public CommonPage<PromptTemplateDto> list(Long tenantId, PromptTemplateQueryParam queryParam) {
        PromptTemplateQueryParam query = queryParam == null ? new PromptTemplateQueryParam() : queryParam;
        List<PromptTemplateDto> filtered = templates.values().stream()
                .filter(template -> tenantId.equals(template.getTenantId()))
                .filter(template -> blank(query.getPromptCode()) || query.getPromptCode().equals(template.getPromptCode()))
                .filter(template -> blank(query.getTaskType()) || query.getTaskType().equals(template.getTaskType()))
                .filter(template -> blank(query.getStatus()) || query.getStatus().equals(template.getStatus()))
                .sorted(Comparator.comparing(PromptTemplateDto::getPromptId).reversed())
                .toList();
        List<PromptTemplateDto> page = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(page, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    @Override
    public Optional<PromptTemplateDto> findByCodeAndVersion(Long tenantId, String promptCode, String version) {
        return templates.values().stream()
                .filter(template -> tenantId.equals(template.getTenantId()))
                .filter(template -> promptCode.equals(template.getPromptCode()))
                .filter(template -> version.equals(template.getVersion()))
                .findFirst();
    }

    @Override
    public Optional<PromptTemplateDto> findActive(Long tenantId, String promptCode) {
        return templates.values().stream()
                .filter(template -> tenantId.equals(template.getTenantId()))
                .filter(template -> promptCode.equals(template.getPromptCode()))
                .filter(template -> PromptTemplateStatus.ACTIVE.equals(template.getStatus()))
                .max(Comparator.comparing(PromptTemplateDto::getPromptId));
    }

    @Override
    public int deactivateCode(Long tenantId, String promptCode) {
        int count = 0;
        for (PromptTemplateDto template : templates.values()) {
            if (tenantId.equals(template.getTenantId()) && promptCode.equals(template.getPromptCode())
                    && PromptTemplateStatus.ACTIVE.equals(template.getStatus())) {
                template.setStatus(PromptTemplateStatus.DRAFT);
                template.setUpdatedAt(LocalDateTime.now());
                count++;
            }
        }
        return count;
    }

    @Override
    public int activateVersion(Long tenantId, String promptCode, String version) {
        PromptTemplateDto target = findByCodeAndVersion(tenantId, promptCode, version)
                .orElseThrow(() -> new IllegalArgumentException("Prompt 版本不存在: " + promptCode + "@" + version));
        deactivateCode(tenantId, promptCode);
        target.setStatus(PromptTemplateStatus.ACTIVE);
        target.setUpdatedAt(LocalDateTime.now());
        return 1;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
