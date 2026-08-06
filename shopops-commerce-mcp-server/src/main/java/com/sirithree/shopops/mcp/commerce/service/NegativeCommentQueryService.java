package com.sirithree.shopops.mcp.commerce.service;

import com.sirithree.shopops.mcp.commerce.config.CommerceMcpServerProperties;
import com.sirithree.shopops.mcp.commerce.domain.NegativeCommentRecord;
import com.sirithree.shopops.mcp.commerce.persistence.CommerceCommentRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NegativeCommentQueryService {
    private final CommerceCommentRepository repository;
    private final CommerceMcpServerProperties properties;

    public NegativeCommentQueryService(CommerceCommentRepository repository,
                                       CommerceMcpServerProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public Map<String, Object> query(long tenantId,
                                     long shopId,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     int maxStar) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        long rangeDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (rangeDays > properties.getMaxDateRangeDays()) {
            throw new IllegalArgumentException("date range exceeds " + properties.getMaxDateRangeDays() + " days");
        }
        if (maxStar < 1 || maxStar > 5) {
            throw new IllegalArgumentException("minStar must be between 1 and 5");
        }

        List<NegativeCommentRecord> records = repository.findNegativeComments(
                tenantId, shopId, startDate, endDate, maxStar);
        Map<String, Long> categoryStats = records.stream().collect(Collectors.groupingBy(
                NegativeCommentRecord::category,
                LinkedHashMap::new,
                Collectors.counting()
        ));
        List<Map<String, Object>> comments = records.stream().map(record -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("commentId", record.id());
            row.put("orderNo", record.orderNo());
            row.put("productId", record.productId());
            row.put("star", record.star());
            row.put("category", record.category());
            row.put("content", record.content());
            row.put("commentDate", record.commentDate().toString());
            return row;
        }).toList();

        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("tenantId", tenantId);
        scope.put("shopId", shopId);
        scope.put("startDate", startDate.toString());
        scope.put("endDate", endDate.toString());
        scope.put("maxStar", maxStar);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("negativeCount", comments.size());
        result.put("riskComments", comments);
        result.put("categoryStats", categoryStats);
        result.put("scope", scope);
        return result;
    }
}
