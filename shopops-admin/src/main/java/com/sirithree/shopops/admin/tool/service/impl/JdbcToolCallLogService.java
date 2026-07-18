package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.ToolCallLogMapper;
import com.sirithree.shopops.admin.persistence.model.ToolCallLog;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcToolCallLogService implements ToolCallLogService {
    private final ToolCallLogMapper toolCallLogMapper;
    private final JacksonJsonSupport jsonSupport;

    public JdbcToolCallLogService(ToolCallLogMapper toolCallLogMapper, JacksonJsonSupport jsonSupport) {
        this.toolCallLogMapper = toolCallLogMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public Long start(ToolInvokeContext context, String toolCode, Object input) {
        ToolCallLog log = new ToolCallLog();
        log.setTenantId(context.getTenantId());
        log.setShopId(context.getShopId());
        log.setTaskId(context.getTaskId());
        log.setStepId(context.getStepId());
        log.setTraceId(context.getTraceId());
        log.setSpanId("sp_" + UUID.randomUUID().toString().replace("-", ""));
        log.setUserId(context.getUserId());
        log.setToolCode(toolCode);
        log.setInputJson(jsonSupport.toJson(input));
        log.setStatus("RUNNING");
        log.setRetryCount(0);
        log.setCreatedAt(LocalDateTime.now());
        toolCallLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public void success(Long logId, Object output, long latencyMs) {
        ToolCallLog log = new ToolCallLog();
        log.setId(logId);
        log.setStatus("SUCCESS");
        log.setOutputJson(jsonSupport.toJson(output));
        log.setLatencyMs((int) latencyMs);
        toolCallLogMapper.finish(log);
    }

    @Override
    public void failed(Long logId, String errorCode, String errorMessage, long latencyMs) {
        ToolCallLog log = new ToolCallLog();
        log.setId(logId);
        log.setStatus("FAILED");
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);
        log.setLatencyMs((int) latencyMs);
        toolCallLogMapper.finish(log);
    }

    @Override
    public List<Map<String, Object>> listByTaskId(Long taskId) {
        return toolCallLogMapper.listByTaskId(taskId).stream()
                .map(log -> Map.<String, Object>of(
                        "id", log.getId(),
                        "taskId", log.getTaskId(),
                        "stepId", log.getStepId(),
                        "traceId", log.getTraceId(),
                        "spanId", log.getSpanId(),
                        "toolCode", log.getToolCode(),
                        "status", log.getStatus(),
                        "input", jsonSupport.toMap(log.getInputJson()),
                        "output", jsonSupport.toMap(log.getOutputJson()),
                        "latencyMs", log.getLatencyMs() == null ? 0 : log.getLatencyMs()
                ))
                .toList();
    }
}
