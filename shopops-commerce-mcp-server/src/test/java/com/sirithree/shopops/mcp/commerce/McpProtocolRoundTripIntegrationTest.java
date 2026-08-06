package com.sirithree.shopops.mcp.commerce;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.common.mcp.CommerceMcpContracts;
import com.sirithree.shopops.mcp.commerce.observability.McpProtocolCounters;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.mcp.commerce.bearer-token=phase8-test-token"
)
class McpProtocolRoundTripIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private McpProtocolCounters counters;

    @Test
    void shouldInitializeDiscoverAndCallNegativeCommentToolOverStreamableHttp() {
        HttpClientStreamableHttpTransport transport =
                HttpClientStreamableHttpTransport
                        .builder("http://127.0.0.1:" + port)
                        .endpoint("/mcp")
                        .httpRequestCustomizer(
                                (request, method, endpoint, body, transportContext) -> {
                                    request.header(
                                            "Authorization",
                                            "Bearer phase8-test-token"
                                    );
                                    request.header(
                                            CommerceMcpContracts.HEADER_TENANT_ID,
                                            "1"
                                    );
                                    request.header(
                                            CommerceMcpContracts.HEADER_SHOP_ID,
                                            "1"
                                    );
                                    request.header(
                                            CommerceMcpContracts.HEADER_USER_ID,
                                            "7"
                                    );
                                    request.header(
                                            CommerceMcpContracts.HEADER_TASK_ID,
                                            "1001"
                                    );
                                    request.header(
                                            CommerceMcpContracts.HEADER_STEP_ID,
                                            "2001"
                                    );
                                    request.header(
                                            CommerceMcpContracts.HEADER_TRACE_ID,
                                            "trace-phase8-roundtrip"
                                    );


                                }
                        )
                        .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(5))
                .build();

        try {
            InitializeResult initializeResult = client.initialize();

            assertThat(initializeResult.protocolVersion())
                    .isNotBlank();

            assertThat(initializeResult.serverInfo().name())
                    .isEqualTo("shopops-commerce-mcp-server");

            ListToolsResult toolsResult = client.listTools();

            Tool negativeComment = toolsResult.tools().stream()
                    .filter(tool ->
                            CommerceMcpContracts.COMMENT_QUERY_NEGATIVE
                                    .equals(tool.name()))
                    .findFirst()
                    .orElseThrow();

            assertThat(
                    CommerceMcpContracts.sha256(
                            CommerceMcpContracts.canonicalJson(
                                    negativeComment.inputSchema()
                            )
                    )
            ).isEqualTo(
                    CommerceMcpContracts.commentQueryNegativeSchemaHash()
            );

            CallToolResult callResult = client.callTool(
                    CallToolRequest
                            .builder(
                                    CommerceMcpContracts.COMMENT_QUERY_NEGATIVE
                            )
                            .arguments(Map.of(
                                    "shopId", 1,
                                    "startDate", "2026-08-01",
                                    "endDate", "2026-08-03",
                                    "minStar", 3
                            ))
                            .build()
            );

            assertThat(callResult.isError())
                    .isNotEqualTo(Boolean.TRUE);

            assertThat(callResult.structuredContent())
                    .isInstanceOf(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> structured =
                    (Map<String, Object>) callResult.structuredContent();

            // JSON 数值类型可能是 Integer 或 Long，因此统一按 Number 验证
            Object negativeCountValue = structured.get("negativeCount");

            assertThat(negativeCountValue)
                    .isInstanceOf(Number.class);

            assertThat(((Number) negativeCountValue).longValue())
                    .isEqualTo(3L);

            Object scopeValue = structured.get("scope");

            assertThat(scopeValue)
                    .isInstanceOf(Map.class);

            Map<?, ?> scope = (Map<?, ?>) scopeValue;

            Object tenantIdValue = scope.get("tenantId");
            Object shopIdValue = scope.get("shopId");

            assertThat(tenantIdValue)
                    .isInstanceOf(Number.class);

            assertThat(((Number) tenantIdValue).longValue())
                    .isEqualTo(1L);

            assertThat(shopIdValue)
                    .isInstanceOf(Number.class);

            assertThat(((Number) shopIdValue).longValue())
                    .isEqualTo(1L);

            assertThat(counters.count("initialize"))
                    .isGreaterThanOrEqualTo(1);

            assertThat(counters.count("tools/list"))
                    .isGreaterThanOrEqualTo(1);

            assertThat(counters.count("tools/call"))
                    .isGreaterThanOrEqualTo(1);
        }
        finally {
            client.closeGracefully();
        }
    }
}