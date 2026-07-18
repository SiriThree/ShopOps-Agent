package com.sirithree.shopops.admin.report.service;

import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import java.util.Optional;
import java.util.Map;

public interface OperationReportService {
    OperationReportDto createDailyReviewReport(Long tenantId, Long shopId, Long taskId, Long userId, String traceId, Map<String, Object> reportData);

    Optional<OperationReportDto> getReport(Long tenantId, Long shopId, Long reportId);
}
