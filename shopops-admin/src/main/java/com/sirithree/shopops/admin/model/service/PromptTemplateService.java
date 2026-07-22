package com.sirithree.shopops.admin.model.service;

import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.PromptEnableParam;
import com.sirithree.shopops.admin.model.domain.PromptRenderResult;
import com.sirithree.shopops.admin.model.domain.PromptRenderTestParam;
import com.sirithree.shopops.admin.model.domain.PromptTemplateDto;
import com.sirithree.shopops.admin.model.domain.PromptTemplateQueryParam;
import com.sirithree.shopops.admin.model.domain.PromptVersionParam;
import com.sirithree.shopops.common.api.CommonPage;

public interface PromptTemplateService {
    PromptTemplateDto createVersion(Long tenantId, Long userId, String promptCode, PromptVersionParam param);
    PromptTemplateDto enableVersion(Long tenantId, String promptCode, PromptEnableParam param);
    CommonPage<PromptTemplateDto> list(Long tenantId, PromptTemplateQueryParam query);
    PromptTemplateDto get(Long tenantId, String promptCode, String version);
    PromptRenderResult renderTest(Long tenantId, String promptCode, PromptRenderTestParam param);
    ModelInvokeParam renderForInvoke(Long tenantId, ModelInvokeParam param);
}
