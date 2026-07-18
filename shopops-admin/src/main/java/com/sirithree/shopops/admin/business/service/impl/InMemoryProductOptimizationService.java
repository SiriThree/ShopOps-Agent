package com.sirithree.shopops.admin.business.service.impl;

import com.sirithree.shopops.admin.business.service.ProductOptimizationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryProductOptimizationService implements ProductOptimizationService {
    @Override
    public Map<String, Object> queryCandidates(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer limit) {
        List<Map<String, Object>> products = List.of(
                Map.of("productId", 1001, "productName", "轻量保温杯 500ml", "reason", "库存高但区间销量偏低", "score", 82.5),
                Map.of("productId", 1008, "productName", "便携收纳箱", "reason", "低星或风险评价集中", "score", 78.0),
                Map.of("productId", 1016, "productName", "运动毛巾", "reason", "标题长度不在推荐区间", "score", 73.5)
        );
        int safeLimit = limit == null || limit <= 0 ? products.size() : Math.min(limit, products.size());
        return Map.of(
                "candidateCount", safeLimit,
                "products", products.subList(0, safeLimit)
        );
    }
}
