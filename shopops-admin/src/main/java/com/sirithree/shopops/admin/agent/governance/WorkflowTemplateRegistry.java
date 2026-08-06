package com.sirithree.shopops.admin.agent.governance;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WorkflowTemplateRegistry {
    private final Map<String, WorkflowTemplate> templates = new LinkedHashMap<>();

    public WorkflowTemplateRegistry() {
        register("daily_review", Set.of("order.query_summary", "comment.query_negative", "product.query_candidates", "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review"), 6);
        register("comment_risk", Set.of("order.query_summary", "comment.query_negative", "product.query_candidates", "report.generate_daily_review"), 4);
        register("product_optimization", Set.of("order.query_summary", "product.query_candidates", "comment.query_negative", "report.generate_daily_review"), 4);
        register("ad_anomaly", Set.of("order.query_summary", "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review"), 4);
    }

    private void register(String type, Set<String> tools, int maxSteps) {
        templates.put(type, new WorkflowTemplate(type, tools, maxSteps, "MEDIUM",
                Set.of(AgentExecutionMode.ADVISORY, AgentExecutionMode.DRAFT, AgentExecutionMode.AUTOMATIC), 1, 120_000L));
    }

    public WorkflowTemplate require(String workflowType) {
        WorkflowTemplate template = templates.get(workflowType);
        if (template == null) throw new IllegalArgumentException("未注册的受控工作流模板: " + workflowType);
        return template;
    }
}
