# Phase 0 Handoff

## Completed artifacts

Phase 0 adds benchmark-only contracts and documentation without changing production Agent behavior.

Code under `shopops-admin/src/test/java/com/sirithree/shopops/admin/benchmark/v1/`:

- `BenchmarkType`
- `BenchmarkCase`
- `EvaluationRecord`
- `DatasetVersionContract`
- `BenchmarkMetrics`
- `BenchmarkCaseValidator`
- `BenchmarkCaseLoader`
- contract/duplicate-ID/metric unit tests

Resources under `shopops-admin/src/test/resources/benchmark/v1/`:

- version contract
- isolated `dev`, `validation`, `test` directories
- six minimal contract/sample cases spanning all four benchmark types

No Phase-0 sample result should be cited as a ShopOpsBench score.

## Old evaluation disposition

1. Keep the 14 existing integration cases unchanged as regression tests.
2. Keep the 280-run artifacts as historical repeated-path evidence.
3. Migrate useful scenarios to outcome/capability Gold in later phases.
4. Deprecate `280/280` as a claim of end-to-end Agent task success under the new definition.

## Real entry points for Phase 1

For end-to-end task evidence:

- `AgentTaskController.createTaskFromNaturalLanguage`
- `AgentTaskService.createTask`
- `DefaultAgentEngineService.executeTask`
- `SequentialAgentExecutorService.execute`
- `DefaultToolGatewayService.invoke`
- `OperationReportService`
- `AgentTaskService.listSteps/listEvents`

For idempotency/side-effect evidence:

- `WriteOperationService.prepare`
- `WriteOperationService.externalSucceeded/externalUnknown/failed`
- `WriteOperationMapper`
- `HighRiskRefundExecuteExecutor`
- `RefundExternalClient`

For recovery evidence:

- `JdbcAgentTaskExecutionWorker.execute`
- `AgentTaskService.requeueStaleTasks`
- `WriteOperationReconciliationService.reconcile`
- `OutboxPublisher.publishPending`

For governance evidence:

- `DefaultToolGatewayService.invoke`
- `JdbcApprovalRequestService`
- `AuthorizationService`
- request identity/role filters and tenant/shop-scoped mappers

## Phase 1 recommended boundary

Implement a runner/observer that executes the Phase-0 case contract through the real HTTP/service entry points and creates `EvaluationRecord` from persisted task events, tool logs, approval records, write operations and authoritative side-effect probes. Do not alter planner/tool behavior to satisfy cases. First target Task Benchmark evidence collection and outcome predicates, then add write-side authoritative side-effect probes before reporting idempotency metrics.

## Verification performed in this environment

### PASS

- Benchmark resource structural validation via Python: 6 cases loaded across dev/validation/test; required fields present; no duplicate `caseId`; all JSON/schema files parse.
- Pure Java Phase-0 contracts compiled with local `javac 21`: `BenchmarkType`, `BenchmarkCase`, `EvaluationRecord`, `DatasetVersionContract`, `BenchmarkMetrics`, `BenchmarkCaseValidator`.
- Existing repository `scripts/phase8-static-validate.py`: `TOTAL=21 PASS=21 FAIL=0`.

### NOT RUN

The environment does not provide Maven (`mvn: command not found`) and the repository contains no Maven Wrapper. Therefore the following Java tests were **NOT RUN**, not passed:

- existing Agent Evaluation suites;
- Agent Task integration/unit tests;
- Tool Gateway tests;
- Approval tests;
- WriteOperation tests;
- MCP integration/tests;
- new `BenchmarkContractTest` and `BenchmarkMetricsTest` under Surefire.

Docker is also unavailable (`docker: command not found`), so MySQL/RabbitMQ/Testcontainers/external MCP scenarios were not executed. No model/network credentials were used.
