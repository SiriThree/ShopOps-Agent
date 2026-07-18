package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public List<Map<String, Object>> listByTaskId(Long taskId) {
        return logs.values().stream()
                .filter(log -> taskId.equals(log.get("taskId")))
                .sorted((left, right) -> Long.compare((Long) left.get("id"), (Long) right.get("id")))
                .toList();
    }
}
