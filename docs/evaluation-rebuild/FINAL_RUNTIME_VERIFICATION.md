# ShopOpsBench v1 — Final Runtime Verification

## 1. Verification levels

ShopOpsBench deliberately separates the following facts:

```text
IMPLEMENTED
!= SOURCE_COMPILED
!= JUNIT_PASS
!= SPRING_RUNTIME_VERIFIED
!= JDBC_INTEGRATION_VERIFIED
!= FORMAL_BENCHMARK_VERIFIED
```

A formal metric may only be emitted when the benchmark-specific eligibility gates are satisfied.

## 2. Real build contract

Repository audit confirms:

- root Maven multi-module build: `pom.xml`;
- production Java target: **17** (`java.version=17`, compiler source/target 17);
- GitHub Actions backend job: Temurin 17 + `mvn -pl shopops-admin -am test`;
- production database path: MySQL/JDBC;
- RabbitMQ exists for Agent task dispatch / Outbox paths, not as a refund-write consumer;
- repository currently does **not** contain `mvnw`, `mvnw.cmd`, or `.mvn/`.

The current coding sandbox has OpenJDK 21 available, but Phase 6 Java contract checks use `javac --release 17`; the project production target was not changed.

## 3. Commands actually attempted in this environment

```text
java -version
./mvnw -version
mvn -version
docker version
getent hosts deb.debian.org
getent hosts repo.maven.apache.org
python3 scripts/phase8-static-validate.py
python3 scripts/generate-benchmark-manifest.py
python3 scripts/verify-benchmark-manifest.py
```

Observed environment:

| Capability | Actual result |
|---|---|
| Java runtime | OpenJDK 21.0.11 present; project target remains Java 17 |
| Maven | `NOT_FOUND` |
| Maven Wrapper | `ABSENT` |
| Docker | `NOT_FOUND` |
| PowerShell (`pwsh`) | `NOT_FOUND` |
| external DNS for Maven/apt acquisition | unavailable in this sandbox |
| Spring/JUnit | `NOT RUN` |
| MySQL/JDBC integration | `NOT RUN` |
| RabbitMQ integration | `NOT RUN` |

The standard Maven Wrapper was not fabricated manually. Because the sandbox could not reach external package/artifact hosts, wrapper setup remains:

```text
MAVEN_WRAPPER_SETUP_NOT_VERIFIED
```

## 4. Phase 6 verification matrix

| Capability | Implemented | Java-17 source/contract check | JUnit | Spring | JDBC/MySQL | Queue | Formal |
|---|---|---|---|---|---|---|---|
| Benchmark dataset/manifest | YES | PASS | NOT RUN | N/A | N/A | N/A | manifest frozen only |
| Formal eligibility | YES | PASS | NOT RUN | N/A | N/A | N/A | enforcement logic PASS |
| Release gate evaluator | YES | PASS | NOT RUN | N/A | N/A | N/A | no formal metrics available |
| Task Benchmark | YES | prior PURE/evaluator evidence exists | NOT RUN | NOT RUN | NOT RUN | N/A | **NOT VERIFIED** |
| Idempotency Benchmark | YES | prior PURE production-write evidence exists | NOT RUN | NOT RUN | NOT RUN | N/A for refund | **NOT VERIFIED** |
| Recovery Benchmark | YES | prior PURE/MEMORY production-recovery evidence exists | NOT RUN | NOT RUN | NOT RUN | N/A for refund | **NOT VERIFIED** |
| Governance Benchmark | YES | prior PURE Tool-Gateway evidence exists | NOT RUN | NOT RUN | NOT RUN | N/A | **NOT VERIFIED** |
| Refund order ownership scope | YES | implementation audited | NOT RUN | NOT RUN | NOT RUN | N/A | **NOT VERIFIED** |
| Rabbit Agent task / Outbox paths | existing production capability | not re-run here | NOT RUN | NOT RUN | NOT RUN | NOT RUN | N/A to refund benchmark |

## 5. Formal benchmark tests prepared

The repository now contains held-out, explicitly gated integration entry points:

- `FormalTaskBenchmarkIntegrationTest`
- `FormalIdempotencyBenchmarkIntegrationTest`
- `FormalRecoveryBenchmarkIntegrationTest`
- `FormalGovernanceBenchmarkIntegrationTest`
- `JdbcRefundIdempotencyIntegrationTest`
- `JdbcRefundRecoveryIntegrationTest`
- `JdbcGovernanceIntegrationTest`

They require explicit properties and are not treated as passing until Maven/Spring/MySQL actually execute them.

## 6. Final formal status

```text
Task End-to-End Agent Task Success    NOT AVAILABLE
Duplicate Side Effects                NOT AVAILABLE
State Convergence Rate                NOT AVAILABLE
Unauthorized Block Rate               NOT AVAILABLE
False Reject Rate                     NOT AVAILABLE
```

This is intentional. Static checks and PURE harnesses are evidence for implementation behavior, not substitutes for the missing formal runtime levels.
