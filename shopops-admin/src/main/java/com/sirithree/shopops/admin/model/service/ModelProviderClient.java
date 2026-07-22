package com.sirithree.shopops.admin.model.service;

import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;

public interface ModelProviderClient {
    String providerCode();

    String defaultModelName();

    ModelInvokeResult invoke(ModelInvokeParam param);
}
