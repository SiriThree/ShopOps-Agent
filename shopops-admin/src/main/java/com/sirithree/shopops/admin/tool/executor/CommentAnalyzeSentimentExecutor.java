package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CommentAnalyzeSentimentExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "comment.analyze_sentiment";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("negativeRatio", 0.138);
        data.put("topics", List.of("delivery delay", "damaged package", "size mismatch"));
        data.put("recommendations", recommendations("Prioritize delivery delay comments", "Create product detail clarification"));
        return data;
    }
}
