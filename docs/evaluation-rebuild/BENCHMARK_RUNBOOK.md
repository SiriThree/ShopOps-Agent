# ShopOpsBench v1 — Runbook

This is the single execution guide for Task, Idempotency, Recovery and Governance benchmarks.

## 1. Required local tools

Repository build contract:

```text
JDK target: 17
Build: Maven multi-module
Database: MySQL
Queue: RabbitMQ only for real Agent-task / Outbox paths
```

For the full formal run you need:

- JDK 17;
- Maven (or a verified standard Maven Wrapper once added);
- MySQL 8-compatible runtime;
- Docker/Compose is the easiest way to provide local integration dependencies;
- RabbitMQ only for queue-path regression, not for the refund write benchmark.

### Maven Wrapper status

This repository currently has no checked-in `mvnw/.mvn`. Phase 6 deliberately did not fabricate wrapper files while the coding sandbox had no external DNS. When Maven/network access is available, generate the standard wrapper using the official Maven Wrapper plugin, then commit the wrapper scripts/config and verify both Unix and Windows entry points.

Until that is done:

```text
MAVEN_WRAPPER_SETUP_NOT_VERIFIED
```

## 2. Verify the frozen dataset before any held-out run

```bash
python3 scripts/verify-benchmark-manifest.py
```

Expected:

```text
BENCHMARK_MANIFEST_VERIFY PASS
formalHeldOutCaseCount 31
```

Do not regenerate the manifest after seeing formal failures unless a real Gold correction is versioned and documented.

## 3. Fast regression

```bash
mvn -pl shopops-admin -am test
```

This includes ordinary unit/Spring tests; explicit formal/JDBC tests remain property-gated.

## 4. Smoke / development

Unix:

```bash
scripts/run-shopops-benchmark.sh --type task --split smoke
scripts/run-shopops-benchmark.sh --type idempotency --split dev
scripts/run-shopops-benchmark.sh --type recovery --split dev
scripts/run-shopops-benchmark.sh --type governance --split dev
```

Windows PowerShell:

```powershell
scripts/run-shopops-benchmark.ps1 -BenchmarkType task -Split smoke
scripts/run-shopops-benchmark.ps1 -BenchmarkType governance -Split dev
```

## 5. Validation split

```bash
scripts/run-shopops-benchmark.sh --type task --split validation
scripts/run-shopops-benchmark.sh --type idempotency --split validation
scripts/run-shopops-benchmark.sh --type recovery --split validation
scripts/run-shopops-benchmark.sh --type governance --split validation
```

## 6. Held-out protection

A normal command using `test` is blocked:

```text
HELD_OUT_BLOCKED
```

Formal held-out execution requires the explicit formal flag.

Unix:

```bash
scripts/run-shopops-benchmark.sh --type task --formal
scripts/run-shopops-benchmark.sh --type idempotency --formal
scripts/run-shopops-benchmark.sh --type recovery --formal
scripts/run-shopops-benchmark.sh --type governance --formal
```

PowerShell:

```powershell
scripts/run-shopops-benchmark.ps1 -BenchmarkType task -Formal
scripts/run-shopops-benchmark.ps1 -BenchmarkType idempotency -Formal
scripts/run-shopops-benchmark.ps1 -BenchmarkType recovery -Formal
scripts/run-shopops-benchmark.ps1 -BenchmarkType governance -Formal
```

`all` means **run four independent benchmarks**, never calculate an average score:

```bash
scripts/run-shopops-benchmark.sh --type all --formal
```

## 7. Current formal integration prerequisites

Formal JDBC tests are configured against:

```text
jdbc:mysql://localhost:3306/shopops_agent
user=root
password=root
```

Use a disposable integration database. The GitHub Actions manual integration job defines an equivalent MySQL service and uses Java 17.

Formal governance uses real JDBC authorization data (`admin=1`, `operator=2`, `viewer=3`) rather than the admin-like memory authorization fixture.

Formal refund tests use an independent in-process `NON_IDEMPOTENT_EXTERNAL` recording system. This proves ShopOps behavior at an external contract boundary; it is not a claim that a commercial refund API was called.

## 8. Replaying one case

Development replay:

```bash
scripts/run-shopops-benchmark.sh --type task --split dev --case <case-id>
```

Held-out replay must remain explicit/formal and should be used for failure investigation after the run is frozen, not for iterative tuning.

## 9. Result locations

Formal integration tests write to:

```text
shopops-admin/target/benchmark/formal/task/
shopops-admin/target/benchmark/formal/idempotency/
shopops-admin/target/benchmark/formal/recovery/
shopops-admin/target/benchmark/formal/governance/
```

Each run produces JSON and Markdown with raw counts and failure reasons.

## 10. Release gate

Before evaluating a release gate, confirm each benchmark is formally eligible. Then apply:

```text
shopops-admin/src/test/resources/benchmark/v1/benchmark-gates-v1.json
```

The current gate is baseline-establishment mode: hard safety invariants are frozen at zero, while quality thresholds are intentionally unset until the first eligible formal baseline exists.

## 11. CI

Normal PR CI performs manifest validation plus Maven regression.

The held-out integration job is manual (`workflow_dispatch` + `run_formal_benchmark=true`) and provides MySQL/RabbitMQ services. This avoids repeatedly tuning against the held-out set on every commit.
