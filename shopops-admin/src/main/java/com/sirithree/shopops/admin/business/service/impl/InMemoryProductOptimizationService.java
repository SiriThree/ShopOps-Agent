package com.sirithree.shopops.admin.business.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.business.service.ProductOptimizationService;
import com.sirithree.shopops.admin.business.support.ConfiguredFilePathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryProductOptimizationService implements ProductOptimizationService {
    private static final TypeReference<List<ProductCandidateRecord>> PRODUCT_CANDIDATE_RECORDS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String productCandidatesFile;

    public InMemoryProductOptimizationService(ObjectMapper objectMapper,
                                              @Value("${shopops.connector.product-candidates.file:}") String productCandidatesFile) {
        this.objectMapper = objectMapper;
        this.productCandidatesFile = productCandidatesFile;
    }

    @Override
    public Map<String, Object> queryCandidates(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer limit) {
        Optional<Map<String, Object>> fileSummary = fileSummary(tenantId, shopId, startDate, endDate, limit);
        if (fileSummary.isPresent()) {
            return withSource(fileSummary.get(), "file.product-candidates");
        }
        return withSource(defaultSummary(limit), "memory.default");
    }

    private Optional<Map<String, Object>> fileSummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer limit) {
        if (productCandidatesFile == null || productCandidatesFile.isBlank()) {
            return Optional.empty();
        }
        Path path = ConfiguredFilePathResolver.resolve(productCandidatesFile);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("商品优化数据文件不存在: " + path);
        }
        try {
            List<ProductCandidateRecord> records = objectMapper.readValue(path.toFile(), PRODUCT_CANDIDATE_RECORDS);
            return records.stream()
                    .filter(record -> same(record.tenantId(), tenantId))
                    .filter(record -> same(record.shopId(), shopId))
                    .filter(record -> startDate.equals(LocalDate.parse(record.startDate())))
                    .filter(record -> endDate.equals(LocalDate.parse(record.endDate())))
                    .findFirst()
                    .map(record -> limitSummary(record.summary(), limit));
        } catch (IOException ex) {
            throw new IllegalArgumentException("商品优化数据文件读取失败: " + path, ex);
        }
    }

    private Map<String, Object> defaultSummary(Integer limit) {
        List<Map<String, Object>> products = List.of(
                Map.of("productId", 1001, "productName", "轻量保温杯 500ml", "reason", "库存高但区间销量偏低", "score", 82.5),
                Map.of("productId", 1008, "productName", "便携收纳箱", "reason", "低星或风险评价集中", "score", 78.0),
                Map.of("productId", 1016, "productName", "运动毛巾", "reason", "标题长度不在推荐区间", "score", 73.5)
        );
        return Map.of(
                "candidateCount", safeLimit(limit, products.size()),
                "products", products.subList(0, safeLimit(limit, products.size()))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> limitSummary(Map<String, Object> summary, Integer limit) {
        Object value = summary.get("products");
        if (!(value instanceof List<?> products)) {
            return summary;
        }
        int safeLimit = safeLimit(limit, products.size());
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>(summary);
        result.put("candidateCount", safeLimit);
        result.put("products", ((List<Object>) products).subList(0, safeLimit));
        return result;
    }

    private Map<String, Object> withSource(Map<String, Object> summary, String source) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>(summary);
        result.put("connectorCode", source);
        return result;
    }

    private int safeLimit(Integer limit, int max) {
        return limit == null || limit <= 0 ? max : Math.min(limit, max);
    }

    private boolean same(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private record ProductCandidateRecord(Long tenantId,
                                          Long shopId,
                                          String startDate,
                                          String endDate,
                                          Map<String, Object> summary) {
    }
}
