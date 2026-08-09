# Phase 1 Evaluation Runtime

## 1. Real Runner entry

TASK smoke cases enter the actual HTTP production surface through `HttpShopOpsBenchmarkRuntime`:

```text
ShopOpsBenchmarkRunner
 -> HttpShopOpsBenchmarkRuntime
 -> POST /api/agent/tasks/natural-language
 -> AgentTaskController.createTaskFromNaturalLanguage
 -> AgentTaskInterpreter / RuleBasedAgentTaskInterpreter
 -> AgentTaskService.createTask
 -> DefaultAgentEngineService.executeTask
 -> RulePlannerService
 -> SequentialAgentExecutorService
 -> DefaultToolGatewayService.invoke
 -> LocalToolProvider / McpToolProvider
 -> business/MCP implementation
```

The runner polls the real `/api/agent/tasks/{taskId}` endpoint with a bounded timeout. It uses `LockSupport.parkNanos` rather than an unbounded `Thread.sleep` loop.

## 2. Production components actually invoked

When `BenchmarkRunnerLifecycleTest` is executed under Spring Boot memory mode it is designed to invoke the real Controller, Interpreter, task service, engine, planner, executor, Tool Gateway, Tool providers, task/trace/log/report services and production in-memory persistence implementations.

The only deliberate infrastructure test double is `InMemoryCommerceMcpClient`, which implements the production `CommerceMcpClient` boundary and preserves MCP discovery/schema-hash/call semantics. It does not replace the Agent, planner, Tool Gateway or business execution chain.

## 3. Runner responsibilities

`ShopOpsBenchmarkRunner` only:

1. filters cases;
2. creates the runtime-safe request;
3. invokes the runtime gateway;
4. collects evidence;
5. invokes evaluators;
6. produces `EvaluationRecord`;
7. aggregates run counts.

It never selects a tool, repairs a plan, changes a production result, or reads Gold before execution.

## 4. Evaluator SPI

```java
EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence)
```

Phase 1 includes:

- `DeterministicBusinessOutcomeEvaluator`;
- `ToolLegalityEvaluator`;
- `GovernanceEvidenceEvaluator`;
- `UnexpectedSideEffectEvaluator`;
- `FinalStateEvaluator`;
- `CompositeTaskBenchmarkEvaluator`.

Business-outcome judging is intentionally narrow: report existence, report evidence domains, explicit report state, and production state facts. No LLM-as-a-Judge is introduced.

## 5. Runtime failure vs Benchmark failure

`CaseExecutionStatus` separates:

```text
EXECUTED
PASSED
FAILED
NOT_EXECUTED
ERROR
```

Infrastructure failures map to `INFRASTRUCTURE_ERROR`/`ERROR` rather than silently becoming an Agent Task failure. Unsupported Phase 1 benchmark types map to `NOT_EXECUTED` + `BENCHMARK_TYPE_NOT_IMPLEMENTED`.

## 6. Environment classification

- **DETERMINISTIC**: rule-based/deterministic orchestration with test infrastructure, while still executing the real ShopOps business runtime.
- **INTEGRATION**: real local JDBC/MySQL/RabbitMQ/MCP infrastructure.
- **MODEL**: a real LLM participates in interpretation/planning while external business dependencies may remain controlled adapters.
- **EXTERNAL**: real third-party business systems participate and can provide external ground truth.

The current smoke contract is labelled DETERMINISTIC. It must not be presented as model-Agent performance.

## 7. Run metadata

`EvaluationRunMetadata` records run ID, benchmark/dataset version and split, git commit when available, environment, execution level, interpreter/planner/model modes, tool provider, database/queue modes, seeds, and timestamps. Missing values are null/unavailable rather than fabricated.

## 8. Gold leakage protection

`BenchmarkRuntimeRequest` has no fields for:

```text
expectedOutcome
requiredCapabilities
acceptableTools
forbiddenTools
sideEffectExpectation
approvalExpectation
goldVersion
```

`GoldLeakageProtectionTest` additionally serializes the request and verifies unique Gold sentinel values do not cross the runtime boundary.

## 9. Replay/filter entry

`ShopOpsBenchmarkRunner` supports caseId, benchmarkType, scenario and tag filtering. `scripts/run-shopops-benchmark.ps1` exposes TASK execution and explicitly returns `NOT_IMPLEMENTED` for idempotency/recovery/governance in Phase 1 rather than creating empty success reports.

## 10. Still unobservable

Phase 1 does not yet provide a generic authoritative external-side-effect ledger, RabbitMQ redelivery evidence collector, external-vs-local reconciliation judge, or deterministic judge for every free-form business outcome. These are intentional inputs to later phases, not hidden zeroes.
