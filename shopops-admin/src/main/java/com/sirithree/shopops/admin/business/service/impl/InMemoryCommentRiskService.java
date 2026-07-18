package com.sirithree.shopops.admin.business.service.impl;

import com.sirithree.shopops.admin.business.service.CommentRiskService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryCommentRiskService implements CommentRiskService {
    @Override
    public Map<String, Object> queryNegativeComments(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer minStar) {
        return Map.of(
                "negativeCount", 7,
                "riskComments", List.of(
                        Map.of("commentId", 50101, "productId", 1001, "productName", "轻量保温杯 500ml", "star", 2, "content", "物流慢，包装有破损", "riskKeywords", List.of("物流慢", "破损")),
                        Map.of("commentId", 50102, "productId", 1008, "productName", "便携收纳箱", "star", 1, "content", "描述不符，申请退款", "riskKeywords", List.of("描述不符", "退款"))
                ),
                "categoryStats", Map.of("物流慢", 3, "描述不符", 2, "包装破损", 2)
        );
    }
}
