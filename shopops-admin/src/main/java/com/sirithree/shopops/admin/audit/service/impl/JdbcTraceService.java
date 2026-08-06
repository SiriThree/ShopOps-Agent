package com.sirithree.shopops.admin.audit.service.impl;

import com.sirithree.shopops.admin.audit.domain.TraceSpanCreateCommand;
import com.sirithree.shopops.admin.audit.domain.TraceSpanDto;
import com.sirithree.shopops.admin.audit.service.TraceService;
import com.sirithree.shopops.admin.persistence.mapper.TraceSpanMapper;
import com.sirithree.shopops.admin.persistence.model.TraceSpan;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcTraceService implements TraceService {
    private final TraceSpanMapper traceSpanMapper;

    public JdbcTraceService(TraceSpanMapper traceSpanMapper) {
        this.traceSpanMapper = traceSpanMapper;
    }

    @Override
    public String startSpan(TraceSpanCreateCommand command) {
        TraceSpan span = new TraceSpan();
        span.setTenantId(command.getTenantId());
        span.setShopId(command.getShopId());
        span.setTraceId(command.getTraceId());
        span.setSpanId("sp_" + UUID.randomUUID().toString().replace("-", ""));
        span.setParentSpanId(command.getParentSpanId());
        span.setSpanType(command.getSpanType());
        span.setSpanName(command.getSpanName());
        span.setRefType(command.getRefType());
        span.setRefId(command.getRefId());
        span.setStatus("RUNNING");
        span.setInputSummary(command.getInputSummary());
        span.setStartedAt(LocalDateTime.now());
        traceSpanMapper.insert(span);
        return span.getSpanId();
    }

    @Override
    public void finishSpan(String traceId, String spanId, String status, String outputSummary, String errorMessage) {
        TraceSpan span = new TraceSpan();
        span.setTraceId(traceId);
        span.setSpanId(spanId);
        span.setStatus(status);
        span.setOutputSummary(outputSummary);
        span.setErrorMessage(errorMessage);
        span.setFinishedAt(LocalDateTime.now());
        traceSpanMapper.finish(span);
    }

    @Override
    public List<TraceSpanDto> listSpans(Long tenantId, String traceId) {
        return traceSpanMapper.listByTraceId(tenantId, traceId).stream().map(this::toDto).toList();
    }

    private TraceSpanDto toDto(TraceSpan span) {
        TraceSpanDto dto = new TraceSpanDto();
        dto.setTraceId(span.getTraceId());
        dto.setSpanId(span.getSpanId());
        dto.setParentSpanId(span.getParentSpanId());
        dto.setSpanType(span.getSpanType());
        dto.setSpanName(span.getSpanName());
        dto.setRefType(span.getRefType());
        dto.setRefId(span.getRefId());
        dto.setStatus(span.getStatus());
        dto.setInputSummary(span.getInputSummary());
        dto.setOutputSummary(span.getOutputSummary());
        dto.setLatencyMs(span.getLatencyMs());
        dto.setErrorMessage(span.getErrorMessage());
        return dto;
    }
}
