package com.sirithree.shopops.admin.audit.service;

import com.sirithree.shopops.admin.audit.domain.TraceSpanCreateCommand;
import com.sirithree.shopops.admin.audit.domain.TraceSpanDto;
import java.util.List;

public interface TraceService {
    String startSpan(TraceSpanCreateCommand command);

    void finishSpan(String traceId, String spanId, String status, String outputSummary, String errorMessage);

    List<TraceSpanDto> listSpans(Long tenantId, String traceId);
}
