package com.sirithree.shopops.admin.business.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.business.service.OrderMetricsService;
import com.sirithree.shopops.admin.business.support.ConfiguredFilePathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryOrderMetricsService implements OrderMetricsService {
    private static final TypeReference<List<OrderSummaryRecord>> ORDER_SUMMARY_RECORDS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String orderSummaryFile;

    public InMemoryOrderMetricsService(ObjectMapper objectMapper,
                                       @Value("${shopops.connector.order-summary.file:}") String orderSummaryFile) {
        this.objectMapper = objectMapper;
        this.orderSummaryFile = orderSummaryFile;
    }

    @Override
    public Map<String, Object> querySummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        return fileSummary(tenantId, shopId, startDate, endDate)
                .map(summary -> withSource(summary, "file.order-summary"))
                .orElseGet(() -> withSource(defaultSummary(), "memory.default"));
    }

    private java.util.Optional<Map<String, Object>> fileSummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        if (orderSummaryFile == null || orderSummaryFile.isBlank()) {
            return java.util.Optional.empty();
        }
        Path path = ConfiguredFilePathResolver.resolve(orderSummaryFile);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("订单汇总数据文件不存在: " + path);
        }
        try {
            List<OrderSummaryRecord> records = objectMapper.readValue(path.toFile(), ORDER_SUMMARY_RECORDS);
            return records.stream()
                    .filter(record -> same(record.tenantId(), tenantId))
                    .filter(record -> same(record.shopId(), shopId))
                    .filter(record -> startDate.equals(LocalDate.parse(record.startDate())))
                    .filter(record -> endDate.equals(LocalDate.parse(record.endDate())))
                    .findFirst()
                    .map(OrderSummaryRecord::summary);
        } catch (IOException ex) {
            throw new IllegalArgumentException("订单汇总数据文件读取失败: " + path, ex);
        }
    }

    private Map<String, Object> defaultSummary() {
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

    private Map<String, Object> withSource(Map<String, Object> summary, String source) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>(summary);
        result.put("connectorCode", source);
        return result;
    }

    private boolean same(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private record OrderSummaryRecord(Long tenantId,
                                      Long shopId,
                                      String startDate,
                                      String endDate,
                                      Map<String, Object> summary) {
    }
}
