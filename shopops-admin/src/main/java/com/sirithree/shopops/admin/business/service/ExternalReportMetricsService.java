package com.sirithree.shopops.admin.business.service;

import java.time.LocalDate;
import java.util.Map;

public interface ExternalReportMetricsService {
    Map<String, Object> queryMetrics(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate);
}
