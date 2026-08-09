# Phase 0 — Current Evaluation Audit

## Scope and evidence rule

This audit is based on the source tree contained in the supplied ShopOps archive. README claims and previously generated reports are treated as evidence artifacts, not as proof that a capability still works. The extracted archive has no usable `.git` history, so a commit SHA cannot be recovered from this input.

## 1. Real Agent task entry and execution chain

### Natural-language entry

```text
POST /api/agent/tasks/natural-language
  -> AgentTaskController.createTaskFromNaturalLanguage
  -> RuleBasedAgentTaskInterpreter.interpret
       -> keyword routing to daily_review/comment_risk/product_optimization/ad_anomaly
  -> RulePlannerService.previewPlan (response preview only)
  -> AgentTaskService.createTask
       memory: InMemoryAgentTaskService
       jdbc:   JdbcAgentTaskService
  -> AgentTaskDispatcher
       synchronous: SynchronousAgentTaskDispatcher
       rabbitmq:    RabbitAgentTaskDispatcher.dispatch
                     -> RabbitAgentTaskConsumer.consume
                     -> JdbcAgentTaskExecutionWorker.execute
  -> DefaultAgentEngineService.executeTask
  -> RulePlannerService.createPlan
       non-daily-review intents: deterministic rulePlan
       daily_review + model planner enabled: modelPlanOrFallback
       model output is accepted only if it matches the fixed six-tool daily-review sequence
  -> SequentialAgentExecutorService.execute
  -> DefaultToolGatewayService.invoke
  -> LocalToolProvider or McpToolProvider
  -> local Business Service / CommerceMcpClient
  -> for governed write tools: ApprovalRequestService / WriteOperationService
  -> persistence: AgentTask / AgentTaskStep / AgentTaskEvent / ToolCallLog / ApprovalRequest / WriteOperation
  -> OperationReportService + AgentTask final state
```

Key code facts:

- `AgentTaskController.createTaskFromNaturalLanguage` always sets `taskType` to `daily_review`, while the interpreter supplies an intent.
- `RuleBasedAgentTaskInterpreter.routeIntent` is keyword based and defaults unknown input to `daily_review`.
- `RulePlannerService.createPlan` uses rule plans for the three specialized intents. The optional model planner is only used for `daily_review`, and `isSafeDailyReviewPlan` rejects any model plan that differs from the exact six-tool sequence.
- `SequentialAgentExecutorService.execute` calls the real `ToolGatewayService`; non-critical tool failures mark the task degraded while failure of `order.query_summary` or `report.generate_daily_review` aborts the execution result.
- `JdbcAgentTaskExecutionWorker.execute` validates persisted dispatch identity and rechecks `AGENT_EXECUTE` authorization before acquiring a task lease.
- The worker maps `AgentTaskStatus.SUCCEEDED` to persisted string `SUCCESS` and `NEEDS_MANUAL_ACTION` to persisted string `DEGRADED`. Benchmark code must therefore preserve the real state vocabulary instead of inventing a replacement state machine.

### Tool / write path

`DefaultToolGatewayService.invoke` performs tool lookup, permission/schema/governance checks, approval creation/verification, provider selection, invocation and tool-call logging. Approval state is handled by `JdbcApprovalRequestService` (`PENDING -> APPROVED -> EXECUTING -> EXECUTED`, with failure/expiry alternatives).

The write reliability state machine is defined by `WriteOperationStatus`:

```text
CREATED
  -> WAITING_APPROVAL | APPROVED | FAILED
APPROVED
  -> EXECUTING | FAILED
EXECUTING
  -> EXTERNAL_UNKNOWN | EXTERNAL_SUCCEEDED | FAILED
EXTERNAL_UNKNOWN
  -> EXTERNAL_SUCCEEDED | FAILED | NEEDS_RECONCILIATION
EXTERNAL_SUCCEEDED
  -> LOCAL_CONFIRMED | NEEDS_RECONCILIATION
LOCAL_CONFIRMED
  -> SUCCEEDED | NEEDS_RECONCILIATION
NEEDS_RECONCILIATION
  -> EXTERNAL_SUCCEEDED | LOCAL_CONFIRMED | FAILED
```

`WriteOperationService.prepare` derives an idempotency key from tool + tenant + shop + business object + operation request id. A replay with the same key returns the existing operation after checking the input hash. `WriteOperationReconciliationService.reconcile` currently knows how to query external reality for `order.refund_execute` operations with an external reference and can recover `EXTERNAL_UNKNOWN` when the external system reports success.

## 2. What the current Evaluation actually judges

The three JUnit suites inherit `AbstractAgentEvaluationIntegrationTestSupport`:

- `AgentEvaluationIntegrationTest`: 7 cases, memory persistence.
- `AgentEvaluationModelIntegrationTest`: 4 cases, memory persistence, report model gateway configured to the `echo` provider.
- `AgentEvaluationDegradedIntegrationTest`: 3 cases, memory persistence, forced failure for `comment.query_negative`.

A case passes only when `CaseResult.mismatches` is empty. The suite then asserts `passedCaseCount == cases.size()`.

### `expectedToolCodes`

`applyExpectations` uses `Objects.equals(expectations.expectedToolCodes, result.actualToolCodes)`. This requires exact list equality, including order and cardinality. It therefore encodes one expected workflow trace, not an equivalence class of acceptable business executions.

### Final state

`finalStatusIn` checks only whether the observed task/invoke status belongs to an allowed string list. For daily-review cases the test fetches the task after creation and reads its persisted status. It does not independently prove that the requested business outcome is correct.

### Does a model really participate?

- The normal and degraded suites do not require a model for interpretation; interpretation is `RuleBasedAgentTaskInterpreter`.
- The model suite enables the **report** model gateway with provider `echo` and checks evidence such as `generationMode=MODEL_GATEWAY` and `modelProviderCode=echo`. This proves the model-gateway code path was exercised, not that a semantic model judged or planned the task correctly.
- The optional planner model is constrained by `RulePlannerService.isSafeDailyReviewPlan` to the same fixed six-tool trace and falls back to rules on error or mismatch.

### Are tools really executed?

Yes, the integration tests call HTTP endpoints and the real `SequentialAgentExecutorService -> ToolGatewayService` path in the Spring test application. However `shopops.persistence=memory` means these suites do not prove JDBC persistence, RabbitMQ delivery or external connector behavior.

### Does the database really change?

Not for the core 14-case Agent evaluation suites: they explicitly use memory persistence. A report object and in-memory task/step state can be created, but these suites are not evidence of MySQL transactional behavior.

### Are approvals really created?

Only `manual_tool_invoke` cases observe whether an `approvalId` is returned. The daily-review cases initialize `actualApprovalCreated=false` and do not scan approval events/write operations as part of success. Approval integration tests elsewhere are valuable regression tests, but they are not folded into current Agent task success.

### Do external side effects really happen?

The current Agent evaluation success criteria do not establish that an externally visible business mutation occurred exactly once. A successful provider result or HTTP 200 is not equivalent to a verified effective external side effect.

### How degraded cases are judged

`AgentEvaluationDegradedIntegrationTest` sets `shopops.tool.fail-code=comment.query_negative`. The evaluator expects task status `DEGRADED`, an expected fixed tool list, a report/evidence shape and selected config values. It does not inject an uncertain write failure and then verify local/external convergence.

## 3. Audit of the “280 natural-language tasks”

The 280 figure is not sourced from `agent-cases-v1.json`. It comes from `scripts/run-agent-natural-language-batch.ps1` plus the generated artifact dated 2026-07-28.

Generation formula:

```text
4 fixed English prompt templates
x 7 fixed dates (2018-08-01 ... 2018-08-07)
x 10 rounds
= 280 executions
```

The script's default is one round; the stored report records `Rounds: 10`. Therefore there are only 28 distinct template/date inputs in that run, and each is repeated ten times.

The script marks a case PASS when all of these hold:

```text
request flow completed
AND taskStatus in SUCCESS/DEGRADED/APPROVAL_REQUIRED
AND rule-based actualIntent == template expectedIntent
AND report exists
AND evidence.dataSources exists
```

It does not compare an independent business-outcome Gold, external side effects, approval correctness, cross-tenant isolation, duplicate effects or recovery convergence. The prompts also contain direct lexical cues (`daily report`, `negative comment`, `product`, `ad/campaign`) that line up with the keyword router.

### Dataset realism classification

| Existing artifact | Classification | Reason |
|---|---|---|
| 14 JSON integration cases | **KEEP + MIGRATE** | Keep as deterministic regression tests. Migrate selected scenarios into capability/outcome-based ShopOpsBench Gold, but do not preserve exact tool-trace equality as task success. |
| 280 natural-language batch executions | **KEEP as regression evidence; REGENERATE for benchmark use** | Useful for repeated-path stability/latency evidence. Too template-repetitive and router-revealing for a held-out task benchmark. |
| generated 280 summary/CSV | **KEEP as historical artifact** | It is reproducibility evidence for a past run, not a current ShopOpsBench score. |
| any claim treating 280/280 as end-to-end business success | **DEPRECATE** | The stored pass predicate is materially narrower than TaskSuccess v1. |

### Leakage / split status

No dev/validation/test isolation is present for the old evaluation resources. The expected traces and keyword-rich prompts live in the same repository as the rule implementation, and there is no independent reviewed Gold provenance. Phase 0 therefore does not promote any old case directly into the held-out `test` score.

## 4. Observability gaps relevant to ShopOpsBench

The production code already exposes useful evidence sources: Agent task/step/event records, tool-call logs, approval records, trace spans, write operations and outbox events. The main missing benchmark layer is correlation and semantic adjudication: Phase 0 defines an `EvaluationRecord` capable of holding these observations, but deliberately does not change runtime behavior to collect new evidence.

One governance caveat is important for later phases: `SequentialAgentExecutorService.toToolContext` currently supplies a hard-coded permission set for Agent-executed tools. Governance cases therefore must distinguish API/user authorization, worker authorization and tool-context permission propagation rather than assuming the task user's complete permission set is forwarded unchanged to every tool call.
