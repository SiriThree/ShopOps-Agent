package com.sirithree.shopops.admin.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.mcp.service.McpProtocolService;
import com.sirithree.shopops.admin.mcp.stdio.ShopOpsMcpStdioServerApplication;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "shopops.persistence=memory"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class McpStdioServerIntegrationTest {
    @Autowired
    private McpProtocolService protocolService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @SuppressWarnings("unchecked")
    void shouldServeMcpRequestsOverLineDelimitedStdio() throws Exception {
        String input = String.join("\n",
                objectMapper.writeValueAsString(Map.of(
                        "jsonrpc", "2.0",
                        "id", "init-1",
                        "method", "initialize"
                )),
                objectMapper.writeValueAsString(Map.of(
                        "jsonrpc", "2.0",
                        "id", "tools-1",
                        "method", "tools/list"
                )),
                objectMapper.writeValueAsString(Map.of(
                        "jsonrpc", "2.0",
                        "id", "call-1",
                        "method", "tools/call",
                        "params", Map.of(
                                "name", "order.query_summary",
                                "arguments", Map.of("startDate", "2018-08-07", "endDate", "2018-08-07")
                        )
                ))
        ) + "\n";

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ShopOpsMcpStdioServerApplication(protocolService, objectMapper).run(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8)
        );

        List<String> lines = output.toString(StandardCharsets.UTF_8).lines().toList();
        assertThat(lines).hasSize(3);

        Map<String, Object> initialize = objectMapper.readValue(lines.get(0), Map.class);
        assertThat((Map<String, Object>) initialize.get("result"))
                .containsEntry("protocolVersion", McpProtocolService.PROTOCOL_VERSION);

        Map<String, Object> listTools = objectMapper.readValue(lines.get(1), Map.class);
        Map<String, Object> listResult = (Map<String, Object>) listTools.get("result");
        assertThat((List<Map<String, Object>>) listResult.get("tools")).hasSize(18);

        Map<String, Object> callTool = objectMapper.readValue(lines.get(2), Map.class);
        Map<String, Object> callResult = (Map<String, Object>) callTool.get("result");
        assertThat(callResult).containsEntry("isError", false);
        Map<String, Object> structured = (Map<String, Object>) callResult.get("structuredContent");
        assertThat(structured)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS");
    }
}
