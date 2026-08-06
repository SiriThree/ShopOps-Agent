package com.sirithree.shopops.admin.mcp.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sirithree.shopops.admin.business.service.CommentRiskService;
import com.sirithree.shopops.admin.mcp.domain.McpClientException;
import com.sirithree.shopops.admin.mcp.domain.McpVerifiedCallResult;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryCommerceMcpClientTest {
    @Test
    void shouldCallCommentRiskServiceWhenSchemaHashMatches() {
        CommentRiskService commentRiskService = mock(CommentRiskService.class);
        when(commentRiskService.queryNegativeComments(
                1L,
                1L,
                LocalDate.parse("2026-07-18"),
                LocalDate.parse("2026-07-18"),
                3))
                .thenReturn(Map.of("negativeCount", 2));
        InMemoryCommerceMcpClient client = new InMemoryCommerceMcpClient(commentRiskService);

        McpVerifiedCallResult result = client.discoverAndCall(
                trustedContext(),
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                validArguments(),
                CommerceMcpContracts.commentQueryNegativeSchemaHash());

        org.assertj.core.api.Assertions.assertThat(result.discoveredTool().schemaHash())
                .isEqualTo(CommerceMcpContracts.commentQueryNegativeSchemaHash());
        org.assertj.core.api.Assertions.assertThat(result.callResult().structuredContent())
                .containsEntry("negativeCount", 2)
                .containsKey("scope");
        org.assertj.core.api.Assertions.assertThat(client.discoveryCallCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(client.toolCallCount()).isEqualTo(1);
        verify(commentRiskService).queryNegativeComments(
                1L,
                1L,
                LocalDate.parse("2026-07-18"),
                LocalDate.parse("2026-07-18"),
                3);
    }

    @Test
    void shouldRejectSchemaDriftBeforeTestScopeToolsCall() {
        CommentRiskService commentRiskService = mock(CommentRiskService.class);
        InMemoryCommerceMcpClient client = new InMemoryCommerceMcpClient(commentRiskService);
        ToolInvokeContext context = trustedContext();

        assertThatThrownBy(() -> client.discoverAndCall(
                context,
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                Map.of(
                        "shopId", 1,
                        "startDate", "2026-07-18",
                        "endDate", "2026-07-18",
                        "minStar", 3),
                "tampered-schema-hash"))
                .isInstanceOfSatisfying(McpClientException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getErrorCode())
                                .isEqualTo("MCP_TOOL_SCHEMA_MISMATCH"));

        org.assertj.core.api.Assertions.assertThat(client.discoveryCallCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(client.toolCallCount()).isZero();
        verifyNoInteractions(commentRiskService);
    }

    @Test
    void shouldRejectMissingSchemaHashBeforeTestScopeToolsCall() {
        CommentRiskService commentRiskService = mock(CommentRiskService.class);
        InMemoryCommerceMcpClient client = new InMemoryCommerceMcpClient(commentRiskService);

        assertThatThrownBy(() -> client.discoverAndCall(
                trustedContext(),
                CommerceMcpContracts.COMMENT_QUERY_NEGATIVE,
                validArguments(),
                " "))
                .isInstanceOfSatisfying(McpClientException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getErrorCode())
                                .isEqualTo("MCP_SCHEMA_HASH_MISSING"));

        org.assertj.core.api.Assertions.assertThat(client.discoveryCallCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(client.toolCallCount()).isZero();
        verifyNoInteractions(commentRiskService);
    }

    @Test
    void shouldRejectUnknownToolBeforeTestScopeToolsCall() {
        CommentRiskService commentRiskService = mock(CommentRiskService.class);
        InMemoryCommerceMcpClient client = new InMemoryCommerceMcpClient(commentRiskService);

        assertThatThrownBy(() -> client.discoverAndCall(
                trustedContext(),
                "comment.query_missing",
                validArguments(),
                CommerceMcpContracts.commentQueryNegativeSchemaHash()))
                .isInstanceOfSatisfying(McpClientException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getErrorCode())
                                .isEqualTo("MCP_TOOL_NOT_DISCOVERED"));

        org.assertj.core.api.Assertions.assertThat(client.discoveryCallCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(client.toolCallCount()).isZero();
        verifyNoInteractions(commentRiskService);
    }

    private ToolInvokeContext trustedContext() {
        ToolInvokeContext context = new ToolInvokeContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(1L);
        context.setTaskId(101L);
        context.setStepId(202L);
        context.setTraceId("trace-test-mcp-schema-drift");
        return context;
    }

    private Map<String, Object> validArguments() {
        return Map.of(
                "shopId", 1,
                "startDate", "2026-07-18",
                "endDate", "2026-07-18",
                "minStar", 3);
    }
}
