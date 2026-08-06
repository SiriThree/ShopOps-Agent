package com.sirithree.shopops.admin.model.service;

import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.common.api.CommonPage;

public interface ModelGatewayService {
    ModelInvokeResult invoke(Long tenantId, Long shopId, Long userId, String username, ModelInvokeParam param);

    CommonPage<ModelCallLogDto> listLogs(Long tenantId, Long shopId, ModelCallLogQueryParam query);
}
