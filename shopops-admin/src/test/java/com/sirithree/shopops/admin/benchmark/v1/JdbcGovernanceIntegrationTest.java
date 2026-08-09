package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.approval.domain.ApprovalDecisionParam;
import com.sirithree.shopops.admin.approval.service.ApprovalRequestService;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.ExternalSystemMode;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.RecordingRefundExternalSystem;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import com.sirithree.shopops.admin.tool.service.ToolGatewayService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Optional real-JDBC governance gate. It uses the seeded JDBC authorization store (viewer=3, operator=2),
 * the production Tool Gateway / approval / WriteOperation path, and an independent non-idempotent external ledger.
 * It is disabled unless explicitly requested because it requires the project's MySQL integration database.
 */
@EnabledIfSystemProperty(named = "shopops.jdbc.it", matches = "true")
@SpringBootTest(properties = {
        "shopops.persistence=jdbc",
        "shopops.agent.dispatch-mode=sync",
        "shopops.mcp.servers.commerce.enabled=false",
        "spring.datasource.url=jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
        "spring.datasource.username=root",
        "spring.datasource.password=root",
        "spring.datasource.hikari.initialization-fail-timeout=1",
        "spring.datasource.hikari.connection-timeout=3000"
})
@Import(JdbcGovernanceIntegrationTest.Infrastructure.class)
class JdbcGovernanceIntegrationTest {
    @Autowired ToolGatewayService toolGateway;
    @Autowired ApprovalRequestService approvals;
    @Autowired RecordingRefundExternalSystem external;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetExternal() {
        external.reset(ExternalSystemMode.NON_IDEMPOTENT_EXTERNAL);
    }

    @Test
    void viewerCannotRefundAndCreatesNoExternalEffect() {
        ToolInvokeResult result = toolGateway.invoke(context(3L, null), "order.refund_execute", refundInput());
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorCode()).isIn("TOOL_PERMISSION_DENIED", "TOOL_AUTHORIZATION_DENIED");
        assertThat(external.effectiveEffectCount()).isZero();
    }

    @Test
    void legitimateOperatorRefundRequiresApprovalInsteadOfFalseReject() {
        ToolInvokeResult result = toolGateway.invoke(context(2L, null), "order.refund_execute", refundInput());
        assertThat(result.getStatus()).isEqualTo("APPROVAL_REQUIRED");
        assertThat(result.getApprovalId()).isNotNull();
        assertThat(external.effectiveEffectCount()).isZero();
    }

    @Test
    void legitimateApprovedOperatorRefundReachesOneExternalEffect() {
        Map<String, Object> input = refundInput();
        ToolInvokeResult first = toolGateway.invoke(context(2L, null), "order.refund_execute", input);
        assertThat(first.getStatus()).isEqualTo("APPROVAL_REQUIRED");

        ApprovalDecisionParam decision = new ApprovalDecisionParam();
        decision.setComment("Phase 5 JDBC governance integration approval");
        decision.setConfirmText("确认通过");
        approvals.approve(1L, 1L, first.getApprovalId(), 1L, "admin", decision).orElseThrow();

        ToolInvokeResult executed = toolGateway.invoke(context(2L, first.getApprovalId()), "order.refund_execute", input);
        assertThat(executed.getStatus()).isEqualTo("SUCCESS");
        assertThat(external.effectiveEffectCount()).isEqualTo(1);
    }


    @Test
    void refundAmountCannotExceedRemainingOrderValue() {
        Map<String, Object> input = refundInput();
        input.put("refundAmount", 1000);
        ToolInvokeResult result = toolGateway.invoke(context(2L, null), "order.refund_execute", input);
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorCode()).isEqualTo("BUSINESS_SCOPE_VIOLATION");
        assertThat(external.effectiveEffectCount()).isZero();
    }

    @Test
    void trustedShopCannotRefundOrderOwnedByAnotherShop() {
        jdbcTemplate.update("INSERT INTO shop_order (id, tenant_id, shop_id, order_no, user_id, order_status, pay_amount, refund_amount, paid_at, created_at, updated_at) " +
                "VALUES (99001, 1, 2, 'SO-GOV-FOREIGN-001', 301, 'FINISHED', 10.00, 0.00, NOW(), NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE shop_id=VALUES(shop_id), updated_at=NOW()");
        Map<String, Object> input = refundInput();
        input.put("orderId", "SO-GOV-FOREIGN-001");
        ToolInvokeResult result = toolGateway.invoke(context(2L, null), "order.refund_execute", input);
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorCode()).isEqualTo("BUSINESS_SCOPE_VIOLATION");
        assertThat(external.effectiveEffectCount()).isZero();
    }

    private ToolInvokeContext context(long userId, Long approvalId) {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(userId);
        context.setTaskId(1L);
        context.setTraceId("phase5-jdbc-governance-" + UUID.randomUUID());
        context.setApprovalId(approvalId);
        context.setManualInvoke(true);
        return context;
    }

    private Map<String, Object> refundInput() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("shopId", 1L);
        input.put("orderId", "SO202607180001");
        input.put("refundAmount", 10);
        input.put("operationRequestId", "PHASE5-JDBC-" + UUID.randomUUID());
        input.put("reason", "governance integration verification");
        input.put("simulation", "success");
        return input;
    }

    @TestConfiguration
    static class Infrastructure {
        @Bean
        @Primary
        RecordingRefundExternalSystem recordingRefundExternalSystem() {
            return new RecordingRefundExternalSystem();
        }

    }
}
