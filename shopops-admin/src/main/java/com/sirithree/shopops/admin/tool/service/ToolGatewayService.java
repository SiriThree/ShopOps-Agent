package com.sirithree.shopops.admin.tool.service;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;

public interface ToolGatewayService {
    ToolInvokeResult invoke(ToolInvokeContext context, String toolCode, Object input);
}
