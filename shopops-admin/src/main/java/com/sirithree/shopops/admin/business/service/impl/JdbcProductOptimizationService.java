package com.sirithree.shopops.admin.business.service.impl;

import com.sirithree.shopops.admin.business.domain.ProductCandidateRow;
import com.sirithree.shopops.admin.business.service.ProductOptimizationService;
import com.sirithree.shopops.admin.business.support.BusinessFileSummaryReader;
import com.sirithree.shopops.admin.persistence.mapper.BusinessProductMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcProductOptimizationService implements ProductOptimizationService {
    private final BusinessProductMapper productMapper;
    private final BusinessFileSummaryReader fileSummaryReader;

    public JdbcProductOptimizationService(BusinessProductMapper productMapper, BusinessFileSummaryReader fileSummaryReader) {
        this.productMapper = productMapper;
        this.fileSummaryReader = fileSummaryReader;
    }

    @Override
    public Map<String, Object> queryCandidates(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer limit) {
        java.util.Optional<Map<String, Object>> fileSummary =
                fileSummaryReader.productCandidates(tenantId, shopId, startDate, endDate, limit);
        if (fileSummary.isPresent()) {
            return fileSummary.get();
        }

        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endExclusiveAt = endDate.plusDays(1).atStartOfDay();
        int safeLimit = limit == null || limit <= 0 ? 10 : limit;
        List<Map<String, Object>> products = productMapper.listCandidateRows(tenantId, shopId, startAt, endExclusiveAt).stream()
                .map(this::toCandidate)
                .filter(candidate -> ((Number) candidate.get("score")).doubleValue() > 0)
                .sorted(Comparator.comparingDouble(candidate -> -((Number) candidate.get("score")).doubleValue()))
                .limit(safeLimit)
                .toList();

        return Map.of(
                "candidateCount", products.size(),
                "products", products
        );
    }

    private Map<String, Object> toCandidate(ProductCandidateRow row) {
        List<String> reasons = new ArrayList<>();
        double score = 0.0;
        long salesQuantity = row.getSalesQuantity() == null ? 0L : row.getSalesQuantity();
        int stock = row.getStock() == null ? 0 : row.getStock();
        long negativeCount = row.getNegativeCount() == null ? 0L : row.getNegativeCount();
        int titleLength = row.getTitle() == null ? 0 : row.getTitle().length();
        BigDecimal avgStar = row.getAvgStar() == null ? BigDecimal.valueOf(5) : row.getAvgStar();

        if (stock >= 500 && salesQuantity <= 3) {
            score += 35;
            reasons.add("库存高但区间销量偏低");
        }
        if (negativeCount > 0) {
            score += Math.min(30, negativeCount * 12);
            reasons.add("低星或风险评价集中");
        }
        if (avgStar.compareTo(BigDecimal.valueOf(4.0)) < 0) {
            score += 15;
            reasons.add("平均评分偏低");
        }
        if (titleLength < 12 || titleLength > 60) {
            score += 12;
            reasons.add("标题长度不在推荐区间");
        }
        if (salesQuantity == 0 && stock > 0) {
            score += 10;
            reasons.add("有库存但区间内无成交");
        }

        return Map.of(
                "productId", row.getProductId(),
                "productName", row.getProductName(),
                "categoryName", row.getCategoryName() == null ? "" : row.getCategoryName(),
                "salesQuantity", salesQuantity,
                "salesAmount", money(row.getSalesAmount()),
                "stock", stock,
                "negativeCount", negativeCount,
                "avgStar", avgStar.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                "reason", reasons.isEmpty() ? "暂无明显风险" : String.join("；", reasons),
                "score", BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP).doubleValue()
        );
    }

    private double money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
