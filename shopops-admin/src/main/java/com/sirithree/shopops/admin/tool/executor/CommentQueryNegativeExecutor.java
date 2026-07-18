package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CommentQueryNegativeExecutor implements ToolExecutor {
    @Override
    public String toolCode() {
        return "comment.query_negative";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> data = Map.of(
                "negativeCount", 7,
                "riskComments", List.of(
                        Map.of("commentId", 50101, "productId", 1001, "star", 2, "content", "物流太慢，包装有破损", "riskKeywords", List.of("物流慢", "破损")),
                        Map.of("commentId", 50102, "productId", 1008, "star", 1, "content", "描述不符，申请退款", "riskKeywords", List.of("描述不符", "退款"))
                ),
                "categoryStats", Map.of("物流慢", 3, "描述不符", 2, "包装破损", 2)
        );
        return ToolInvokeResult.success(data, null);
    }
}
