package com.sirithree.shopops.mcp.commerce.persistence;

import com.sirithree.shopops.mcp.commerce.domain.NegativeCommentRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Independent Commerce state used by the external adapter in development and integration tests.
 * It is intentionally not backed by shopops-admin services or mappers.
 */
@Repository
public class InMemoryCommerceCommentRepository implements CommerceCommentRepository {
    private final List<NegativeCommentRecord> records = List.of(
            new NegativeCommentRecord(1, 1, 1, "ORD-1001", "SKU-RED-01", 1, "QUALITY", "包装破损且商品有划痕", LocalDate.of(2026, 8, 1)),
            new NegativeCommentRecord(2, 1, 1, "ORD-1002", "SKU-RED-01", 2, "LOGISTICS", "配送延迟三天", LocalDate.of(2026, 8, 2)),
            new NegativeCommentRecord(3, 1, 1, "ORD-1003", "SKU-BLUE-02", 3, "DESCRIPTION", "尺寸与详情页描述不一致", LocalDate.of(2026, 8, 3)),
            new NegativeCommentRecord(4, 1, 2, "ORD-2001", "SKU-GREEN-03", 1, "QUALITY", "开箱后无法正常使用", LocalDate.of(2026, 8, 2)),
            new NegativeCommentRecord(5, 2, 9, "ORD-9001", "SKU-OTHER-01", 1, "QUALITY", "other tenant data", LocalDate.of(2026, 8, 2))
    );

    @Override
    public List<NegativeCommentRecord> findNegativeComments(long tenantId,
                                                            long shopId,
                                                            LocalDate startDate,
                                                            LocalDate endDate,
                                                            int maxStar) {
        return records.stream()
                .filter(record -> record.tenantId() == tenantId)
                .filter(record -> record.shopId() == shopId)
                .filter(record -> !record.commentDate().isBefore(startDate))
                .filter(record -> !record.commentDate().isAfter(endDate))
                .filter(record -> record.star() <= maxStar)
                .toList();
    }
}
