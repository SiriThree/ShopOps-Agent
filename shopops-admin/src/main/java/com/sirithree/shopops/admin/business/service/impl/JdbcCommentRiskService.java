package com.sirithree.shopops.admin.business.service.impl;

import com.sirithree.shopops.admin.business.domain.CommentRiskRow;
import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.persistence.mapper.BusinessCommentMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcCommentRiskService implements CommentRiskService {
    private static final List<String> RISK_KEYWORDS = List.of("退款", "破损", "物流慢", "描述不符", "质量", "客服");

    private final BusinessCommentMapper commentMapper;

    public JdbcCommentRiskService(BusinessCommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @Override
    public Map<String, Object> queryNegativeComments(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer minStar) {
        int safeMinStar = minStar == null ? 3 : minStar;
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endExclusiveAt = endDate.plusDays(1).atStartOfDay();
        Long count = commentMapper.countNegativeComments(tenantId, shopId, startAt, endExclusiveAt, safeMinStar);
        List<CommentRiskRow> rows = commentMapper.listNegativeComments(tenantId, shopId, startAt, endExclusiveAt, safeMinStar, 20);

        Map<String, Integer> categoryStats = new LinkedHashMap<>();
        List<Map<String, Object>> riskComments = new ArrayList<>();
        for (CommentRiskRow row : rows) {
            List<String> keywords = matchKeywords(row.getContent(), row.getStar(), safeMinStar);
            keywords.forEach(keyword -> categoryStats.merge(keyword, 1, Integer::sum));
            riskComments.add(Map.of(
                    "commentId", row.getCommentId(),
                    "productId", row.getProductId(),
                    "productName", row.getProductName() == null ? "" : row.getProductName(),
                    "star", row.getStar(),
                    "content", row.getContent(),
                    "riskKeywords", keywords
            ));
        }

        return Map.of(
                "negativeCount", count == null ? 0L : count,
                "riskComments", riskComments,
                "categoryStats", categoryStats
        );
    }

    private List<String> matchKeywords(String content, Integer star, int minStar) {
        List<String> keywords = new ArrayList<>();
        String safeContent = content == null ? "" : content;
        for (String keyword : RISK_KEYWORDS) {
            if (safeContent.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        if (keywords.isEmpty() && star != null && star <= minStar) {
            keywords.add("低星评价");
        }
        return keywords;
    }
}
