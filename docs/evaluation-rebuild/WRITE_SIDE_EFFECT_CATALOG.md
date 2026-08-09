# ShopOps Write Side-Effect Catalog — Phase 3

## Audit rule

This catalog is derived from current Java production code, Flyway migrations, and tool metadata. A tool is not treated as a real external write merely because its metadata says `write` or its output contains `UPDATED` / `SYNCED`.

## Catalog

| Operation | Tool | Real entry | Production executor / service | Approval | Risk / permission | Idempotency identity | External side effect / identity | Reconciliation | Current reachability | Benchmark status |
|---|---|---|---|---|---|---|---|---|---|---|
| Refund execution | `order.refund_execute` | `DefaultToolGatewayService.invoke` | `HighRiskRefundExecuteExecutor` → `WriteOperationService` → `RefundExternalClient` | Required | HIGH / `order:refund` | `toolCode:tenant:shop:orderId:operationRequestId`; `write_operation.idempotency_key` UNIQUE in V20 | Refund accepted by external transport; external refund/effect reference | `WriteOperationReconciliationService` for external reference + `EXTERNAL_UNKNOWN` | `TOOL_GATEWAY`, `WRITE_OPERATION`; not planned by current NL Agent | **FLAGSHIP** |
| Product title update | `product.update_title` | Tool Gateway | `ProductUpdateTitleExecutor` / `PortfolioOperationToolExecutor` | Required by catalog | HIGH / `product:write` | Tool metadata marks non-idempotent; no `WriteOperationService` path | **No persistent/external mutation in current executor.** It returns an `UPDATED` response only | None | `TOOL_GATEWAY`; not current NL Agent | Excluded from formal side-effect benchmark |
| Feishu report sync | `feishu.sync_report` | Tool Gateway | `FeishuSyncReportExecutor` | No approval in V19 | MEDIUM / `feishu:write` | Catalog marks idempotent, but no ShopOps `WriteOperation` identity | Real HTTP webhook only when connector enabled; current code fabricates document identity from report id and does not query durable external revision | None | `TOOL_GATEWAY`; not current NL Agent | Extension candidate; not flagship |
| Product optimize title | `product.optimize_title` | Tool Gateway | portfolio executor path | No high-risk approval | MEDIUM / `product:write` | Catalog marks idempotent | Current implementation produces suggestion-like output, not durable external mutation | None | Tool Gateway | Not a proven side effect |
| Ad budget suggestion | `ad.suggest_budget` | Tool Gateway | portfolio executor path | Required | HIGH / `ad:write` | Catalog marks non-idempotent | Current implementation does not expose an authoritative external campaign revision | None | Tool Gateway | Not a proven side effect |
| Comment reply draft | `comment.create_reply_draft` | Tool Gateway | portfolio executor path | No | MEDIUM / `comment:write` | Catalog marks idempotent | Draft response only; no independent external object source found | None | Tool Gateway | Not a proven side effect |

## Flagship chain

```text
ToolGatewayService
  -> DefaultToolGatewayService
  -> schema / scope / permission / risk
  -> ApprovalRequestService
  -> HighRiskRefundExecuteExecutor
  -> WriteOperationService.prepare
  -> RefundExternalClient
  -> RefundExternalTransport
  -> WriteOperationService.externalSucceeded / externalUnknown / failed
  -> Outbox (JDBC success / unknown paths)
```

`V20__phase2_write_reliability.sql` creates `write_operation`, gives `idempotency_key` a unique key, and updates `order.refund_execute` to `idempotent=1`, `retry_count=0`, `risk_level=HIGH`, `need_approval=1`, permission `order:refund`.

## Idempotency ownership

### Memory mode

Phase 3 changed `WriteOperationService.prepareMemory` from a non-atomic lookup/insert pattern to one `ConcurrentHashMap.compute` decision. Only the creator receives `freshExecution=true`; replay callers receive detached snapshots with `freshExecution=false`.

### JDBC mode

The existing protection remains application + database:

1. read by idempotency key;
2. attempt insert;
3. DB unique constraint `uk_write_operation_idempotency` resolves concurrent first-writer races;
4. duplicate insert catches `DuplicateKeyException`, rereads the winner, and verifies semantic input hash;
5. state transitions use status + version compare-and-set semantics.

## Payload binding

The semantic input hash must bind business payload, not execution metadata. Phase 3 excludes top-level `approvalId` from the hash. Amount/order/operation request changes remain conflicts. This aligns idempotency payload binding with the existing approval binding behavior, which already canonicalizes approval identity separately.

## Reachability limitation

Current natural-language planning does **not** plan `order.refund_execute`, `product.update_title`, or `feishu.sync_report`. Phase 3 therefore reports the flagship benchmark execution level as `TOOL_GATEWAY` (or `WRITE_OPERATION` for narrow reliability tests), never as an Agent end-to-end write benchmark.
