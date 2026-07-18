# ShopOps Agent

ShopOps Agent is an enterprise-oriented AI operations platform for ecommerce teams. It turns daily shop operation workflows into governed Agent tasks: data retrieval, analysis, report generation, risk review, approval, and full-chain audit.

The project is designed around multi-tenant ecommerce operations, MCP-style tool governance, controlled Agent orchestration, approval workflows, model gateway abstraction, and traceable execution.

## Positioning

ShopOps Agent is not a simple LLM chatbot. It is an AgentOps and CommerceOps platform for ecommerce operation scenarios.

Core capabilities:

- Multi-tenant and multi-shop isolation.
- Ecommerce connectors for orders, products, comments, ads, reports, and collaboration tools.
- MCP-style tool registry with schema, permission, risk level, version, timeout, and audit metadata.
- Tool Gateway as the only controlled execution boundary for Agent tool calls.
- Planner / Executor / Verifier Agent execution chain.
- Human approval for high-risk operations.
- Full-chain trace across task, step, tool call, model call, approval, and report.
- Degradation strategy for tool failure and model unavailability.

## Reference Architecture

The backend design references mature open-source ecommerce systems such as `mall`, `mall-swarm`, `mall4cloud`, and headless commerce platforms. `mall-master` is used locally as a Java backend and ecommerce domain reference, especially for Spring Boot layering, security, MyBatis, orders, products, comments, and RabbitMQ examples.

ShopOps adds its own enterprise Agent platform layer on top of those concepts:

```text
React / Ant Design Pro Console
        |
        v
Spring Boot Platform Backend
  ├── Auth & Tenant
  ├── Shop Center
  ├── Connector Center
  ├── MCP Tool Center
  ├── Tool Gateway
  ├── Agent Engine
  ├── Workflow / Approval
  ├── Report Center
  ├── Model Gateway
  └── Observability / Audit
        |
        v
MySQL / Redis / RabbitMQ / Elasticsearch / MinIO
        |
        v
Ollama / OpenAI-compatible Models / Ecommerce Platforms
```

## Documents

- [Enterprise Platform Design](docs/ShopOps%20Agent企业级平台实现方案.md)
- [Enterprise Database ER Design](docs/ShopOps%20Agent企业级数据库ER设计.md)
- [Backend API and Service Design](docs/ShopOps%20Agent后端API与服务设计.md)
- [Daily Review P0 Main Flow Design](docs/ShopOps%20每日经营复盘P0主链路实现设计.md)
- [Local Development Guide](docs/本地开发启动指南.md)
- [Original Platform-Level Design Report](ShopOps%20Agent平台级详细设计报告.md)

## SQL

P0 schema and seed data:

- [P0 Schema](sql/shopops_p0_schema.sql)
- [P0 Seed Data](sql/shopops_p0_seed.sql)

P0 focuses on the daily operation review flow:

```text
User task
  -> Agent task
  -> Rule Planner
  -> Tool Gateway
  -> order.query_summary
  -> comment.query_negative
  -> product.query_candidates
  -> report.generate_daily_review
  -> operation_report
  -> trace_span
```

## P0 Tool Set

```text
order.query_summary
comment.query_negative
product.query_candidates
report.generate_daily_review
```

## Planned Backend Stack

- Java 17
- Spring Boot 3
- Spring Security + JWT
- MyBatis / MyBatis Generator
- MySQL
- Redis
- RabbitMQ
- SpringDoc / OpenAPI
- Docker Compose
- Ollama and OpenAI-compatible model providers

## Backend P0 Skeleton

The repository now contains a Spring Boot P0 backend skeleton:

```text
shopops-common
  └── common API response and paging wrappers

shopops-admin
  ├── Agent task API
  ├── Tool registry API
  ├── Tool Gateway
  ├── P0 ToolExecutor implementations
  ├── Report API
  └── Trace API
```

P0 currently uses in-memory services so the main flow can be validated before MyBatis persistence is wired in.
MyBatis dependencies, datasource configuration, P0 persistence models, and mapper interfaces have been added as the next persistence layer foundation.

Start development infrastructure:

```bash
docker compose -f deploy/docker-compose.dev.yml up -d
```

Persistence mode:

```yaml
shopops:
  persistence: memory # memory or jdbc
```

To use MyBatis persistence:

1. Create database `shopops_agent`.
2. Execute [P0 Schema](sql/shopops_p0_schema.sql).
3. Execute [P0 Seed Data](sql/shopops_p0_seed.sql).
4. Update datasource credentials in `shopops-admin/src/main/resources/application.yml`.
5. Set `shopops.persistence=jdbc`.

The JDBC mode currently persists:

```text
mcp_tool
agent_task
agent_task_step
agent_task_event
tool_call_log
operation_report
trace_span
```

Trace instrumentation currently covers:

```text
agent.task
agent.planner
agent.executor
agent.verifier
tool.<toolCode>
```

Run locally with Java 17 and Maven:

```bash
mvn clean install -DskipTests
mvn -pl shopops-admin spring-boot:run
```

Create a daily review task:

```bash
curl -X POST http://localhost:8080/api/agent/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "taskType": "daily_review",
    "userInput": "帮我生成今天店铺运营复盘",
    "dateRange": {
      "start": "2026-07-18",
      "end": "2026-07-18"
    }
  }'
```

Useful P0 APIs:

```text
POST /api/agent/tasks
GET  /api/agent/tasks/{taskId}
GET  /api/agent/tasks/{taskId}/steps
GET  /api/reports/{reportId}
GET  /api/tasks/{taskId}/trace
GET  /api/tools
POST /api/tools/{toolCode}/invoke
```

## Repository Status

Current stage: enterprise design, P0 database preparation, and Java backend P0 skeleton.

Next engineering step:

```text
Wire the P0 skeleton to MyBatis persistence:
SQL-backed AgentTaskService / McpToolService / ToolCallLogService / OperationReportService
```
