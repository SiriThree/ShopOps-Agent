# Phase 1 Handoff — Evaluation Runtime & Evidence Collector

## Phase objective

Phase 1 establishes the observation path required before ShopOps can calculate a credible End-to-End Agent Task Success metric:

```text
BenchmarkCase
 -> real ShopOps Agent Runtime
 -> production Task/Tool/Approval/WriteOperation/Report facts
 -> Evidence Collector
 -> Evaluator
 -> auditable EvaluationRecord
 -> JSON / Markdown report
```

It does not claim a formal Agent score.

## IMPLEMENTED vs RUNTIME VERIFIED

| Capability | Implemented | Runtime verified here |
|---|---:|---:|
| Benchmark RuntimeRequest Gold allowlist | yes | static + pure javac verified |
| TASK Runner/filter/single-case replay | yes | JUnit NOT RUN |
| HTTP real-runtime gateway | yes | JUnit NOT RUN |
| bounded task polling | yes | JUnit NOT RUN |
| Evidence Collector | yes | JUnit NOT RUN |
| EvidenceRef + sanitization | yes | JUnit NOT RUN |
| Evaluator SPI + five deterministic evaluators | yes | JUnit NOT RUN |
| Failure reason taxonomy | yes | pure javac verified for enum/contracts |
| Evaluation Run metadata | yes | pure javac verified |
| JSON/Markdown writer | yes | JUnit NOT RUN |
| planner fallback instrumentation | yes | Maven compile/test NOT RUN |
| WriteOperation task query instrumentation | yes | Maven compile/test NOT RUN |
| permission propagation fix | yes | Maven regression NOT RUN |
| smoke dataset | yes, 3 TASK cases | executions NOT RUN |
| Idempotency/Recovery/Governance runners | no | `NOT_IMPLEMENTED` by design |

## Real Runner entry

Current TASK runner uses:

```text
HttpShopOpsBenchmarkRuntime
 -> POST /api/agent/tasks/natural-language
 -> AgentTaskController.createTaskFromNaturalLanguage
 -> AgentTaskInterpreter / RuleBasedAgentTaskInterpreter
 -> AgentTaskService.createTask
 -> DefaultAgentEngineService
 -> RulePlannerService
 -> SequentialAgentExecutorService
 -> DefaultToolGatewayService
 -> LocalToolProvider / McpToolProvider
```

The Runner itself never implements planning or business behavior.

## Real Evidence sources

- Task + final state: `AgentTaskService.getTask`.
- State history: `AgentTaskService.listEvents`.
- Steps: `AgentTaskService.listSteps`.
- Tool logs: `ToolCallLogService.listByTaskId`.
- Trace/planner observation: `TraceService.listSpans` and `agent.planner` span.
- Approval: `ApprovalRequestService.list` filtered by task ID.
- Write operations: `WriteOperationService.listByTaskId`.
- Final report: `OperationReportService.getReport`.
- Business facts: deterministic projection from those facts.

## Identity propagation fact

Phase 0's hard-coded permission concern was confirmed. Before Phase 1, `SequentialAgentExecutorService.toToolContext(...)` created a fixed permission set. Phase 1 now resolves the trusted authorization snapshot with `AuthorizationService.resolve(tenantId, shopId, userId)` and places exactly those permissions into `ToolInvokeContext`.

Regression coverage was added to `SequentialAgentExecutorServiceTest`, but Maven is unavailable in this environment, so the test is **NOT RUN**.

`InMemoryAuthorizationService` remains an ADMIN-like fixture and is not suitable for measuring fine-grained governance. Later governance experiments should use JDBC authorization or another runtime with real role/permission facts.

## Interpreter / Planner modes

Current default smoke classification:

```text
interpreterMode = RULE_BASED
plannerMode = RULE_BASED (specialized intents or model planner disabled)
environment = DETERMINISTIC
```

`RulePlannerService` now exposes observation fields in `AgentTaskContext` and the `agent.planner` Trace span:

```text
plannerMode
fallback
fallbackReason
plannedToolCodes
```

This is instrumentation only; the fixed daily-review plan safety rule is not removed or loosened.

## Deterministic business outcomes currently judgeable

Phase 1 can machine-judge a deliberately narrow set:

- report required / absent;
- report evidence domain presence;
- explicit report status when specified;
- legal/forbidden Tool use independent of unique ordering;
- tool execution failure classification;
- approval evidence presence where explicitly required;
- unexpected internal write-operation presence for a zero-write case;
- final Agent task terminal state.

It cannot yet deterministically judge every free-form report semantic claim.

## Side effects currently observable

`WriteOperation` records for the task can be observed. That proves a production logical write lifecycle exists, but it does **not** automatically prove an effective external business side effect.

The following remain unavailable/generalized later:

- authoritative effective external side-effect count;
- duplicate effective external side-effect count;
- generic external-ground-truth ledger;
- RabbitMQ redelivery evidence;
- local-vs-external convergence across every operation type.

Accordingly those metrics remain null/unavailable rather than zero.

## Gold leakage protection

Runtime conversion is a strict allowlist. `BenchmarkRuntimeRequest` only exposes:

```text
caseId
scenario
input
identity
initialState
```

Gold fields do not exist on this type. `GoldLeakageProtectionTest` uses sentinel values to ensure serialized Runtime input does not contain them.

## Failure reason taxonomy

Stable reason codes now include:

```text
BUSINESS_OUTCOME_INCORRECT
REQUIRED_CAPABILITY_MISSING
FORBIDDEN_TOOL_USED
INVALID_TOOL_ARGUMENT
UNAUTHORIZED_EXECUTION
APPROVAL_BYPASS
UNEXPECTED_SIDE_EFFECT
DUPLICATE_SIDE_EFFECT
FINAL_STATE_INCORRECT
STATE_NOT_CONVERGED
EXTERNAL_STATE_MISMATCH
REPORT_INCONSISTENT
INTERPRETATION_ERROR
PLANNING_ERROR
TOOL_EXECUTION_ERROR
APPROVAL_REQUIRED
INFRASTRUCTURE_ERROR
EVALUATION_ERROR
BENCHMARK_TYPE_NOT_IMPLEMENTED
```

## Current smoke cases

```text
smoke-task-daily-review-001      clean read-only
smoke-task-comment-risk-001      clean multi-tool + MCP test boundary
smoke-task-degraded-ad-001       injected tool failure/degraded path
```

All are **NOT RUN** in this environment because Maven is unavailable. Do not convert them into a success percentage.

## Actual checks executed

```text
python3 scripts/phase8-static-validate.py
TOTAL=21 PASS=21 FAIL=0

Phase 1 JSON/static dataset validation
9 total versioned cases including smoke
0 JSON parse errors
0 duplicate case IDs
PASS

javac pure Phase 1 Benchmark contracts
PASS
```

Unavailable commands/infrastructure:

```text
mvn    NOT FOUND
docker NOT FOUND
pwsh   NOT FOUND
```

## KNOWN LIMITATIONS

1. Full Maven compilation and all Spring/JUnit executions are unverified here.
2. DETERMINISTIC memory authorization is too coarse for formal Governance metrics.
3. TASK runner has no generalized per-case dynamic fault-injection control; the degraded smoke uses the production test failpoint property in its dedicated Spring test context.
4. Outbox/RabbitMQ publish and redelivery evidence are not yet part of `ProductionBenchmarkEvidenceCollector`.
5. External business side effects are not inferred from Tool attempts; generic external ground truth is not yet available.
6. `provider` is not a first-class field in the current `ToolCallLog` projection; run-level tool provider mode and Trace/Tool evidence are available, but the collector does not invent a per-call provider field.
7. Planner `previewPlan` returned by the HTTP endpoint is separate from the actual planner observation; the latter is collected from the production `agent.planner` trace.
8. Phase 1 only implements TASK execution. Idempotency, Recovery and Governance execution paths intentionally report `NOT_IMPLEMENTED`.
9. Current workspace has no `.git` metadata; `gitCommit` is unavailable unless injected by `GIT_COMMIT`/`git.commit` in CI.

## Phase 2 real entry

Phase 2 should **reuse**, not rebuild:

- `ShopOpsBenchmarkRunner`;
- `HttpShopOpsBenchmarkRuntime`;
- `BenchmarkRuntimeRequest` Gold boundary;
- `ProductionBenchmarkEvidenceCollector`;
- `EvidenceRef` / `CollectedEvidence`;
- `CompositeTaskBenchmarkEvaluator` and evaluator SPI;
- `FailureReasonCode`;
- `EvaluationRunMetadataFactory`;
- `BenchmarkReportWriter`;
- `scripts/run-shopops-benchmark.ps1`.

Phase 2 should concentrate on the actual End-to-End Task Benchmark: richer independent task Gold, deterministic business outcome judges per scenario, acceptable alternative traces, clarification/parameter-missing cases, and formal dev/validation/test execution — only after the Phase 1 Maven/integration suite is runtime verified.

## Existing evaluation preservation

The Phase 0/legacy fixed-workflow regressions were not deleted or rewritten:

```text
AgentEvaluationIntegrationTest
AgentEvaluationModelIntegrationTest
AgentEvaluationDegradedIntegrationTest
```

They remain fixed Workflow Regression tests. ShopOpsBench Phase 1 is additive and does not reinterpret their historical pass condition as End-to-End Task Success.


> Environment note: Maven installation was also attempted with `apt-get`, but the command timed out and Maven was still absent afterward. No test result is inferred from that failed installation attempt.
