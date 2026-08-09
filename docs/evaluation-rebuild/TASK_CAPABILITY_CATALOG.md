# ShopOps Task Capability Catalog — Phase 2

This catalog is derived from the actual `RuleBasedAgentTaskInterpreter`, `RulePlannerService`, `SequentialAgentExecutorService`, Tool executors, and report assembly path. It describes what the current natural-language Agent can reach, not every Tool that happens to exist in the platform.

## Natural-language reachable task families

| Scenario | Interpreter intent | Production plan tools | Read / write | Approval | Deterministic business Source of Truth | NL reachable |
|---|---|---|---|---|---|---|
| Daily / order operations review | `daily_review` | `order.query_summary`, `comment.query_negative`, `product.query_candidates`, `ad.query_performance`, `report.query_external_metrics`, `report.generate_daily_review` | business reads + internal report artifact | No | structured outputs from all five read tools + `OperationReport.evidence` | YES |
| Comment risk handling / analysis | `comment_risk` | `order.query_summary`, `comment.query_negative`, `product.query_candidates`, `report.generate_daily_review` | read-only business analysis + report | No | negative comments, candidate products, order baseline, report evidence IDs | YES |
| Product optimization analysis | `product_optimization` | `order.query_summary`, `product.query_candidates`, `comment.query_negative`, `report.generate_daily_review` | analysis only; no product mutation | No | product candidates, comment signals, report product IDs | YES |
| Ad anomaly analysis | `ad_anomaly` | `order.query_summary`, `ad.query_performance`, `report.query_external_metrics`, `report.generate_daily_review` | read-only business analysis + report | No | ad performance, external metrics, report campaign names and `adDataStatus` | YES |

## Tools that exist but are not reachable from the current NL Planner

The repository contains additional production Tools, including:

- `order.refund_execute`
- `product.update_title`
- `feishu.sync_report`
- `report.export_excel`
- `comment.create_reply_draft`
- `product.optimize_title`
- `ad.suggest_budget`

Their existence does **not** mean the current natural-language Agent can plan them.

Phase 2 therefore does not fabricate `REPORT_SYNC`, refund-write, or product-write TASK cases. Those tools remain relevant to later dedicated governance/idempotency/recovery benchmarks or future production planner work.

`TaskCapabilityCatalog` records this distinction with `reachableFromNaturalLanguageAgent`.

## Interpreter facts

`RuleBasedAgentTaskInterpreter` currently routes by keyword groups:

- daily/report/review-style terms → `daily_review`;
- ad/advertising terms → `ad_anomaly`;
- negative comment/review terms → `comment_risk`;
- product/title/low-click terms → `product_optimization`;
- otherwise → `daily_review`.

There is no production multi-turn clarification state in this path. Missing date is resolved by the HTTP entry using a bounded same-day default rather than clarification.

Therefore:

- missing-parameter cases may legitimately pass via `SAFE_DEFAULT`;
- clarification metrics are **NOT_SUPPORTED** by the current runtime;
- ambiguous language can expose interpreter limitations and is not rewritten by the Benchmark Runner.

## Planner facts

For specialized intents (`comment_risk`, `product_optimization`, `ad_anomaly`) the current planner uses deterministic rule plans.

For `daily_review`, model planning may be enabled, but `RulePlannerService` still validates the model output against the six-step fixed daily-review sequence. Rejected/invalid model output falls back to the rule plan and records:

- `plannerMode`;
- `fallback`;
- `fallbackReason`;
- `plannedToolCodes`.

Phase 2 does not remove that fixed-sequence safety constraint.

## Schema-error capability

The current planner emits Tool codes, while `SequentialAgentExecutorService` constructs the actual Tool input maps. There is no current production planner output contract that directly supplies arbitrary Tool arguments.

Therefore a benchmark case that asks the model planner to emit a malformed `shopId/startDate/...` Tool JSON cannot be honestly driven end-to-end without introducing a new planning contract. Phase 2 implements evaluator/reason-code tests for `MCP_INPUT_INVALID`, but marks full NL-Agent schema-error injection as **NOT_SUPPORTED_BY_CURRENT_PLANNER_OUTPUT_CONTRACT**.

## Report sync

`feishu.sync_report` is a real Tool, but there is no current natural-language intent/plan that reaches it. `ReportSyncOutcomeEvaluatorTest` explicitly protects against accidentally claiming this capability.
