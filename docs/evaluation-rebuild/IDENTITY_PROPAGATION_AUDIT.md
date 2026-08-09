# Phase 1 Identity Propagation Audit

## Scope

This audit was rebuilt from the current ShopOps source. It does not treat Phase 0 prose as proof of runtime behavior.

## Confirmed propagation chain

```text
HTTP headers
  -> RequestContextResolver
  -> RequestContextHolder
  -> AgentTaskController.createTaskFromNaturalLanguage
  -> JdbcAgentTaskService.createTask
  -> AgentTaskContext
  -> DefaultAgentEngineService
  -> SequentialAgentExecutorService.toToolContext
  -> DefaultToolGatewayService.invoke
  -> LocalToolProvider / McpToolProvider
  -> Business service / CommerceMcpClient
```

| Boundary | tenant | shop | user | permission | trusted source |
|---|---|---|---|---|---|
| HTTP -> RequestContext | `X-Tenant-Id` | `X-Shop-Id` | `X-User-Id` | resolved authorization snapshot; roles may be header/profile input depending runtime mode | `RequestContextResolver` + `AuthorizationService` |
| RequestContext -> AgentTask create | yes | yes | yes | not persisted as a caller-supplied permission list | `RequestContextHolder.current()` in `AgentTaskController` |
| AgentTask -> AgentTaskContext | yes | yes | yes | n/a | persisted task + create context in `JdbcAgentTaskService` |
| AgentTaskContext -> ToolInvokeContext | yes | yes | yes | **resolved again from `AuthorizationService`** | `SequentialAgentExecutorService.toToolContext` |
| ToolInvokeContext -> ToolGateway | yes | yes | yes | `ToolInvokeContext.hasPermission(tool.permissionCode)` | `DefaultToolGatewayService.invoke` |
| ToolGateway -> MCP | trusted context retained | trusted context retained | trusted context retained | gateway has already enforced permission | `McpToolProvider` / `CommerceMcpClient` |
| LLM/tool arguments -> business scope | may contain IDs in arguments | may contain shop ID in arguments | should not be trusted as execution identity | never authoritative | trusted input normalization + gateway/provider validation |

## Phase 0 finding: hard-coded permissions

The Phase 0 source had `SequentialAgentExecutorService.toToolContext(...)` constructing a fixed permission set. That meant an Agent execution could reach the Tool Gateway with permissions that did not faithfully represent the caller authorization snapshot.

Phase 1 makes the minimum production correction:

```java
AuthorizationService.AuthorizationSnapshot authorization = authorizationService.resolve(
    context.getTenantId(), context.getShopId(), context.getUserId());
toolContext.setPermissions(authorization.permissions());
```

This is intentionally not a Benchmark-only bypass. The production executor now asks the production authorization boundary for the trusted permission set before each Tool invocation. `SequentialAgentExecutorServiceTest.shouldPropagatePermissionsFromTrustedAuthorizationServiceInsteadOfHardCodingThem` is the regression test.

## Trusted execution identity vs model/tool-supplied identity

The Benchmark treats `tenantId/shopId/userId` carried through the trusted execution context as authoritative. IDs emitted by a model or included in tool arguments are data and must not override that context. The existing test MCP client already rejects `arguments.shopId` when it conflicts with `ToolInvokeContext.shopId`.

## Important limitation: memory authorization mode

`InMemoryAuthorizationService` is intentionally simplified and resolves the valid memory fixture to an ADMIN-like authorization snapshot. Therefore a DETERMINISTIC memory smoke run is suitable for execution plumbing, but **not** evidence that fine-grained OPERATOR/VIEWER authorization works. Governance scores that depend on role distinctions must later run against a runtime where `JdbcAuthorizationService` and its real role/permission data are active.

## Approval identity

Approval records are collected from the production `ApprovalRequestService`. Phase 1 records only fields actually exposed by the current approval DTO. If payload binding/hash is absent in the production model, the Evaluation Runtime does not invent it.

## Benchmark impact

The identity propagation fix is necessary so future Governance cases observe the same permission decision used by real Tool execution. Phase 1 does **not** claim Unauthorized Block Rate, False Reject Rate, cross-tenant effective-write count, or approval-bypass rate from the smoke dataset.
