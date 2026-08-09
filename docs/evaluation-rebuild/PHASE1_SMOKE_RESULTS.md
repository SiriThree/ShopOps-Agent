# Phase 1 Smoke Results

## Status

**IMPLEMENTED, NOT RUNTIME VERIFIED in the current execution environment.**

The Phase 1 smoke dataset contains three TASK cases. Their contracts, runner path, evidence collector and evaluators are implemented, but the actual Spring/JUnit executions were **NOT RUN** because Maven is not installed in the current environment. Docker is also unavailable.

| caseId | intended runtime mode | execution result in this environment | reason |
|---|---|---|---|
| `smoke-task-daily-review-001` | DETERMINISTIC / HTTP / memory / rule-based | **NOT RUN** | `mvn` unavailable |
| `smoke-task-comment-risk-001` | DETERMINISTIC / HTTP / memory / rule-based / MCP test adapter | **NOT RUN** | `mvn` unavailable |
| `smoke-task-degraded-ad-001` | DETERMINISTIC / HTTP / memory / injected `ad.query_performance` failure | **NOT RUN** | `mvn` unavailable |

No Task Success percentage is reported from these cases.

## What was actually verified

The following checks did execute successfully:

```text
python3 scripts/phase8-static-validate.py
TOTAL=21 PASS=21 FAIL=0
```

Phase 1 resource validation:

```text
JSON_FILES=7
PARSE_ERRORS=0
BENCHMARK_CASES=9
DUPLICATE_CASE_IDS=[]
STATIC_BENCHMARK_RESOURCE_VALIDATION=PASS
```

Pure Java Benchmark contract compilation:

```text
JAVAC_PHASE1_PURE_CONTRACTS PASS
```

This javac check covers classes that do not require Spring/Jackson/JUnit dependencies. It does **not** replace Maven compilation or integration tests.

## Environment facts

```text
mvn: NOT FOUND
docker: NOT FOUND
pwsh: NOT FOUND
java: OpenJDK 21.0.11
```

There are 17 declared `@Test` methods under the ShopOpsBench v1 test package after Phase 1 (7 Phase 0 contract/metric tests + 10 Phase 1 runtime/evidence/report/replay tests). None of those JUnit methods were executed here.

Therefore the following remain **NOT RUN**:

- `BenchmarkRunnerLifecycleTest`;
- `BenchmarkDegradedSmokeIntegrationTest`;
- `BenchmarkEvidenceCollectorTest`;
- `BenchmarkEvaluatorTest`;
- `GoldLeakageProtectionTest`;
- `SingleCaseReplayTest`;
- `BenchmarkReportSerializationTest`;
- `EvaluationRunMetadataTest`;
- `FailureReasonMappingTest`;
- updated `SequentialAgentExecutorServiceTest` authorization regression;
- original Agent Evaluation regression tests;
- Agent Task / Tool Gateway / Approval / WriteOperation / MCP JUnit suites;
- JDBC/MySQL/RabbitMQ/real MCP/model executions.

## Commands to run in a complete environment

```powershell
mvn -pl shopops-admin -am test

powershell -ExecutionPolicy Bypass -File scripts/run-shopops-benchmark.ps1 `
  -BenchmarkType task -Split smoke -CaseId smoke-task-daily-review-001

powershell -ExecutionPolicy Bypass -File scripts/run-shopops-benchmark.ps1 `
  -BenchmarkType task -Split smoke -CaseId smoke-task-comment-risk-001
```

For the injected degraded path:

```text
mvn -pl shopops-admin -am -Dtest=BenchmarkDegradedSmokeIntegrationTest test
```

A run is not considered RUNTIME VERIFIED until these commands complete in an environment with the required dependencies.


> Environment note: Maven installation was also attempted with `apt-get`, but the command timed out and Maven was still absent afterward. No test result is inferred from that failed installation attempt.
