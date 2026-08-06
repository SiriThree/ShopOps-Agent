package com.sirithree.shopops.admin.tool.service;

import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;

public interface ToolProvider {
    boolean supports(McpToolDto tool);

    ToolInvokeResult invoke(ToolInvokeContext context, McpToolDto tool, Object input);
}
