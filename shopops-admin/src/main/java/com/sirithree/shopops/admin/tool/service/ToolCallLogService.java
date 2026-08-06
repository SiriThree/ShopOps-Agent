package com.sirithree.shopops.admin.tool.service;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolCallLogQueryParam;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.List;
import java.util.Map;

public interface ToolCallLogService {
    Long start(ToolInvokeContext context, String toolCode, Object input);

    void success(Long logId, Object output, long latencyMs);

    void successWithGovernanceNote(Long logId, Object output, String riskLevel, String noteCode, String noteMessage, long latencyMs);

    void approvalRequired(Long logId, Long approvalId, String riskLevel, String errorMessage, long latencyMs);

    void failed(Long logId, String errorCode, String errorMessage, long latencyMs);

    CommonPage<Map<String, Object>> list(Long tenantId, Long shopId, ToolCallLogQueryParam param);

    List<Map<String, Object>> listByTaskId(Long tenantId, Long shopId, Long taskId);
}
