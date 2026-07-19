package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.tool.domain.ToolCallLogQueryParam;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryToolCallLogService implements ToolCallLogService {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, Map<String, Object>> logs = new ConcurrentHashMap<>();

    @Override
    public Long start(ToolInvokeContext context, String toolCode, Object input) {
        Long id = idGenerator.getAndIncrement();
        Map<String, Object> log = new HashMap<>();
        log.put("id", id);
        log.put("tenantId", context.getTenantId());
        log.put("shopId", context.getShopId());
        log.put("taskId", context.getTaskId());
        log.put("stepId", context.getStepId());
        log.put("traceId", context.getTraceId());
        log.put("toolCode", toolCode);
        log.put("input", input);
        log.put("status", "RUNNING");
        log.put("createdAt", LocalDateTime.now().toString());
        logs.put(id, log);
        return id;
    }

    @Override
    public void success(Long logId, Object output, long latencyMs) {
        Map<String, Object> log = logs.get(logId);
        if (log != null) {
            log.put("status", "SUCCESS");
            log.put("output", output);
            log.put("latencyMs", latencyMs);
        }
    }

    @Override
    public void failed(Long logId, String errorCode, String errorMessage, long latencyMs) {
        Map<String, Object> log = logs.get(logId);
        if (log != null) {
            log.put("status", "FAILED");
            log.put("errorCode", errorCode);
            log.put("errorMessage", errorMessage);
            log.put("latencyMs", latencyMs);
        }
    }

    @Override
    public CommonPage<Map<String, Object>> list(Long tenantId, Long shopId, ToolCallLogQueryParam param) {
        ToolCallLogQueryParam query = param == null ? new ToolCallLogQueryParam() : param;
        List<Map<String, Object>> filtered = logs.values().stream()
                .filter(log -> tenantId.equals(log.get("tenantId")) && shopId.equals(log.get("shopId")))
                .filter(log -> query.getLogId() == null || query.getLogId().equals(log.get("id")))
                .filter(log -> query.getTaskId() == null || query.getTaskId().equals(log.get("taskId")))
                .filter(log -> query.getStatus() == null || query.getStatus().isBlank() || query.getStatus().equals(log.get("status")))
                .filter(log -> query.getToolCode() == null || query.getToolCode().isBlank() || query.getToolCode().equals(log.get("toolCode")))
                .sorted((left, right) -> Long.compare((Long) left.get("id"), (Long) right.get("id")))
                .toList();
        List<Map<String, Object>> pageList = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(pageList, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    @Override
    public List<Map<String, Object>> listByTaskId(Long tenantId, Long shopId, Long taskId) {
        ToolCallLogQueryParam query = new ToolCallLogQueryParam();
        query.setTaskId(taskId);
        query.setPageSize(100);
        return list(tenantId, shopId, query).getList();
    }
}
