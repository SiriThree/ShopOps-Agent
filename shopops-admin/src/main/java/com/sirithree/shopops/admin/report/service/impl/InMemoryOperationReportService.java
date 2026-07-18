package com.sirithree.shopops.admin.report.service.impl;

import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryOperationReportService implements OperationReportService {
    private final AtomicLong idGenerator = new AtomicLong(90001);
    private final Map<Long, OperationReportDto> reports = new ConcurrentHashMap<>();

    @Override
    public OperationReportDto createDailyReviewReport(Long tenantId, Long shopId, Long taskId, Long userId, String traceId, Map<String, Object> reportData) {
        Long id = idGenerator.getAndIncrement();
        OperationReportDto report = new OperationReportDto();
        report.setReportId(id);
        report.setTaskId(taskId);
        report.setReportNo("RPT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + id);
        report.setReportType("daily_review");
        report.setTitle((String) reportData.get("title"));
        report.setMarkdown((String) reportData.get("markdown"));
        report.setEvidence(reportData.get("evidence"));
        report.setTraceId(traceId);
        report.setStatus("SUCCESS");
        reports.put(id, report);
        return report;
    }

    @Override
    public Optional<OperationReportDto> getReport(Long tenantId, Long shopId, Long reportId) {
        return Optional.ofNullable(reports.get(reportId));
    }
}
