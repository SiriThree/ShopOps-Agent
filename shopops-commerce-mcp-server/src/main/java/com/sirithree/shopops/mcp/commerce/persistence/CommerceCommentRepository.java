package com.sirithree.shopops.mcp.commerce.persistence;

import com.sirithree.shopops.mcp.commerce.domain.NegativeCommentRecord;
import java.time.LocalDate;
import java.util.List;

public interface CommerceCommentRepository {
    List<NegativeCommentRecord> findNegativeComments(
            long tenantId,
            long shopId,
            LocalDate startDate,
            LocalDate endDate,
            int maxStar
    );
}
