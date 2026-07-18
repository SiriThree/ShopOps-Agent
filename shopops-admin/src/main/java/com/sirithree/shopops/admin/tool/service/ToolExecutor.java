package com.sirithree.shopops.admin.tool.service;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;

public interface ToolExecutor {
    String toolCode();

    ToolInvokeResult execute(ToolInvokeContext context, Object input);
}
