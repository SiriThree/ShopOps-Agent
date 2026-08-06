package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentTaskInterpretation;
import com.sirithree.shopops.admin.agent.domain.AgentTaskSpec;
import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.agent.service.AgentTaskInterpreter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedAgentTaskInterpreter implements AgentTaskInterpreter {
    @Override
    public AgentTaskInterpretation interpret(String userInput, DateRangeParam dateRange) {
        String normalizedInput = userInput == null ? "" : userInput.trim();
        String intent = routeIntent(normalizedInput);

        AgentTaskSpec spec = new AgentTaskSpec();
        spec.setIntent(intent);
        spec.setObjective(normalizedInput);
        spec.setDateRange(dateRange);
        spec.setFocusAreas(focusAreas(normalizedInput));
        spec.setRequiredEvidence(requiredEvidence(intent));
        spec.setOutputFormat("structured_markdown_report");
        spec.setConstraints(defaultConstraints());

        AgentTaskInterpretation interpretation = new AgentTaskInterpretation();
        interpretation.setTaskSpec(spec);
        interpretation.setConfidence(routeConfidence(normalizedInput));
        interpretation.setIntentLabel(intentLabel(intent));
        interpretation.setRoutedReason(routedReason(intent));
        interpretation.setDataSources(dataSources(spec.getRequiredEvidence()));
        interpretation.setRecommendedActions(recommendedActions(intent));
        return interpretation;
    }

    private String routeIntent(String userInput) {
        String normalized = userInput.toLowerCase();
        if (containsAny(normalized, "日报", "复盘", "报告", "daily", "report")) {
            return "daily_review";
        }
        if (containsAny(normalized, "投放", "广告", "消耗", "转化", "ad", "campaign", "conversion")) {
            return "ad_anomaly";
        }
        if (containsAny(normalized, "差评", "评价", "negative", "comment", "review")) {
            return "comment_risk";
        }
        if (containsAny(normalized, "低点击", "商品", "标题", "product", "title", "click")) {
            return "product_optimization";
        }
        return "daily_review";
    }

    private double routeConfidence(String userInput) {
        String normalized = userInput.toLowerCase();
        if (containsAny(normalized, "日报", "daily", "复盘", "报告", "report")) {
            return 0.95;
        }
        if (containsAny(normalized, "差评", "商品", "投放", "评价", "退款", "negative", "product", "campaign")) {
            return 0.82;
        }
        return 0.68;
    }

    private String intentLabel(String intent) {
        return switch (intent) {
            case "comment_risk" -> "差评风险分析";
            case "product_optimization" -> "商品优化识别";
            case "ad_anomaly" -> "投放异常检查";
            default -> "运营日报复盘";
        };
    }

    private String routedReason(String intent) {
        return switch (intent) {
            case "comment_risk" -> "识别为差评风险分析，将核对订单基线、评价风险和受影响商品。";
            case "product_optimization" -> "识别为商品优化任务，将分析订单基线、商品候选和关联评价信号。";
            case "ad_anomaly" -> "识别为投放异常任务，将检查订单基线、广告表现和外部指标。";
            default -> "识别为经营复盘任务，将汇总订单、评价、商品、投放和外部指标。";
        };
    }

    private List<String> focusAreas(String userInput) {
        String normalized = userInput.toLowerCase();
        List<String> areas = new ArrayList<>();
        if (containsAny(normalized, "日报", "复盘", "报告", "daily", "report")) {
            areas.add("运营日报");
        }
        if (containsAny(normalized, "差评", "评价", "negative", "comment", "review")) {
            areas.add("差评风险");
        }
        if (containsAny(normalized, "退款", "退货", "售后", "refund", "return")) {
            areas.add("退款售后");
        }
        if (containsAny(normalized, "商品", "标题", "低点击", "product", "title", "click")) {
            areas.add("商品优化");
        }
        if (containsAny(normalized, "投放", "广告", "消耗", "转化", "ad", "campaign", "conversion")) {
            areas.add("投放表现");
        }
        if (areas.isEmpty()) {
            areas.addAll(List.of("运营日报", "异常识别", "改进建议"));
        }
        return areas;
    }

    private List<String> requiredEvidence(String intent) {
        return switch (intent) {
            case "comment_risk" -> List.of("order_summary", "negative_comments", "product_candidates");
            case "product_optimization" -> List.of("order_summary", "product_candidates", "negative_comments");
            case "ad_anomaly" -> List.of("order_summary", "ad_performance", "external_metrics");
            default -> List.of("order_summary", "negative_comments", "product_candidates", "ad_performance", "external_metrics");
        };
    }

    private List<String> dataSources(List<String> requiredEvidence) {
        Map<String, String> labels = Map.of(
                "order_summary", "订单汇总",
                "negative_comments", "评价明细",
                "product_candidates", "商品候选",
                "ad_performance", "广告投放",
                "external_metrics", "平台外部指标"
        );
        return requiredEvidence.stream().map(labels::get).toList();
    }

    private List<String> recommendedActions(String intent) {
        return switch (intent) {
            case "comment_risk" -> List.of("聚类低星评价原因", "标记需客服优先介入的商品", "追踪退款与差评共振风险");
            case "product_optimization" -> List.of("识别低点击或高风险商品", "生成标题和主图优化建议", "沉淀待优化商品清单");
            case "ad_anomaly" -> List.of("检查高消耗低转化计划", "评估 ROI 和 CPC 异常", "给出预算调整建议");
            default -> List.of("汇总核心经营指标", "识别异常告警", "生成结构化运营建议");
        };
    }

    private Map<String, Object> defaultConstraints() {
        Map<String, Object> constraints = new LinkedHashMap<>();
        constraints.put("readOnlyAnalysis", true);
        constraints.put("allowDegradedEvidence", true);
        constraints.put("requireTraceableEvidence", true);
        return constraints;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
