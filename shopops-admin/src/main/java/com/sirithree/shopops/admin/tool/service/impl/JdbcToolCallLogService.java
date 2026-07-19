package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.ToolCallLogMapper;
import com.sirithree.shopops.admin.persistence.model.ToolCallLog;
import com.sirithree.shopops.admin.tool.domain.ToolCallLogQueryParam;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.service.ToolCallLogService;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
    public CommonPage<Map<String, Object>> list(Long tenantId, Long shopId, ToolCallLogQueryParam param) {
        ToolCallLogQueryParam query = param == null ? new ToolCallLogQueryParam() : param;
        List<Map<String, Object>> list = toolCallLogMapper.listByPage(
                        tenantId,
                        shopId,
                        query.getLogId(),
                        query.getTaskId(),
                        query.getStatus(),
                        query.getToolCode(),
                        query.offset(),
                        query.safePageSize()
                ).stream()
                .map(this::toMap)
                .toList();
        Long total = toolCallLogMapper.countByPage(
                tenantId,
                shopId,
                query.getLogId(),
                query.getTaskId(),
                query.getStatus(),
                query.getToolCode()
        );
        return CommonPage.of(list, query.safePageNum(), query.safePageSize(), total);
    }

    @Override
    public List<Map<String, Object>> listByTaskId(Long tenantId, Long shopId, Long taskId) {
        ToolCallLogQueryParam query = new ToolCallLogQueryParam();
        query.setTaskId(taskId);
        query.setPageSize(100);
        return list(tenantId, shopId, query).getList();
    }

    private Map<String, Object> toMap(ToolCallLog log) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", log.getId());
        result.put("taskId", log.getTaskId());
        result.put("stepId", log.getStepId());
        result.put("traceId", log.getTraceId());
        result.put("spanId", log.getSpanId());
        result.put("toolCode", log.getToolCode());
        result.put("status", log.getStatus());
        result.put("riskLevel", log.getRiskLevel());
        result.put("input", jsonSupport.toMap(log.getInputJson()));
        result.put("output", jsonSupport.toMap(log.getOutputJson()));
        result.put("latencyMs", log.getLatencyMs() == null ? 0 : log.getLatencyMs());
        result.put("retryCount", log.getRetryCount() == null ? 0 : log.getRetryCount());
        result.put("errorCode", log.getErrorCode());
        result.put("errorMessage", log.getErrorMessage());
        result.put("createdAt", log.getCreatedAt());
        return result;
    }
}
