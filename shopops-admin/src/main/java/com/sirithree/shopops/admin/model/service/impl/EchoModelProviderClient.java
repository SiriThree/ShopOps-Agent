package com.sirithree.shopops.admin.model.service.impl;

import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelProviderClient;
import org.springframework.stereotype.Component;

@Component
public class EchoModelProviderClient implements ModelProviderClient {
    @Override
    public String providerCode() {
        return "echo";
    }

    @Override
    public String defaultModelName() {
        return "echo-001";
    }

    @Override
    public ModelInvokeResult invoke(ModelInvokeParam param) {
        ModelInvokeResult result = new ModelInvokeResult();
        result.setProviderCode(providerCode());
        result.setModelName(defaultModelName());
        result.setOutputText("Echo model response: " + param.getPrompt());
        result.setStatus(ModelCallStatus.SUCCESS);
        return result;
    }
}
