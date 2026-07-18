package com.sirithree.shopops.admin.business.service;

import java.time.LocalDate;
import java.util.Map;

public interface OrderMetricsService {
    Map<String, Object> querySummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate);
}
