package com.sirithree.shopops.admin.business.service.impl;

import com.sirithree.shopops.admin.business.domain.OrderSummaryData;
import com.sirithree.shopops.admin.business.service.OrderMetricsService;
import com.sirithree.shopops.admin.persistence.mapper.BusinessOrderMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcOrderMetricsService implements OrderMetricsService {
    private final BusinessOrderMapper orderMapper;

    public JdbcOrderMetricsService(BusinessOrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Map<String, Object> querySummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endExclusiveAt = endDate.plusDays(1).atStartOfDay();
        OrderSummaryData current = orderMapper.querySummary(tenantId, shopId, startAt, endExclusiveAt);

        long days = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate.plusDays(1)));
        OrderSummaryData yesterday = orderMapper.querySummary(
                tenantId,
                shopId,
                startDate.minusDays(days).atStartOfDay(),
                startDate.atStartOfDay()
        );
        LocalDateTime sevenStart = startDate.minusDays(7).atStartOfDay();
        LocalDateTime sevenEnd = startDate.atStartOfDay();
        BigDecimal sevenDayAvgGmv = defaultZero(orderMapper.queryAvgDailyGmv(tenantId, shopId, sevenStart, sevenEnd));
        OrderSummaryData sevenDays = orderMapper.querySummary(tenantId, shopId, sevenStart, sevenEnd);

        return Map.of(
                "gmv", money(current.getGmv()),
                "orderCount", current.getOrderCount(),
                "refundAmount", money(current.getRefundAmount()),
                "refundRate", rate(current.getRefundRate()),
                "avgOrderAmount", money(current.getAvgOrderAmount()),
                "compareYesterday", Map.of(
                        "gmvGrowth", growth(current.getGmv(), yesterday.getGmv()),
                        "orderGrowth", growth(BigDecimal.valueOf(current.getOrderCount()), BigDecimal.valueOf(yesterday.getOrderCount()))
                ),
                "compareSevenDayAvg", Map.of(
                        "gmvGrowth", growth(current.getGmv(), sevenDayAvgGmv),
                        "refundRateDelta", rate(current.getRefundRate().subtract(sevenDays.getRefundRate()))
                )
        );
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double money(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double rate(BigDecimal value) {
        return defaultZero(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private double growth(BigDecimal current, BigDecimal baseline) {
        BigDecimal safeCurrent = defaultZero(current);
        BigDecimal safeBaseline = defaultZero(baseline);
        if (safeBaseline.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) == 0 ? 0.0 : 1.0;
        }
        return safeCurrent.subtract(safeBaseline)
                .divide(safeBaseline, 4, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
