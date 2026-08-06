# Phase 8 Real MCP Integration — Read-only First Batch

## Status

This document describes the implemented source change, not a claim that all Phase 8 acceptance gates have passed. Maven compilation and protocol execution remain unverified in the current restricted environment.

## SDK and transport

- Official SDK: `io.modelcontextprotocol.sdk`
- BOM: `mcp-bom:2.0.0`
- Modules: `mcp-core`, `mcp-json-jackson2`
- Protocol generation targeted by SDK: MCP 2025-11-25
- Transport: Streamable HTTP
- JSON implementation: Jackson 2, matching Spring Boot 3.3.x

The JDK HttpClient/Servlet implementation was chosen to avoid upgrading Spring Boot or introducing a parallel WebFlux stack.

## Independent process boundary

`shopops-commerce-mcp-server` is a separate Spring Boot jar on port 8090. It depends only on `shopops-common`, Spring Boot web/actuator, Micrometer, and the official MCP SDK. It does not depend on `shopops-admin` and imports no Admin service, mapper, state machine, or entity.

Its development/integration Commerce State is owned by `InMemoryCommerceCommentRepository`. This is a repeatable external-system simulator and not a call back into Admin.

## Real protocol path in source

```text
OfficialCommerceMcpClient.withInitializedClient
  -> HttpClientStreamableHttpTransport(/mcp)
  -> client.initialize()
  -> client.listTools()
  -> compare remote inputSchema hash with approved local hash
  -> client.callTool(CallToolRequest)
```

Server side:

```text
HttpServletStreamableServerTransportProvider(/mcp)
  -> McpServer.sync(...)
  -> tools capability
  -> addTool(CommentQueryNegativeMcpTool)
```

No ordinary REST controller is used for tool discovery or invocation.

## Governance lifecycle

`DefaultToolGatewayService` remains the single entry:

```text
Tool lookup
-> enabled
-> trusted shop normalization
-> input schema validation
-> permission
-> approval policy
-> unique ToolProvider selection
-> MCP discovery/schema-hash verification/tools-call
-> result normalization
-> Tool Call Log and Trace
```

`McpToolProvider` is the only production class outside the MCP client package that references `CommerceMcpClient`. Controllers, Planners, Agent Executors, and approval callbacks do not reference it.

## Discovery and schema governance

The MCP server provides `name`, `description`, and `inputSchema`. ShopOps stores an approved local schema hash. `OfficialCommerceMcpClient.discoverAndCall` performs `tools/list`, computes the canonical SHA-256 hash, and raises `MCP_TOOL_SCHEMA_MISMATCH` before `tools/call` when it differs.

The migration updates only remote-binding/discovery columns for `comment.query_negative`. Existing local permission/risk/approval/idempotency/enabled fields remain locally controlled.

Approved first-batch hash:

```text
f5448fec19329d3ccbd75397e888351ab8df725e0dabdee81df89f62454e3873
```

## Trusted context and authentication

Admin injects Bearer authentication plus tenant, shop, user, task, step, trace and optional approval identifiers as transport headers. The server extracts trusted context into `McpTransportContext`; tool arguments cannot override the trusted shop.

The Server refuses startup if `SHOPOPS_COMMERCE_MCP_TOKEN` is blank. Admin MCP is disabled by default and requires explicit token configuration. Tokens are never included in protocol evidence logs.

## `comment.query_negative`

Input:

- `shopId` (trusted and scope-checked)
- `startDate`
- `endDate`
- `minStar` (legacy field name; behavior is an inclusive maximum star threshold)

The tool queries independent tenant/shop-scoped Commerce state, applies date and star filters, and returns structured content containing count, comments, category statistics and effective scope.

## Error separation

Client mapping distinguishes:

- `MCP_CONNECT_TIMEOUT`
- `MCP_CALL_TIMEOUT`
- `MCP_CONNECT_FAILED`
- `MCP_PROTOCOL_ERROR`
- `MCP_TRANSPORT_ERROR`
- `MCP_REMOTE_ERROR`
- `MCP_TOOL_NOT_DISCOVERED`
- `MCP_TOOL_SCHEMA_MISMATCH`

## Tests added

- `McpProtocolRoundTripIntegrationTest`: embedded independent Server plus official client; asserts initialize/list/call counters.
- `OfficialCommerceMcpClientExternalIntegrationTest`: runs through `DefaultToolGatewayService` with only `McpToolProvider`; no local executor exists.
- `McpSchemaDriftExternalIntegrationTest`: initialize/list occur, drift rejects before call.
- `McpServerUnavailableIntegrationTest`: closed endpoint maps to connection failure.
- `McpToolGatewayGovernanceTest`: missing permission rejects before Provider selection/invocation.
- `scripts/phase8-mcp-readonly-smoke.sh`: starts the independent jar, runs normal and drift tests, and requires two initialize, two tools/list, exactly one tools/call.

## Verification status

Executed successfully:

```text
python scripts/phase8-static-validate.py
TOTAL=21 PASS=21 FAIL=0
```

Not executable in the current environment:

```text
mvn -pl shopops-commerce-mcp-server,shopops-admin -am test
```

Reason: `mvn` is not installed, the repository has no Maven Wrapper, Docker is unavailable, and outbound DNS/downloads are blocked. Therefore no claim is made that Java compilation or live MCP tests passed.
