package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolExecutor;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductQueryCandidatesExecutor implements ToolExecutor {
    @Override
    public String toolCode() {
        return "product.query_candidates";
    }

    @Override
    public ToolInvokeResult execute(ToolInvokeContext context, Object input) {
        Map<String, Object> data = Map.of(
                "candidateCount", 3,
                "products", List.of(
                        Map.of("productId", 1001, "productName", "轻量保温杯 500ml", "reason", "库存高但近 7 日销量低", "score", 82.5),
                        Map.of("productId", 1008, "productName", "便携收纳箱", "reason", "差评集中在描述不符", "score", 78.0),
                        Map.of("productId", 1016, "productName", "运动毛巾", "reason", "标题长度偏短，关键词覆盖不足", "score", 73.5)
                )
        );
        return ToolInvokeResult.success(data, null);
    }
}
