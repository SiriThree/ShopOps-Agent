package com.sirithree.shopops.admin.model.service;

import com.sirithree.shopops.admin.model.domain.PromptTemplateDto;
import com.sirithree.shopops.admin.model.domain.PromptTemplateQueryParam;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.Optional;

public interface PromptTemplateStore {
    PromptTemplateDto save(PromptTemplateDto template);
    CommonPage<PromptTemplateDto> list(Long tenantId, PromptTemplateQueryParam query);
    Optional<PromptTemplateDto> findByCodeAndVersion(Long tenantId, String promptCode, String version);
    Optional<PromptTemplateDto> findActive(Long tenantId, String promptCode);
    int deactivateCode(Long tenantId, String promptCode);
    int activateVersion(Long tenantId, String promptCode, String version);
}
