package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.business.support.ToolInputParser;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CommentQueryNegativeExecutor implements ToolExecutor {
    private final CommentRiskService commentRiskService;

    public CommentQueryNegativeExecutor(CommentRiskService commentRiskService) {
        this.commentRiskService = commentRiskService;
    }

    @Override
    public String toolCode() {
        return "comment.query_negative";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> inputMap = ToolInputParser.asMap(input);
        Long shopId = ToolInputParser.longValue(inputMap, "shopId", context.getShopId());
        LocalDate startDate = ToolInputParser.dateValue(inputMap, "startDate");
        LocalDate endDate = ToolInputParser.dateValue(inputMap, "endDate");
        Integer minStar = ToolInputParser.intValue(inputMap, "minStar", 3);
        Map<String, Object> data = commentRiskService.queryNegativeComments(context.getTenantId(), shopId, startDate, endDate, minStar);
        return ToolInvokeResult.success(data, null);
    }
}
