# Phase 8 MCP Current State Audit

## Scope

This audit is based on the uploaded `ShopOps-main(4).zip`. The first delivery is intentionally limited to the read-only `comment.query_negative` protocol loop.

## Existing runtime before Phase 8

The production execution path was:

```text
Agent Executor
  -> DefaultToolGatewayService
  -> Map<String, ToolExecutor>
  -> in-process Java ToolExecutor
```

`mcp_tool`, `McpToolService`, and `McpToolController` were registry/governance names. The repository did not contain an MCP session, protocol initialization, capability negotiation, `tools/list`, `tools/call`, or a network transport.

## Existing governance retained

`DefaultToolGatewayService` already owned tool lookup, enabled checks, permission checks, approval checks, Tool Call Log, and Trace completion. This is retained as the only execution gateway.

## Reusable extension points

- `McpToolDto` and the `mcp_tool` table remain the local governance catalog.
- Existing `ToolExecutor` implementations remain available through `LocalToolProvider`.
- Existing approval, trace, and call-log services remain in the gateway.
- Agent Planner and Agent Executor continue to call `ToolGatewayService`; they do not receive an MCP client.

## Gaps found

1. No independent MCP server process.
2. No official MCP Java SDK dependency.
3. No provider routing between local and remote tools.
4. `input_schema`, provider identity, remote name, and schema hash were not sufficient to govern a remote binding.
5. No fail-closed behavior for remote schema drift.
6. No protocol-level integration tests.

## Minimal compatible design chosen

```text
DefaultToolGatewayService
  -> ToolProvider
       -> LocalToolProvider -> existing ToolExecutor
       -> McpToolProvider -> OfficialCommerceMcpClient
                              -> Streamable HTTP /mcp
                              -> independent Commerce MCP Server
```

The remote `tools/list` schema is compared with the locally approved `schema_hash`. A mismatch stops before `tools/call`. Remote discovery does not overwrite permission, risk, approval, idempotency, enabled state, or tenant policy.

## First-batch boundary

Implemented now:

- independent Commerce MCP Server;
- official Java MCP SDK client/server wiring;
- `initialize -> tools/list -> tools/call` code path;
- one real read-only tool: `comment.query_negative`;
- trusted tenant/shop/user/task/step/trace propagation;
- local schema, permission, provider, audit and trace path;
- protocol capture tests and failure tests.

Not implemented in this batch:

- `product.query_candidates`;
- `product.update_title`;
- `order.refund_execute`;
- immutable `ExecutionSnapshot`;
- high-risk approval callback execution;
- write idempotency and reconciliation;
- MCP server administration APIs and persisted discovery refresh.

## Compatibility risks

The build environment used for this delivery has Java 21 but no Maven binary, no Maven Wrapper, no Docker, and no network path to Maven Central. Source/POM/YAML/schema/static boundary validation was executed, but Maven compilation and live protocol tests could not be run in this environment. The exact raw output is recorded under the delivery logs.
