package com.sirithree.shopops.admin.model.service;

import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.common.api.CommonPage;

public interface ModelCallLogStore {
    ModelCallLogDto save(ModelCallLogDto log);

    CommonPage<ModelCallLogDto> list(Long tenantId, Long shopId, ModelCallLogQueryParam query);
}
