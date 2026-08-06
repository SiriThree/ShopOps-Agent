package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductOptimizeTitleExecutor extends PortfolioOperationToolExecutor {
    @Override
    public String toolCode() {
        return "product.optimize_title";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        Map<String, Object> data = base(context, input);
        data.put("titleSuggestions", List.of(
                Map.of("productId", "PRD-LOW-001", "title", "Breathable Storage Organizer for Bedroom and Travel", "score", 91),
                Map.of("productId", "PRD-LOW-002", "title", "Lightweight Kitchen Organizer with Durable Handle", "score", 87)
        ));
        data.put("verifierChecks", List.of("length", "bannedWords", "duplicateKeywords"));
        return data;
    }
}
