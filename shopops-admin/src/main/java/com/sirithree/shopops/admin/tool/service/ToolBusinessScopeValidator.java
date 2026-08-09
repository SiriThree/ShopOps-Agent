package com.sirithree.shopops.admin.tool.service;

import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;

/** Production execution-boundary policy for business-object ownership/scope checks. */
public interface ToolBusinessScopeValidator {
    boolean supports(String toolCode);

    /** Throws ToolGovernanceException when the target object is outside the trusted execution scope. */
    void validate(ToolInvokeContext context, McpToolDto tool, Object normalizedInput);
}
