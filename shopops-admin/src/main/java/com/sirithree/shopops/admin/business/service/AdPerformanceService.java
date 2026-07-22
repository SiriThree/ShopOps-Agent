package com.sirithree.shopops.admin.business.service;

import java.time.LocalDate;
import java.util.Map;

public interface AdPerformanceService {
    Map<String, Object> queryPerformance(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate);
}
