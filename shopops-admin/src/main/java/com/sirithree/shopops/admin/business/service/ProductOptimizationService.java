package com.sirithree.shopops.admin.business.service;

import java.time.LocalDate;
import java.util.Map;

public interface ProductOptimizationService {
    Map<String, Object> queryCandidates(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer limit);
}
