package com.sirithree.shopops.admin.tool.service;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;

public interface ToolCallLogService {
    Long start(ToolInvokeContext context, String toolCode, Object input);

    void success(Long logId, Object output, long latencyMs);

    void failed(Long logId, String errorCode, String errorMessage, long latencyMs);

    List<Map<String, Object>> listByTaskId(Long taskId);
}
