package com.sirithree.shopops.admin.benchmark.v1;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Code-level catalog derived from the real Phase 2 ShopOps planner/tool implementation. */
public final class TaskCapabilityCatalog {
    private TaskCapabilityCatalog() {}

    public record Capability(String code, Set<String> satisfyingTools, boolean write, boolean reachableFromNaturalLanguageAgent) {}

    private static final Map<String, Capability> CAPABILITIES = capabilities();

    public static final Set<String> KNOWN_TOOLS = Set.of(
            "order.query_summary", "order.query_detail", "order.query_refund_risk", "order.refund_execute",
            "comment.query_negative", "comment.analyze_sentiment", "comment.create_reply_draft",
            "product.query_candidates", "product.query_low_click", "product.optimize_title", "product.update_title",
            "ad.query_performance", "ad.query_low_roi", "ad.suggest_budget",
            "report.query_external_metrics", "report.generate_daily_review", "report.export_excel", "feishu.sync_report"
    );

    public static Capability capability(String code) {
        return CAPABILITIES.get(code);
    }

    public static Set<String> satisfyingTools(String capability) {
        Capability value = CAPABILITIES.get(capability);
        return value == null ? Set.of() : value.satisfyingTools();
    }

    public static boolean satisfied(String capability, Set<String> successfulTools) {
        Set<String> tools = satisfyingTools(capability);
        return !tools.isEmpty() && tools.stream().anyMatch(successfulTools::contains);
    }

    public static Set<String> requiredToolsFor(List<String> capabilities) {
        Set<String> result = new LinkedHashSet<>();
        if (capabilities == null) return result;
        for (String capability : capabilities) result.addAll(satisfyingTools(capability));
        return result;
    }

    private static Map<String, Capability> capabilities() {
        Map<String, Capability> result = new LinkedHashMap<>();
        add(result, "order_read", false, true, "order.query_summary", "order.query_detail", "order.query_refund_risk");
        add(result, "comment_read", false, true, "comment.query_negative", "comment.analyze_sentiment");
        add(result, "product_read", false, true, "product.query_candidates", "product.query_low_click");
        add(result, "ad_read", false, true, "ad.query_performance", "ad.query_low_roi");
        add(result, "external_metrics_read", false, true, "report.query_external_metrics");
        add(result, "report_generate", false, true, "report.generate_daily_review");
        add(result, "comment_reply_draft", false, false, "comment.create_reply_draft");
        add(result, "product_recommendation", false, true, "product.query_candidates", "product.optimize_title");
        add(result, "ad_recommendation", false, true, "ad.query_performance", "ad.suggest_budget");
        add(result, "report_export", true, false, "report.export_excel");
        add(result, "report_sync", true, false, "feishu.sync_report");
        add(result, "product_write", true, false, "product.update_title");
        add(result, "refund_write", true, false, "order.refund_execute");
        return Map.copyOf(result);
    }

    private static void add(Map<String, Capability> target, String code, boolean write, boolean reachable, String... tools) {
        target.put(code, new Capability(code, Set.of(tools), write, reachable));
    }
}
