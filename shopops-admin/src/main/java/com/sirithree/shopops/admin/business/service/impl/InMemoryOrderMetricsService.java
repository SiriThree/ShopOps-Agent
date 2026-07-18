package com.sirithree.shopops.admin.business.service.impl;

import com.sirithree.shopops.admin.business.service.OrderMetricsService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryOrderMetricsService implements OrderMetricsService {
    @Override
    public Map<String, Object> querySummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        return Map.of(
                "gmv", 128936.50,
                "orderCount", 842,
                "refundAmount", 5360.00,
                "refundRate", 0.0416,
                "avgOrderAmount", 153.13,
                "compareYesterday", Map.of("gmvGrowth", 0.083, "orderGrowth", 0.057),
                "compareSevenDayAvg", Map.of("gmvGrowth", 0.026, "refundRateDelta", -0.004)
        );
    }
}
