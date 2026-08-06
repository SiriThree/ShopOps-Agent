package com.sirithree.shopops.mcp.commerce.domain;

import java.time.LocalDate;

public record NegativeCommentRecord(
        long id,
        long tenantId,
        long shopId,
        String orderNo,
        String productId,
        int star,
        String category,
        String content,
        LocalDate commentDate
) {
}
