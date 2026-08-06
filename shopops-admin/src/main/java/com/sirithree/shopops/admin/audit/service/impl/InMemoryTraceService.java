package com.sirithree.shopops.admin.audit.service.impl;

import com.sirithree.shopops.admin.audit.domain.TraceSpanCreateCommand;
import com.sirithree.shopops.admin.audit.domain.TraceSpanDto;
import com.sirithree.shopops.admin.audit.service.TraceService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryTraceService implements TraceService {
    private final Map<String, TraceSpanDto> spans = new ConcurrentHashMap<>();
    private final Map<String, Long> startedAt = new ConcurrentHashMap<>();

    @Override
    public String startSpan(TraceSpanCreateCommand command) {
        String spanId = "sp_" + UUID.randomUUID().toString().replace("-", "");
        TraceSpanDto span = new TraceSpanDto();
        span.setTraceId(command.getTraceId());
        span.setSpanId(spanId);
        span.setParentSpanId(command.getParentSpanId());
        span.setSpanType(command.getSpanType());
        span.setSpanName(command.getSpanName());
        span.setRefType(command.getRefType());
        span.setRefId(command.getRefId());
        span.setStatus("RUNNING");
        span.setInputSummary(command.getInputSummary());
        spans.put(key(command.getTraceId(), spanId), span);
        startedAt.put(key(command.getTraceId(), spanId), System.currentTimeMillis());
        return spanId;
    }

    @Override
    public void finishSpan(String traceId, String spanId, String status, String outputSummary, String errorMessage) {
        TraceSpanDto span = spans.get(key(traceId, spanId));
        if (span == null) {
            return;
        }
        span.setStatus(status);
        span.setOutputSummary(outputSummary);
        span.setErrorMessage(errorMessage);
        Long started = startedAt.get(key(traceId, spanId));
        if (started != null) {
            span.setLatencyMs((int) (System.currentTimeMillis() - started));
        }
    }

    @Override
    public List<TraceSpanDto> listSpans(Long tenantId, String traceId) {
        return spans.values().stream()
                .filter(span -> traceId.equals(span.getTraceId()))
                .toList();
    }

    private String key(String traceId, String spanId) {
        return traceId + ":" + spanId;
    }
}
