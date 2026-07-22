package com.sirithree.shopops.admin.agent.controller;

import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskEventDto;
import com.sirithree.shopops.admin.agent.domain.AgentTaskQueryParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskRecoveryResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskStepDto;
import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.agent.domain.NaturalLanguageTaskCreateResult;
import com.sirithree.shopops.admin.agent.domain.NaturalLanguageTaskRequest;
import com.sirithree.shopops.admin.agent.service.AgentTaskService;
import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tasks")
public class AgentTaskController {
    private final AgentTaskService agentTaskService;

    public AgentTaskController(AgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    @PostMapping
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<AgentTaskCreateResult> createTask(@Valid @RequestBody AgentTaskCreateParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.createTask(context.getTenantId(), context.getShopId(), context.getUserId(), param));
    }

    @PostMapping("/natural-language")
    @RequireRole(AuthRole.OPERATOR)
    public CommonResult<NaturalLanguageTaskCreateResult> createTaskFromNaturalLanguage(@Valid @RequestBody NaturalLanguageTaskRequest request) {
        RequestContext context = RequestContextHolder.current();
        AgentTaskCreateParam param = new AgentTaskCreateParam();
        String intent = routeIntent(request.getUserInput());
        param.setTaskType("daily_review");
        param.setUserInput(request.getUserInput().trim());
        param.setIntent(intent);
        param.setDateRange(defaultDateRange(request.getDateRange()));

        AgentTaskCreateResult task = agentTaskService.createTask(context.getTenantId(), context.getShopId(), context.getUserId(), param);

        NaturalLanguageTaskCreateResult result = new NaturalLanguageTaskCreateResult();
        result.setIntent(intent);
        result.setIntentLabel(intentLabel(intent));
        result.setConfidence(routeConfidence(request.getUserInput()));
        result.setTaskType(param.getTaskType());
        result.setRoutedReason(routedReason(intent));
        result.setFocusAreas(focusAreas(request.getUserInput()));
        result.setDataSources(dataSources(result.getFocusAreas()));
        result.setRecommendedActions(recommendedActions(intent));
        result.setDateRange(param.getDateRange());
        result.setTask(task);
        return CommonResult.success(result);
    }

    @PostMapping("/{taskId}/retry")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<AgentTaskCreateResult> retryTask(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.retryTask(context.getTenantId(), context.getShopId(), context.getUserId(), taskId));
    }

    @PostMapping("/stale/requeue")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<AgentTaskRecoveryResult> requeueStaleTasks(@RequestParam(defaultValue = "10") Integer queuedTimeoutMinutes,
                                                                   @RequestParam(defaultValue = "30") Integer runningTimeoutMinutes,
                                                                   @RequestParam(defaultValue = "20") Integer limit) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.requeueStaleTasks(
                context.getTenantId(),
                context.getShopId(),
                context.getUserId(),
                queuedTimeoutMinutes,
                runningTimeoutMinutes,
                limit
        ));
    }

    @GetMapping
    public CommonResult<CommonPage<AgentTaskDto>> listTasks(AgentTaskQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.listTasks(context.getTenantId(), context.getShopId(), param));
    }

    @GetMapping("/{taskId}")
    public CommonResult<AgentTaskDto> getTask(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return agentTaskService.getTask(context.getTenantId(), context.getShopId(), taskId)
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("任务不存在"));
    }

    @GetMapping("/{taskId}/steps")
    public CommonResult<List<AgentTaskStepDto>> listSteps(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.listSteps(context.getTenantId(), context.getShopId(), taskId));
    }

    @GetMapping("/{taskId}/events")
    public CommonResult<List<AgentTaskEventDto>> listEvents(@PathVariable Long taskId) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(agentTaskService.listEvents(context.getTenantId(), context.getShopId(), taskId));
    }

    private DateRangeParam defaultDateRange(DateRangeParam requested) {
        if (requested != null && hasText(requested.getStart()) && hasText(requested.getEnd())) {
            return requested;
        }
        LocalDate today = LocalDate.now();
        DateRangeParam fallback = new DateRangeParam();
        fallback.setStart(today.toString());
        fallback.setEnd(today.toString());
        return fallback;
    }

    private double routeConfidence(String userInput) {
        String normalized = userInput == null ? "" : userInput.toLowerCase();
        if (normalized.contains("日报") || normalized.contains("daily") || normalized.contains("复盘")
                || normalized.contains("报告") || normalized.contains("report")) {
            return 0.95;
        }
        if (normalized.contains("差评") || normalized.contains("商品") || normalized.contains("投放")
                || normalized.contains("评价") || normalized.contains("退款")) {
            return 0.82;
        }
        return 0.68;
    }

    private String routeIntent(String userInput) {
        String normalized = userInput == null ? "" : userInput.toLowerCase();
        if (containsAny(normalized, "\u65e5\u62a5", "\u590d\u76d8", "\u62a5\u544a", "daily", "report")) {
            return "daily_review";
        }
        if (containsAny(normalized, "\u6295\u653e", "\u5e7f\u544a", "\u6d88\u8017", "\u8f6c\u5316", "ad", "campaign", "conversion")) {
            return "ad_anomaly";
        }
        if (containsAny(normalized, "\u5dee\u8bc4", "\u8bc4\u4ef7", "negative", "comment", "review")) {
            return "comment_risk";
        }
        if (containsAny(normalized, "\u4f4e\u70b9\u51fb", "\u5546\u54c1", "\u6807\u9898", "product", "title", "click")) {
            return "product_optimization";
        }
        return "daily_review";
    }

    private String intentLabel(String intent) {
        return switch (intent) {
            case "comment_risk" -> "\u5dee\u8bc4\u98ce\u9669\u5206\u6790";
            case "product_optimization" -> "\u5546\u54c1\u4f18\u5316\u8bc6\u522b";
            case "ad_anomaly" -> "\u6295\u653e\u5f02\u5e38\u68c0\u67e5";
            default -> "\u8fd0\u8425\u65e5\u62a5\u590d\u76d8";
        };
    }

    private String routedReason(String intent) {
        return switch (intent) {
            case "comment_risk" -> "\u5df2\u8bc6\u522b\u4e3a\u5dee\u8bc4\u98ce\u9669\u5206\u6790\uff1aAgent \u5c06\u6267\u884c\u8ba2\u5355\u57fa\u7ebf\u3001\u8bc4\u4ef7\u98ce\u9669\u3001\u53d7\u5f71\u54cd\u5546\u54c1\u548c\u4e13\u9879\u62a5\u544a\u751f\u6210\u6b65\u9aa4\u3002";
            case "product_optimization" -> "\u5df2\u8bc6\u522b\u4e3a\u5546\u54c1\u4f18\u5316\u8bc6\u522b\uff1aAgent \u5c06\u6267\u884c\u8ba2\u5355\u57fa\u7ebf\u3001\u4f4e\u70b9\u51fb\u5546\u54c1\u3001\u5173\u8054\u8bc4\u4ef7\u4fe1\u53f7\u548c\u4e13\u9879\u62a5\u544a\u751f\u6210\u6b65\u9aa4\u3002";
            case "ad_anomaly" -> "\u5df2\u8bc6\u522b\u4e3a\u6295\u653e\u5f02\u5e38\u68c0\u67e5\uff1aAgent \u5c06\u6267\u884c\u8ba2\u5355\u57fa\u7ebf\u3001\u6295\u653e\u5f02\u5e38\u3001\u5e73\u53f0\u6307\u6807\u5bf9\u6bd4\u548c\u4e13\u9879\u62a5\u544a\u751f\u6210\u6b65\u9aa4\u3002";
            default -> "\u5df2\u8bc6\u522b\u4e3a\u8fd0\u8425\u65e5\u62a5\u590d\u76d8\uff1a\u5f53\u524d Agent \u4f1a\u6c47\u603b\u8ba2\u5355\u3001\u8bc4\u4ef7\u3001\u5546\u54c1\u3001\u6295\u653e\u548c\u5e73\u53f0\u6307\u6807\u3002";
        };
    }

    private List<String> recommendedActions(String intent) {
        return switch (intent) {
            case "comment_risk" -> List.of("\u805a\u7c7b\u4f4e\u661f\u8bc4\u4ef7\u539f\u56e0", "\u6807\u8bb0\u9700\u4f18\u5148\u5ba2\u670d\u4ecb\u5165\u7684\u5546\u54c1", "\u8ffd\u8e2a\u9000\u6b3e\u548c\u5dee\u8bc4\u5171\u632f\u98ce\u9669");
            case "product_optimization" -> List.of("\u8bc6\u522b\u4f4e\u70b9\u51fb\u6216\u9ad8\u98ce\u9669\u5546\u54c1", "\u751f\u6210\u6807\u9898\u548c\u4e3b\u56fe\u4f18\u5316\u5efa\u8bae", "\u6c89\u6dc0\u5f85\u4f18\u5316\u5546\u54c1\u6e05\u5355");
            case "ad_anomaly" -> List.of("\u68c0\u67e5\u9ad8\u6d88\u8017\u4f4e\u8f6c\u5316\u8ba1\u5212", "\u8bc4\u4f30 ROI \u548c CPC \u5f02\u5e38", "\u7ed9\u51fa\u9884\u7b97\u8c03\u6574\u5efa\u8bae");
            default -> List.of("\u6c47\u603b\u6838\u5fc3\u7ecf\u8425\u6307\u6807", "\u8bc6\u522b\u5f02\u5e38\u544a\u8b66", "\u751f\u6210\u7ed3\u6784\u5316\u8fd0\u8425\u5efa\u8bae");
        };
    }

    private List<String> focusAreas(String userInput) {
        String normalized = userInput == null ? "" : userInput.toLowerCase();
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
            areas.add("运营日报");
            areas.add("异常识别");
            areas.add("改进建议");
        }
        return areas;
    }

    private List<String> dataSources(List<String> focusAreas) {
        List<String> sources = new ArrayList<>();
        sources.add("订单汇总");
        if (focusAreas.contains("差评风险")) {
            sources.add("评价明细");
        }
        if (focusAreas.contains("商品优化")) {
            sources.add("商品候选");
        }
        if (focusAreas.contains("投放表现")) {
            sources.add("广告投放");
        }
        sources.add("平台外部指标");
        return sources;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
