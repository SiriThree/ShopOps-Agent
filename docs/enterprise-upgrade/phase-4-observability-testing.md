# Phase 4 - Observability, Testing and Performance Baseline

## 1. Audit conclusion

ShopOps previously had business audit records, persisted Agent/Tool TraceSpan data and basic HTTP MDC fields. It did not expose Actuator/Prometheus metrics, define health probe groups, provide a repeatable performance script, or separate frontend type checking in CI. The existing TraceSpan implementation is application-level business tracing rather than a complete OpenTelemetry distributed trace implementation.

## 2. Actual completed scope

- Added Spring Boot Actuator and Prometheus registry dependencies.
- Exposed health, info, metrics and Prometheus endpoints.
- Enabled liveness/readiness probes and HTTP latency histograms for P50/P95/P99 calculation.
- Added `traceId` to HTTP MDC propagation.
- Added a bounded-label `ShopOpsMetrics` facade for task, tool, connector, tenant rejection, idempotency and lease-expiry metrics.
- Added a frontend `typecheck` script.
- Added a repeatable k6 smoke baseline for Dashboard, order pagination and task list APIs.
- Added Prometheus scrape configuration.
- Added Testcontainers dependencies for future real MySQL/RabbitMQ integration tests.

## 3. Not completed

- OpenTelemetry SDK/exporter and RabbitMQ W3C trace-context propagation are not fully integrated.
- `ShopOpsMetrics` is an available runtime facade but not every business branch has been instrumented in this phase; claiming all listed metrics are currently emitted would be inaccurate.
- Redis cache metrics are only available when Spring Data Redis/Lettuce instrumentation is active and actual Redis operations occur.
- No Grafana dashboard or executable alert manager rules were validated.
- Testcontainers dependencies were added, but full infrastructure integration suites were not authored or executed in this environment.
- No performance numbers are reported because the application and dependencies were not started for k6 execution.

## 4. Modified files

- `shopops-admin/pom.xml`
- `shopops-admin/src/main/resources/application.yml`
- `shopops-admin/src/main/java/com/sirithree/shopops/admin/common/context/RequestContextInterceptor.java`
- `shopops-admin-ui/package.json`

## 5. Added files

- `shopops-admin/src/main/java/com/sirithree/shopops/admin/observability/ShopOpsMetrics.java`
- `performance/k6-smoke.js`
- `deploy/observability/prometheus.yml`
- `docs/enterprise-upgrade/phase-4-observability-testing.md`

## 6. Deleted files

None.

## 7. Core design

Metrics labels deliberately avoid requestId, userId, tenantId and shopId. Tool code is retained as a bounded registry-defined dimension. Business audit, application logs, application TraceSpan and Micrometer metrics remain separate concerns.

## 8. Database changes

None.

## 9. Configuration changes

Actuator endpoints `health`, `info`, `prometheus` and `metrics` are exposed. Health details remain restricted to authorized requests. HTTP server request histograms and 0.5/0.95/0.99 percentiles are enabled.

## 10. Executed verification

See final delivery report. No unexecuted test is marked as passed.

## 11. Known risks

The largest remaining gap is end-to-end trace propagation across RabbitMQ and asynchronous executors. Metric instrumentation coverage also remains incomplete until the facade is wired into each critical state transition.

## 12. Recommended next dependency

Before performance claims, run the application with MySQL, Redis and RabbitMQ, execute integration tests and k6, and record environment, data size, concurrency, duration, throughput, percentiles, error rate and resource use.

## 13. Verification results (2026-08-05)

| Command | Result |
|---|---|
| `mvn test` | Not executed: Maven is not installed (`exit 127`). |
| `npm --prefix shopops-admin-ui run typecheck` | Failed: `src/users.tsx` contains multiple implicit `any` parameters. |
| `npm --prefix shopops-admin-ui run build` | Failed at the same TypeScript errors before Vite bundling. |
| `docker compose -p shopops-demo -f deploy/docker-compose.demo.yml config --quiet` | Not executed: Docker is not installed (`exit 127`). |
| `node --check performance/k6-smoke.js` | Passed syntax validation. This is not a k6 load-test result. |

## 14. Phase handoff

Phase 4 stops here. Phase 5 must not begin automatically. The next executor should first obtain a Maven/Docker-capable environment, resolve the existing frontend TypeScript baseline errors, add real Testcontainers suites, then run the repeatable performance baseline before making any latency, throughput or reliability claim.
