package com.sirithree.shopops.admin.benchmark.v1;

import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TaskEvaluationFixtures {
    private TaskEvaluationFixtures() {}

    static BenchmarkCase benchmarkCase(String scenario) {
        BenchmarkCase c = new BenchmarkCase();
        c.caseId = "fixture-" + scenario;
        c.benchmarkType = BenchmarkType.TASK;
        c.scenario = scenario;
        c.difficulty = "MEDIUM";
        c.input.put("userInput", "fixture request");
        c.input.put("dateRange", Map.of("start", "2018-08-07", "end", "2018-08-07"));
        c.identity.put("tenantId", 1L);
        c.identity.put("shopId", 1L);
        c.identity.put("userId", 2L);
        c.expectedOutcome.put("reportRequired", true);
        c.expectedOutcome.put("expectedIntent", scenario);
        c.expectedOutcome.put("requiredTerminalTaskStates", List.of("SUCCESS"));
        c.sideEffectExpectation.expectedLogicalSideEffects = 0;
        c.sideEffectExpectation.forbiddenEffectTypes.add("REFUND_CREATED");
        c.approvalExpectation.required = false;
        c.goldVersion = "shopopsbench-gold-v1.1";
        c.origin = "HAND_AUTHORED";
        c.humanReviewed = true;
        switch (scenario) {
            case "daily_review" -> {
                c.requiredCapabilities.addAll(List.of("order_read", "comment_read", "product_read", "ad_read", "external_metrics_read", "report_generate"));
                c.acceptableTools.addAll(List.of("order.query_summary", "comment.query_negative", "product.query_candidates",
                        "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review"));
            }
            case "comment_risk" -> {
                c.requiredCapabilities.addAll(List.of("order_read", "comment_read", "product_read", "report_generate"));
                c.acceptableTools.addAll(List.of("order.query_summary", "comment.query_negative", "product.query_candidates", "report.generate_daily_review"));
                c.expectedOutcome.put("resultClass", "RISK_FOUND");
            }
            case "product_optimization" -> {
                c.requiredCapabilities.addAll(List.of("order_read", "product_read", "comment_read", "report_generate"));
                c.acceptableTools.addAll(List.of("order.query_summary", "product.query_candidates", "comment.query_negative", "report.generate_daily_review"));
            }
            case "ad_anomaly" -> {
                c.requiredCapabilities.addAll(List.of("order_read", "ad_read", "external_metrics_read", "report_generate"));
                c.acceptableTools.addAll(List.of("order.query_summary", "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review"));
                c.expectedOutcome.put("resultClass", "RISK_FOUND");
            }
            default -> {}
        }
        c.forbiddenTools.addAll(List.of("order.refund_execute", "product.update_title", "feishu.sync_report"));
        return c;
    }

    static CollectedEvidence evidence(String scenario) {
        CollectedEvidence e = new CollectedEvidence();
        AgentTaskDto task = new AgentTaskDto();
        task.setTaskId(42L);
        task.setStatus("SUCCESS");
        task.setTenantId(1L);
        task.setShopId(1L);
        e.task = task;

        Map<String, Object> order = linked(
                "gmv", 128936.50, "orderCount", 842, "refundAmount", 5360.00,
                "refundRate", 0.0416, "avgOrderAmount", 153.13, "connectorCode", "memory.default");
        Map<String, Object> comments = linked(
                "negativeCount", 2,
                "riskComments", List.of(
                        Map.of("commentId", 50101, "productId", 1001, "productName", "轻量保温杯 500ml"),
                        Map.of("commentId", 50102, "productId", 1008, "productName", "便携收纳箱")),
                "connectorCode", "memory.default");
        Map<String, Object> products = linked(
                "candidateCount", 2,
                "products", List.of(
                        Map.of("productId", 1001, "productName", "轻量保温杯 500ml"),
                        Map.of("productId", 1008, "productName", "便携收纳箱")),
                "connectorCode", "memory.default");
        Map<String, Object> ads = linked(
                "spend", 18600.0, "impressions", 420000, "clicks", 18600,
                "ctr", 0.0443, "roi", 3.72,
                "campaigns", List.of(Map.of("campaignName", "收纳好物拉新", "roi", 2.86)),
                "connectorCode", "memory.default");
        Map<String, Object> external = linked(
                "visitorCount", 36520, "conversionRate", 0.031, "favoriteCount", 4210,
                "cartAddCount", 2980, "connectorCode", "memory.default");

        List<String> tools = switch (scenario) {
            case "comment_risk" -> List.of("order.query_summary", "comment.query_negative", "product.query_candidates", "report.generate_daily_review");
            case "product_optimization" -> List.of("order.query_summary", "product.query_candidates", "comment.query_negative", "report.generate_daily_review");
            case "ad_anomaly" -> List.of("order.query_summary", "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review");
            default -> List.of("order.query_summary", "comment.query_negative", "product.query_candidates", "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review");
        };
        Map<String, Map<String, Object>> outputs = Map.of(
                "order.query_summary", order,
                "comment.query_negative", comments,
                "product.query_candidates", products,
                "ad.query_performance", ads,
                "report.query_external_metrics", external,
                "report.generate_daily_review", Map.of("reportId", 9L));
        for (String tool : tools) e.toolLogs.add(toolLog(tool, outputs.get(tool), "SUCCESS", null));

        OperationReportDto report = new OperationReportDto();
        report.setReportId(9L);
        report.setStatus("SUCCESS");
        report.setMarkdown("# deterministic report\n\nEvidence-backed recommendation.");
        Map<String, Object> reportEvidence = new LinkedHashMap<>();
        reportEvidence.put("intent", scenario);
        reportEvidence.put("toolCodes", tools.stream().filter(t -> !"report.generate_daily_review".equals(t)).toList());
        reportEvidence.put("riskCommentIds", List.of(50101, 50102));
        reportEvidence.put("productIds", List.of(1001, 1008));
        reportEvidence.put("campaignNames", List.of("收纳好物拉新"));
        reportEvidence.put("adDataStatus", "AVAILABLE");
        Map<String, Object> sources = new LinkedHashMap<>();
        sources.put("orderSummary", source(order, "gmv", "orderCount", "refundAmount", "refundRate", "avgOrderAmount"));
        sources.put("negativeComments", source(comments, "negativeCount"));
        sources.put("productCandidates", source(products, "candidateCount"));
        sources.put("adPerformance", source(ads, "spend", "impressions", "clicks", "ctr", "roi"));
        sources.put("externalReportMetrics", source(external, "visitorCount", "conversionRate", "favoriteCount", "cartAddCount"));
        reportEvidence.put("dataSources", sources);
        report.setEvidence(reportEvidence);
        e.report = report;
        return e;
    }

    static CollectedEvidence adNoDataEvidence() {
        CollectedEvidence e = evidence("ad_anomaly");
        e.toolLogs = new ArrayList<>();
        Map<String, Object> order = linked("gmv", 100.0, "orderCount", 1, "refundAmount", 0.0, "refundRate", 0.0, "avgOrderAmount", 100.0, "connectorCode", "memory.default");
        Map<String, Object> ads = linked("connectorCode", "file.ad-performance");
        Map<String, Object> external = linked("visitorCount", 1000, "conversionRate", 0.03, "favoriteCount", 20, "cartAddCount", 10, "connectorCode", "memory.default");
        e.toolLogs.add(toolLog("order.query_summary", order, "SUCCESS", null));
        e.toolLogs.add(toolLog("ad.query_performance", ads, "SUCCESS", null));
        e.toolLogs.add(toolLog("report.query_external_metrics", external, "SUCCESS", null));
        e.toolLogs.add(toolLog("report.generate_daily_review", Map.of("reportId", 9L), "SUCCESS", null));
        @SuppressWarnings("unchecked")
        Map<String, Object> reportEvidence = (Map<String, Object>) e.report.getEvidence();
        reportEvidence.put("toolCodes", List.of("order.query_summary", "ad.query_performance", "report.query_external_metrics"));
        reportEvidence.put("campaignNames", List.of());
        reportEvidence.put("adDataStatus", "NO_DATA");
        Map<String, Object> sources = new LinkedHashMap<>();
        sources.put("orderSummary", source(order, "gmv", "orderCount", "refundAmount", "refundRate", "avgOrderAmount"));
        sources.put("adPerformance", source(ads, "spend", "impressions", "clicks", "ctr", "roi"));
        sources.put("externalReportMetrics", source(external, "visitorCount", "conversionRate", "favoriteCount", "cartAddCount"));
        reportEvidence.put("dataSources", sources);
        return e;
    }

    static Map<String, Object> toolLog(String tool, Map<String, Object> output, String status, String errorCode) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("toolCode", tool);
        log.put("status", status);
        log.put("input", Map.of("shopId", 1L, "startDate", "2018-08-07", "endDate", "2018-08-07"));
        if (output != null) log.put("output", output);
        if (errorCode != null) log.put("errorCode", errorCode);
        return log;
    }

    static Map<String, Object> source(Map<String, Object> output, String... keys) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        for (String key : keys) if (output.containsKey(key)) metrics.put(key, output.get(key));
        return Map.of("connectorCode", String.valueOf(output.getOrDefault("connectorCode", "unknown")), "metrics", metrics);
    }

    static Map<String, Object> linked(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) map.put(String.valueOf(kv[i]), kv[i + 1]);
        return map;
    }
}
