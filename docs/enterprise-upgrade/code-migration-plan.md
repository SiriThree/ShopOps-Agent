# ShopOps Enterprise Upgrade Code Migration Plan

Reference repository: `C:\Users\ZHUANZ~1\AppData\Local\Temp\shopops-phase7-ref-da5cc46d4aac42db8f7375366222654b`

Target repository: `D:\找实习\ShopOps`

Allowed strategies: `KEEP_REAL`, `COPY_REFERENCE`, `MERGE`, `REIMPLEMENT_FOR_REAL`, `SKIP_NOT_APPLICABLE`, `DEFER`

## Baseline

| Item | Result |
| --- | --- |
| Git branch | `main` |
| Last commit before migration | `a15a679 fix: make audit timeline mobile friendly` |
| Initial working tree | Existing untracked `data/`, `shopops-admin-ui/`, and static admin assets were present and were not reverted |
| Reference extraction | ZIP expanded outside the real repository under `%TEMP%` |

## Migration Log

| Capability | Reference files | Target files | Current real implementation | Strategy | Conflicts | Connected to main flow | Test result |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Security context, multi-tenant and permissions | `shopops-admin/src/main/java/.../auth/**`, `.../organization/**`, auth/organization tests | `shopops-admin/src/main/java/com/sirithree/shopops/admin/auth/**`, `.../organization/**`, `common/context/**` | Real repo already had token login, request context, and audit basics | `MERGE` | Header-dev and bearer token paths had different permission sources; merged role-derived permissions and kept production validator strict | Yes | `mvn -pl shopops-admin test` PASS; `mvn test` PASS |
| Database migrations | `shopops-admin/src/main/resources/db/migration/V8__...` through `V21__...` | Same Flyway directory | Real repo had `V1`-`V7` only | `COPY_REFERENCE` | `V1` differed and was not overwritten; no `V8`-`V21` version conflicts | Yes | Compile/package PASS; disposable MySQL 8.4 Flyway migration PASS to v21 |
| Approval, idempotency, state machine and Outbox | `approval/**`, `reliability/**`, `TaskStatusTransitionValidator`, `V8`, `V9`, `V20` | Same backend packages and migrations | Real repo had Agent status validation but no full approval/write-operation governance | `MERGE` | Reference used `WITHDRAWN`/new states while legacy API expected `SUCCESS`/`DEGRADED`; reimplemented compatibility mapping instead of weakening tests | Yes | Approval/tool/idempotency integration tests PASS via module and root Maven suites |
| RabbitMQ task lease, error classification and cancellation | `agent/reliability/**`, Rabbit dispatch tests, JDBC worker changes, `V21` | `agent/**`, `persistence/mapper/AgentTaskMapper.java`, `persistence/model/AgentTask.java` | Real repo had Rabbit dispatch and JDBC worker basics | `MERGE` | Runtime stores legacy public statuses while accepting new state-machine names; worker/task services now normalize both | Yes | `AgentTaskRabbitDispatchIntegrationTest` included in `mvn -pl shopops-admin test` PASS |
| Connector pagination, cursor, dedupe and checkpoint | `connector/**`, connector mappers/models, `V12`-`V16`, `V21` | Same backend packages and migrations | No connector governance package in real repo | `COPY_REFERENCE` plus targeted `REIMPLEMENT_FOR_REAL` fixes | Status-check failures were recorded as page fetch failures; fixed log classification to `CHECK`/`NOT_CONFIGURED` | Yes | `ConnectorSyncJobIntegrationTest` PASS; full real connector credentials not verified |
| Actuator, Metrics and health checks | `observability/**`, actuator/prometheus deps, app config | `observability/**`, `pom.xml`, `application*.yml`, deploy observability config | Real repo had a simple system health surface | `MERGE` | Kept existing health flow and added actuator/prometheus wiring | Yes | Maven package PASS; compose config PASS for deploy files |
| Frontend permissions, shop context and operations navigation | `shopops-admin-ui/**`, generated `static/admin/**` | `shopops-admin-ui/**`, `shopops-admin/src/main/resources/static/admin/**` | Real repo had static pages and an untracked UI folder | `COPY_REFERENCE` | Vite build regenerates static admin assets; existing `.git` and repo history untouched | Yes | `npm ci` PASS, `npm run typecheck` PASS, `npm run build` PASS |
| Agent execution mode, workflow templates, Plan Validator and limited Repair | `agent/domain/**`, `agent/governance/**`, `agent/service/impl/**`, Agent tests | Same backend packages | Real repo had rule planner and sequential executor basics | `MERGE` | Plan validator needed real permission propagation into tool context; repaired without `any`/test deletion | Yes | Agent evaluation/NL/model/task tests PASS in module suite |
| README, architecture, demo and delivery docs | `README.md`, `docs/**`, `scripts/**`, `deploy/**`, `performance/**` | Same paths | Real repo had earlier docs and dev compose | `MERGE` | Copied reference docs including encoded Chinese filenames as-is; did not remove existing repo-specific files | Partially: docs/scripts are delivery support, not runtime flow | Maven/npm/docker config checks PASS where applicable |

## Flyway Version Audit

| Version | Real | Reference | Action |
| --- | --- | --- | --- |
| `V1` | Present and differs | Present | `KEEP_REAL`; not overwritten |
| `V2`-`V7` | Present | Present | `KEEP_REAL`; not overwritten |
| `V8`-`V21` | Missing | Present | `COPY_REFERENCE` |
| `V22+` | Not created | Not present | Reserved for future conflict follow-ups if production schema drift is found |

## Mock, Demo and Unverified Notes

| Area | Status |
| --- | --- |
| `shopops.persistence=memory` write-operation path | Explicit test/demo fallback only; production should use JDBC tables from Flyway |
| File-backed business data services | Demo/data-file backed unless configured against verified production data feeds |
| External connectors | Paging/checkpoint/dedupe logic is wired and tested with integration tests, but real third-party credentials were not available in this run |
| Feishu sync | Webhook integration remains disabled by default in demo compose and was not verified against a live Feishu endpoint |
| npm audit | `npm ci` reported 2 moderate vulnerabilities; no automatic dependency mutation was applied |

## Verification History

| Command | Result | Notes |
| --- | --- | --- |
| `git branch --show-current; git status --short; git log -1 --oneline` | PASS | Baseline captured before migration |
| `mvn -pl shopops-admin "-Dtest=GlobalExceptionHandlerIntegrationTest,McpToolCatalogIntegrationTest,ToolApprovalGatewayIntegrationTest,ApprovalCenterIntegrationTest" test` | PASS | Targeted approval/tool/security smoke |
| `mvn -pl shopops-admin "-Dtest=ModelGatewayIntegrationTest,OpenAiCompatibleModelProviderIntegrationTest,PromptTemplateIntegrationTest,OrganizationAdminIntegrationTest,ConnectorSyncJobIntegrationTest" test` | PASS | Model, prompt, organization and connector surface |
| `mvn -pl shopops-admin "-Dtest=ConnectorSyncJobIntegrationTest" test` | PASS | Connector status/checkpoint regression |
| `mvn -pl shopops-admin "-Dtest=AgentEvaluationIntegrationTest,AgentEvaluationDegradedIntegrationTest,AgentEvaluationModelIntegrationTest" test` | PASS | Agent evaluation compatibility |
| `mvn -pl shopops-admin test` | PASS | 122 tests, 0 failures, 0 errors, 8 skipped |
| `git diff --check` | PASS | Only Git CRLF warnings on Windows |
| `mvn test` | PASS | Reactor build: root, common, admin |
| `mvn package` | PASS | Repackaged Spring Boot admin jar |
| `npm ci` | PASS | 2 moderate npm audit vulnerabilities reported |
| `npm run typecheck` | PASS | TypeScript project build passed |
| `npm run build` | PASS | Vite build passed; large chunk warnings reported |
| `docker compose config` | FAIL | No root default compose file exists |
| `docker compose -f deploy/docker-compose.dev.yml config` | PASS | Dev infra compose parsed |
| `docker compose -f deploy/docker-compose.demo.yml config` | PASS | Demo app/MySQL compose parsed |
| `docker compose -p shopops-migrate -f deploy/docker-compose.demo.yml up -d shopops-mysql` | PASS | Temporary MySQL 8.4 started on host port `13306` |
| `java -jar ... --spring.profiles.active=prod --spring.main.web-application-type=none` with default PATH `java` | FAIL | PATH resolved to Java 8; Maven uses `JAVA_HOME` JDK 17 |
| `JAVA_HOME\bin\java.exe -jar ... --spring.profiles.active=prod --spring.main.web-application-type=none` with demo secret | FAIL | Production validator rejected default/weak token secret as expected |
| `JAVA_HOME\bin\java.exe -jar ... --spring.profiles.active=prod --spring.main.web-application-type=none` with strong temporary secrets | PASS | Flyway validated and applied 21 migrations to empty MySQL schema, now v21 |
| `docker compose -p shopops-migrate -f deploy/docker-compose.demo.yml down -v` | PASS | Temporary MySQL container, network and volume removed |

## Current BLOCKER/HIGH

| Severity | Item | Status |
| --- | --- | --- |
| HIGH | Root-level `docker compose config` cannot run without a default `docker-compose.yml` | Existing repository layout issue; deploy compose files pass |
| HIGH | Full external MySQL Flyway migration | Completed against disposable MySQL 8.4; warnings remain for Flyway support matrix and MySQL `VALUES()` deprecation |
| HIGH | External connector/Feishu/model integrations are not live-credential verified | Marked as unverified/demo where applicable |
| MEDIUM | PATH `java` points to Java 8 while Maven uses `JAVA_HOME` JDK 17 | Use `JAVA_HOME\bin\java.exe` or fix PATH for direct jar execution |
| MEDIUM | Frontend bundle has large chunk warnings | Build passes; code splitting can be future optimization |
| MEDIUM | `npm ci` reports 2 moderate vulnerabilities | Not auto-fixed to avoid dependency churn during migration |
