package com.sirithree.shopops.admin.report.service.impl;

import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.domain.OperationReportQueryParam;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.common.api.CommonPage;
import java.util.Comparator;
import java.util.List;
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
        report.setTenantId(tenantId);
        report.setShopId(shopId);
        report.setTaskId(taskId);
        report.setReportNo("RPT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + id);
        report.setReportType("daily_review");
        report.setTitle((String) reportData.get("title"));
        report.setMarkdown((String) reportData.get("markdown"));
        report.setEvidence(reportData.get("evidence"));
        report.setTraceId(traceId);
        report.setStatus("SUCCESS");
        report.setCreatedBy(userId);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        reports.put(id, report);
        return report;
    }

    @Override
    public CommonPage<OperationReportDto> listReports(Long tenantId, Long shopId, OperationReportQueryParam param) {
        OperationReportQueryParam query = param == null ? new OperationReportQueryParam() : param;
        List<OperationReportDto> filtered = reports.values().stream()
                .filter(report -> tenantId.equals(report.getTenantId()) && shopId.equals(report.getShopId()))
                .filter(report -> query.getTaskId() == null || query.getTaskId().equals(report.getTaskId()))
                .filter(report -> matches(query.getReportNo(), report.getReportNo()))
                .filter(report -> matches(query.getReportType(), report.getReportType()))
                .filter(report -> matches(query.getTraceId(), report.getTraceId()))
                .filter(report -> matches(query.getStatus(), report.getStatus()))
                .filter(report -> query.getCreatedBy() == null || query.getCreatedBy().equals(report.getCreatedBy()))
                .filter(report -> query.getCreatedStart() == null || (report.getCreatedAt() != null && !report.getCreatedAt().isBefore(query.getCreatedStart())))
                .filter(report -> query.getCreatedEnd() == null || (report.getCreatedAt() != null && !report.getCreatedAt().isAfter(query.getCreatedEnd())))
                .sorted(Comparator.comparing(OperationReportDto::getReportId).reversed())
                .toList();
        List<OperationReportDto> pageList = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(pageList, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    @Override
    public Optional<OperationReportDto> getReport(Long tenantId, Long shopId, Long reportId) {
        return Optional.ofNullable(reports.get(reportId))
                .filter(report -> tenantId.equals(report.getTenantId()) && shopId.equals(report.getShopId()));
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }
}
