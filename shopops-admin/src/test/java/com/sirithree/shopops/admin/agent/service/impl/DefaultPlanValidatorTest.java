package com.sirithree.shopops.admin.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.governance.WorkflowTemplateRegistry;
import com.sirithree.shopops.admin.auth.service.AuthorizationService;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.service.McpToolService;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultPlanValidatorTest {
    private final McpToolService toolService = mock(McpToolService.class);
    private final AuthorizationService authorizationService = mock(AuthorizationService.class);
    private final DefaultPlanValidator validator = new DefaultPlanValidator(toolService, authorizationService, new WorkflowTemplateRegistry());

    @Test
    void shouldAcceptSequentialPlanEndingWithReport() {
        when(toolService.getTool(anyLong(), anyString())).thenReturn(tool());
        when(authorizationService.isAuthorized(anyLong(), anyLong(), anyLong(), anyString())).thenReturn(true);

        assertThatCode(() -> validator.validate(context(), plan(
                new AgentPlanStep(1, "查询订单", "order.query_summary"),
                new AgentPlanStep(2, "生成报告", "report.generate_daily_review")
        ))).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDuplicateOrOutOfOrderTools() {
        when(toolService.getTool(anyLong(), anyString())).thenReturn(tool());
        when(authorizationService.isAuthorized(anyLong(), anyLong(), anyLong(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(context(), plan(
                new AgentPlanStep(1, "查询订单", "order.query_summary"),
                new AgentPlanStep(2, "重复查询", "order.query_summary"),
                new AgentPlanStep(3, "生成报告", "report.generate_daily_review")
        ))).hasMessageContaining("重复工具");

        assertThatThrownBy(() -> validator.validate(context(), plan(
                new AgentPlanStep(2, "查询订单", "order.query_summary"),
                new AgentPlanStep(3, "生成报告", "report.generate_daily_review")
        ))).hasMessageContaining("连续递增");
    }

    @Test
    void shouldRejectReportToolBeforeFinalStep() {
        when(toolService.getTool(anyLong(), anyString())).thenReturn(tool());
        when(authorizationService.isAuthorized(anyLong(), anyLong(), anyLong(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(context(), plan(
                new AgentPlanStep(1, "生成报告", "report.generate_daily_review"),
                new AgentPlanStep(2, "查询订单", "order.query_summary")
        ))).hasMessageContaining("最后一步");
    }

    private AgentTaskContext context() {
        AgentTaskCreateParam param = new AgentTaskCreateParam();
        param.setTaskType("daily_review");
        param.setIntent("daily_review");
        AgentTaskContext context = new AgentTaskContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(1L);
        context.setCreateParam(param);
        return context;
    }

    private AgentPlan plan(AgentPlanStep... steps) {
        AgentPlan plan = new AgentPlan();
        plan.setTaskType("daily_review");
        plan.setSteps(List.of(steps));
        return plan;
    }

    private McpToolDto tool() {
        return new McpToolDto("test", "test", "test", "test", "LOW");
    }
}
