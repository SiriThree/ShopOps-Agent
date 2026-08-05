# ShopOps 企业级改造阶段 0：完整仓库审计与基线

- 审计日期：2026-08-05
- 审计对象：`ShopOps-main(1).zip`
- 仓库版本标识：压缩包未包含可用 Git 提交信息，因此无法记录 commit SHA
- 执行约束：仓库中未找到 `00_全局执行约束.md`；本次严格采用用户在任务中提供的同名约束内容作为审计准则
- 本阶段代码修改：无
- 本阶段新增文件：`docs/enterprise-upgrade/phase-0-audit.md`

## 1. 阶段审计结论

ShopOps 当前已经不是单纯聊天界面的 Agent Demo。实际代码呈现为一个 Spring Boot 模块化单体后端、一个 React 管理端和一组运营业务、组织权限、Agent、工具、审批、审计、连接器、报表及评测模块。Agent 工作台是其中一个入口，但 README 和根 Maven artifact 仍以 `shopops-agent`/AgentOps 为主要叙事，业务平台主体定位尚未完全落实。

当前系统具备较完整的演示闭环：登录或开发请求头建立上下文，创建 Agent 任务，规则规划，计划校验，顺序执行工具，必要时创建审批，生成报告，Verifier 检查证据并最多执行一次定向修复，任务及 Trace 数据写入 MySQL。RabbitMQ 生产和消费代码、队列与 DLQ 声明真实存在，但默认运行模式为同步派发，且数据库事务与消息发布之间没有 Outbox，因此不能宣称异步链路已经达到可靠生产语义。

最严重的问题不是“功能缺失”，而是安全边界和可靠性语义不足：默认开启请求头开发认证，前端自动发送可控 tenant/shop/user/roles；高风险工具审批可被店铺配置关闭；RabbitMQ 发布不在事务一致性边界内；外部写工具缺少统一幂等键和状态未知处理；Worker 崩溃恢复依赖人工调用 stale requeue；默认密钥和数据库密码可直接启动。

## 2. 仓库概览

### 2.1 根目录

| 路径 | 实际用途 |
|---|---|
| `pom.xml` | Maven 父工程，包含 `shopops-common`、`shopops-admin` |
| `shopops-common/` | 通用 API 返回与分页等基础模型 |
| `shopops-admin/` | Spring Boot 后端主体，包含业务、Agent、权限、审批、审计等所有服务 |
| `shopops-admin-ui/` | React + TypeScript + Vite 管理端 |
| `deploy/` | 后端 Dockerfile、开发/演示 Compose |
| `sql/` | 早期 P0/P1 SQL 副本，不是当前完整迁移源 |
| `shopops-admin/src/main/resources/db/migration/` | 当前实际 Flyway V1—V19 迁移 |
| `.github/workflows/ci.yml` | 后端测试、前端构建、Compose 校验与 Docker 构建 |
| `scripts/` | PowerShell 演示、评测、数据准备和证据生成脚本 |
| `docs/` | 架构、演示、评测和简历证据文档 |

仓库共约 594 个文件，其中后端主代码 313 个 Java 文件、测试代码 57 个 Java 文件。

### 2.2 Maven 模块

1. `shopops-common`
   - `spring-boot-starter-web`
   - `spring-boot-starter-validation`
   - 当前主要承载通用响应和分页对象，不是独立业务服务。

2. `shopops-admin`
   - 依赖 `shopops-common`
   - Spring Boot Web、Validation、MyBatis、MySQL、Redis、RabbitMQ、Flyway、Springdoc、Spring Boot Test
   - 唯一 Spring Boot 启动入口：`ShopOpsAdminApplication`
   - `@MapperScan("com.sirithree.shopops.admin.persistence.mapper")`

结论：当前是双 Maven 模块的模块化单体，不是微服务系统。

## 3. 真实技术栈

### 后端

- Java 17 编译目标
- Spring Boot 3.3.7
- Spring MVC
- Jakarta Validation
- MyBatis Spring Boot Starter 3.0.3，Mapper 采用注解 SQL
- MySQL Connector/J 8.4.0
- Flyway Core + Flyway MySQL
- Spring Data Redis 依赖已引入，但业务代码中未发现形成缓存、锁或会话主链路的 Redis 实现
- Spring AMQP / RabbitMQ
- 自定义 HMAC-SHA256 Token，不是 Spring Security/JWT 库
- AES-GCM 连接器凭据加密
- 自定义 Trace、工具调用日志、认证审计、连接器审计
- OpenAI-compatible HTTP Model Gateway，可关闭并回退到 echo provider

### 前端

- React 19
- TypeScript 5.7
- Vite 6
- Axios
- Ant Design
- ECharts
- 多 HTML 入口管理端，不是单一 SPA 路由结构

### 基础设施

- MySQL 8.4
- Redis 7.4（Compose 提供，但当前核心链路未实际使用）
- RabbitMQ 3.13 Management
- Docker 多阶段构建
- GitHub Actions

## 4. 代码模块结构

`shopops-admin` 按包划分为：

- `auth`：登录、Token、Token Session、角色拦截器、认证审计
- `organization`：租户、店铺、成员、店铺配置后台管理
- `business`：订单指标、评论分析、商品优化等运营查询服务
- `dashboard`：管理看板聚合
- `agent`：任务、自然语言解释、Planner、Validator、Executor、Verifier、Repair、同步/异步派发、恢复
- `tool`：MCP 风格工具目录、Gateway、Executor、调用日志、风险与审批接入
- `approval`：审批创建、通过、拒绝、撤回、批量、过期
- `report`：运营报告持久化和查询
- `audit`：Trace 与管理审计时间线
- `connector`：状态、凭据、同步任务、API 调用日志和文件型连接器
- `model`：模型提供方、Prompt 模板、模型调用日志
- `evaluation`：Agent 自然语言批量评测 API
- `persistence`：MyBatis Mapper 与持久化模型
- `system`：健康状态
- `common`：请求上下文、异常处理、JSON 工具和 Web 配置

## 5. 数据表清单

Flyway V1—V19 实际创建或变更以下表：

| 领域 | 表 |
|---|---|
| 租户组织 | `tenant`, `tenant_member`, `shop`, `shop_member`, `shop_config`, `user_account` |
| 业务 | `product`, `shop_order`, `shop_order_item`, `product_comment` |
| Agent/工具 | `mcp_tool`, `agent_task`, `agent_task_step`, `agent_task_event`, `tool_call_log` |
| 报表与追踪 | `operation_report`, `trace_span` |
| 认证 | `auth_audit_event`, `auth_token_session` |
| 审批 | `approval_request` |
| 模型 | `model_call_log`, `prompt_template` |
| 连接器 | `connector_credential`, `connector_audit_event`, `connector_sync_job`, `connector_api_call_log` |

V2、V3、V4、V9、V17—V19 同时写入演示用户、业务数据和工具目录。迁移体系能表达当前数据结构，但 `spring.flyway.enabled=false`，由自定义 `FlywayMigrationRunner` 根据 `shopops.flyway.enabled` 启动迁移，属于非标准双开关设计，需要后续简化和验证。

## 6. 外部依赖

- MySQL：JDBC 持久化默认必需
- Redis：配置和 Compose 存在，未形成已验证的核心用途
- RabbitMQ：仅 `shopops.agent.dispatch-mode=rabbitmq` 时启用
- 本地 JSON 文件：订单汇总、差评、商品候选、广告表现和外部报表连接器
- 飞书 Webhook：默认关闭；报告同步工具可调用外部 Webhook
- OpenAI-compatible 模型 API：默认关闭；Planner 和 Report 可分别启用模型调用
- Olist 和其他演示数据文件

## 7. HTTP 业务链路

真实通用链路：

```text
React multi-entry admin page
→ Axios apiClient
→ /api/**
→ RequestContextInterceptor
→ RequestContextResolver
→ Bearer Token Session，或默认开启的开发请求头上下文
→ RoleAuthorizationInterceptor（仅检查标注了 @RequireRole 的接口）
→ Controller
→ Service
→ MyBatis Mapper / 文件连接器 / Webhook / Model Provider
→ MySQL / Local File / External Service
```

业务查询服务的 MyBatis SQL普遍包含 `tenant_id` 和 `shop_id` 条件。组织后台存在平台级租户管理接口，其权限边界依赖 `@RequireRole`，不是所有接口都具有细粒度 permission code。

前端 `buildHeaders()` 默认发送：

```text
X-Tenant-Id: 1
X-Shop-Id: 1
X-User-Id: 1
X-User-Roles: ADMIN,OPERATOR
```

当无 Bearer Token 且 `header-dev-mode=true` 时，后端会接受这些客户端字段作为认证上下文。这是明确的 P0 越权风险。

## 8. Agent 执行链路

实际主链路为：

```text
POST /api/agent/tasks 或 /natural-language
→ RuleBasedAgentTaskInterpreter（自然语言入口）
→ AgentTaskService 创建任务、步骤快照和事件
→ AgentTaskDispatcher
   ├─ SynchronousAgentTaskDispatcher（默认）
   └─ RabbitAgentTaskDispatcher（显式配置）
→ JdbcAgentTaskExecutionWorker
→ DefaultAgentEngineService
→ PlannerService
   ├─ RulePlannerService
   └─ 可选 Model Gateway Planner
→ PlanValidator
→ SequentialAgentExecutorService
→ DefaultToolGatewayService
→ McpToolService + ToolExecutor
→ ApprovalRequestService（工具标记需要审批且店铺审批开关开启时）
→ OperationReportService
→ BasicVerifierService
→ 一次定向 Repair Plan（补缺失证据工具 + 重新生成报告）
→ 再次 Verifier
→ Agent Task 状态、步骤、事件、报告、Tool Log、Trace 持久化
```

### 已确认真实接入

- Planner、Plan Validator、Executor、Tool Gateway、Tool Executor、Verifier、Repair、Report、Trace 均在 `DefaultAgentEngineService` 主链路中调用。
- Verifier 失败且可修复时，会根据 `repairToolCodes` 定向补充工具并重新生成报告，只允许一次修复后重新验证。
- Executor 对订单汇总和报告生成失败按任务失败处理，其余证据工具失败会标记 degraded。
- Tool Gateway 会检查工具是否存在、启用、是否需要审批，并写调用日志和 Trace。

### 重要限制

- Planner 主要是规则模板；模型 Planner 默认关闭。
- Verifier 仅检查执行成功、报告 ID 和“要求的工具是否成功返回非空数据”，不验证业务数值、来源可信度、时效性、冲突或外部副作用。
- Repair 只是证据工具补跑，不是通用计划修复器。
- 工具审批结果不会自动恢复原暂停步骤；通常需要携带 approvalId 再次调用或重试。
- Agent 本身仍能通过 Tool Gateway 执行工具，但权限检查主要是工具元数据和审批，不是完整业务 permission policy。

## 9. 异步任务链路

真实链路：

```text
AgentTaskService 持久化 task/steps/events
→ AgentTaskDispatcher
→ RabbitTemplate.convertAndSend（rabbitmq 模式）
→ Direct Exchange
→ Durable Queue + DLX/DLQ 参数
→ @RabbitListener RabbitAgentTaskConsumer
→ JdbcAgentTaskExecutionWorker
→ QUEUED 条件更新为 RUNNING
→ Agent Engine 执行
→ SUCCESS / DEGRADED / FAILED 持久化
```

### 已实现

- Durable exchange/queue
- DLX 和 DLQ 声明
- Consumer 与 Worker
- 任务状态 CAS 风格更新，能避免同一 QUEUED 任务被并行启动两次
- 对终态消息重复投递直接返回
- stale task requeue 管理接口与 recovery service

### 未达到企业可靠性要求

- 默认 `dispatch-mode=sync`，RabbitMQ 不是默认主流程。
- 任务落库与 `convertAndSend` 之间没有本地事务 + Outbox；可能出现任务已提交但消息未发出。
- 未配置 publisher confirm/return 处理。
- Listener 未显式定义 retry/backoff/ack/requeue 策略；异常语义依赖 Spring AMQP 默认行为。
- Worker 捕获运行时异常后把任务标记 FAILED，Consumer 通常不会抛出，因此失败消息不会自然进入 DLQ。
- 没有 DLQ 消费、回放或运维 API。
- Worker 崩溃后 RUNNING 任务依赖 stale requeue 接口人工触发，没有调度器或 lease/heartbeat 自动恢复。

## 10. 外部连接器链路

实际链路分为两类。

### 文件型业务连接器

```text
application.yml 文件路径
→ ConfiguredFilePathResolver
→ JSON 文件读取服务
→ Order/Comment/Product/Ad/External Report 工具执行器
→ Agent 报告证据
```

这些连接器可读取真实文件，但本质仍是本地文件适配器，不是电商平台 API 同步系统。

### 管理连接器

```text
Connector Controller
→ Credential Service
→ AES-GCM encrypted_secret 持久化
→ Connector Status Service
→ Connector Sync Job Service
→ ConnectorSyncJobExecutor
→ 状态检查
→ Sync Job / API Call Log / Connector Audit
```

`ConnectorSyncJobExecutor` 当前只执行连接器可用性检查并记录日志，没有拉取、映射、upsert 业务数据。因此“连接器同步任务”是部分实现，不能宣称已完成外部电商数据同步闭环。

凭据使用 AES-GCM 加密，密钥由一个配置字符串 SHA-256 派生；没有 KMS、密钥版本、轮换或 envelope encryption。默认弱密钥使加密在默认部署中失去生产意义。

## 11. 能力矩阵

状态含义：完整接入、部分实现、仅接口、仅数据结构、仅模拟、仅测试、仅文档、未实现。

| 能力 | 状态 | 实际依据与限制 |
|---|---|---|
| 登录与认证 | 部分实现 | 用户名密码、Token、Session、注销已接入；默认开发请求头可绕过正式认证 |
| Token 生命周期 | 部分实现 | 签发、过期、Session touch、revoke；无刷新 Token、密钥轮换、设备/session 管理 |
| 用户与组织 | 部分实现 | 租户、店铺、成员、配置 CRUD；平台边界与权限粒度不足 |
| RBAC | 部分实现 | ADMIN/OPERATOR 等角色和 `@RequireRole`；不是权限点模型 |
| 权限点 | 未实现 | 无统一 permission code/policy engine |
| 多租户 | 部分实现 | 表与多数 Mapper 带 tenant 条件；入口身份可伪造，无法称安全多租户 |
| 店铺数据隔离 | 部分实现 | 多数业务 SQL带 shop 条件；依赖不可信请求上下文 |
| 订单 | 部分实现 | 数据表和查询/汇总工具；缺订单管理完整 CRUD、状态机、平台同步 |
| 商品 | 部分实现 | 表、候选查询、标题优化/修改工具；非完整商品中心 |
| 评论 | 部分实现 | 表、差评查询、情感和回复草稿；非完整评论工单系统 |
| 报表 | 完整接入（演示范围） | 生成、持久化、查询、Excel/飞书相关执行器；生产外部交付可靠性不足 |
| Dashboard | 部分实现 | 汇总 API、图表页面；指标来自当前演示表 |
| 审批 | 部分实现 | 创建、决策、撤回、批量、过期和工具接入；可配置绕过高风险审批 |
| 审计 | 部分实现 | Auth、Connector、Tool、Trace 和管理时间线；非防篡改审计 |
| 任务中心 | 完整接入（当前范围） | 创建、查询、步骤、事件、重试、恢复和管理页面 |
| RabbitMQ | 部分实现 | Producer/Consumer/Queue/DLQ 声明；默认关闭，可靠投递语义不完整 |
| 重试 | 部分实现 | 任务 retry、Verifier 一次 repair、连接器手工 retry；无统一策略 |
| 死信 | 仅配置/数据通道 | DLQ 已声明；无明确失败投递与消费回放主流程 |
| 任务恢复 | 部分实现 | stale requeue API；无自动 lease/heartbeat/reaper |
| 幂等 | 部分实现 | 任务状态 CAS 降低重复消费；写工具无统一业务幂等键 |
| 本地事务 | 部分实现 | 部分单操作 Mapper 更新；核心任务创建/事件/消息未形成完整事务边界 |
| Outbox | 未实现 | 未发现 outbox 表、publisher 或 relay |
| 连接器 | 部分实现 | 文件读取、凭据、状态、检查任务和日志；无真实数据同步映射闭环 |
| 凭据加密 | 部分实现 | AES-GCM；默认密钥、无轮换/KMS |
| Agent Planner | 部分实现 | 规则 Planner 真接入；模型 Planner 可选且默认关闭 |
| Plan Validator | 完整接入（基础规则） | 真正在 Engine 中调用；校验深度有限 |
| Executor | 完整接入 | 顺序执行并记录步骤 |
| Verifier | 完整接入（基础验证） | 能控制 repair/失败路由；验证能力较浅 |
| Repair | 部分实现 | 单次定向证据补充，不是通用修复 |
| Tool Registry | 完整接入 | DB 工具目录和管理 API |
| Tool Gateway | 完整接入 | 启用检查、审批、执行、日志、Trace |
| 工具风险等级 | 完整接入（元数据） | LOW/MEDIUM/HIGH 等写入工具目录和日志 |
| 工具审批 | 部分实现 | Gateway 真接入；可由店铺配置绕过 |
| 前端页面 | 完整实现（管理端演示范围） | React 多入口页面并调用 API，不是纯静态 Mock |
| 前端权限 | 部分实现/不可信 | 页面会携带角色，但真正安全必须由后端决定；本地存储可修改 |
| 单元测试 | 部分实现 | 多个 Service/Executor 测试；覆盖率未测 |
| 集成测试 | 部分实现 | Spring MockMvc/JDBC 相关测试存在；基础设施真实程度需运行环境验证 |
| CI | 完整配置 | Maven test、npm build、Compose config、Docker build |
| Metrics | 部分实现 | 业务/任务聚合指标；无 Micrometer/Prometheus 生产指标体系 |
| Trace | 部分实现 | DB Span 真接入；非 OpenTelemetry 分布式追踪 |
| 健康检查 | 部分实现 | 系统健康 API和 Docker 静态页 healthcheck；未覆盖依赖健康语义 |
| Docker 部署 | 部分实现 | 后端+MySQL demo Compose；Redis/RabbitMQ 未进入 demo 主链路，密钥弱 |
| Evaluation | 部分实现 | 14 个本地 JSON case 和 API/脚本；不是外部独立 benchmark |

## 12. 关键风险

### P0

1. **客户端可伪造身份与租户上下文**
   - `shopops.auth.header-dev-mode=true` 为默认值。
   - 无 Bearer Token 时接受 `X-Tenant-Id`、`X-Shop-Id`、`X-User-Id`、`X-User-Roles`。
   - React 默认主动发送这些字段并使用 `ADMIN,OPERATOR`。
   - 影响：任意调用者可能伪造管理员、跨租户或跨店铺访问。

2. **高风险审批可由店铺配置绕过**
   - Tool Gateway 读取 `agent_tool_approval_enabled`。
   - 当工具 `needApproval=true` 但该配置为 false 时仍直接执行，并只记录治理备注。
   - 影响：退款、标题修改等高风险写操作不满足“不可绕过审批”。

3. **默认密钥和默认数据库凭据**
   - Token secret、Connector secret、MySQL root/root、RabbitMQ shopops/shopops 均有可直接使用默认值。
   - Demo Compose 同样提供固定默认密钥。
   - 影响：误用默认部署会导致 Token 伪造和凭据解密。

### P1

1. **任务落库和 RabbitMQ 发布存在丢失窗口**：无 Outbox 和 publisher confirm。
2. **外部写工具缺统一幂等语义**：重复提交、超时后状态未知和人工重试可能重复产生副作用。
3. **RabbitMQ 失败未必进入 DLQ**：Worker 把异常转换为 FAILED 后返回，Consumer 不再抛错。
4. **Worker 崩溃任务可能长期 RUNNING**：恢复需要手工调用 stale requeue。
5. **审批与实际执行之间缺少不可变输入摘要校验**：只校验 approvalId 状态和 toolCode，未证明重试输入与审批输入完全一致。
6. **任务创建、步骤、事件等多表写入原子性需要加强**：未看到覆盖整个创建流程的事务边界和失败补偿。
7. **连接器重试是同步手工重跑**：对外部结果未知和非幂等 API 没有分类处理。

### P2

1. Redis 依赖和 Compose 存在但未形成实际缓存、锁或 rate limit 能力。
2. 连接器 Sync Job 实际只是状态检查，不是真实数据同步。
3. Trace 是数据库自定义 Span，无 OpenTelemetry/Micrometer 和标准指标出口。
4. 无覆盖率报告，无法验证 README 对质量的暗示。
5. 集成测试是否依赖真实 MySQL/RabbitMQ 未在本环境完成执行验证。
6. Flyway 使用自定义开关，Spring 原生 Flyway 明确关闭，增加配置理解成本。
7. 模型调用日志、工具输入摘要和错误信息需要进一步检查脱敏策略。
8. 访问日志会记录异常 message；若下游异常包含凭据或响应体，可能泄密。

### P3

1. README 仍将系统定义为 AgentOps 平台，与新的“运营平台为主体”定位不一致。
2. 根 artifactId 和大量文档以 Agent 为主，业务平台边界不清晰。
3. 前端是多 HTML 入口，没有统一前端路由与权限守卫体验。
4. 演示文档数量很多，部分文件名在压缩包中被编码为 `#Uxxxx`，可维护性较差。

## 13. 构建与测试基线

执行环境：

- 审计日期：2026-08-05
- OS：容器 Linux
- Java：OpenJDK 21.0.10（项目目标 Java 17）
- Node.js：22.16.0
- npm：10.9.2
- Maven：未安装
- Docker：未安装
- MySQL/Redis/RabbitMQ：未启动

| 命令 | 结果 | 说明 |
|---|---|---|
| `mvn --batch-mode --no-transfer-progress test` | 未执行成功 | `mvn: command not found`；仓库未提供 Maven Wrapper |
| `mvn package` | 未执行 | 同上 |
| `npm ci` | 失败 | 内部 npm 镜像中 `zrender-5.6.1.tgz` 返回 404；不是已确认的仓库代码错误 |
| `npm run typecheck` | 不适用 | `package.json` 无独立 `typecheck` 脚本；类型检查包含在 `npm run build` 的 `tsc -b` 中 |
| `npm run build` | 失败 | `npm ci` 未完成，依赖不存在，首先出现 React/Ant Design 模块缺失及其连锁类型错误，不能据此认定源码本身类型失败 |
| `docker compose config` | 未执行成功 | `docker: command not found` |
| 数据库迁移 | 未运行 | 缺 MySQL/Maven 运行环境 |
| RabbitMQ 核心回归 | 未运行 | 缺 Maven、RabbitMQ |

仓库包含 GitHub Actions，配置会在官方 runner 上执行：

- `mvn -pl shopops-admin -am test`
- `npm ci`
- `npm run build`
- `docker compose ... config --quiet`
- Docker image build

本次没有访问 GitHub Actions 历史结果，因此不能宣称 CI 当前通过。README 中“88 tests, 0 failures, 8 skipped”“14/14 通过”等内容属于仓库历史自述，本阶段未复验。

## 14. README 与实际实现差异

1. “Agent 任务流已完成同步/异步执行”
   - 代码存在同步和 RabbitMQ 两套实现，但默认同步，异步可靠性不完整。

2. “高风险工具进入审批流程”
   - 通常成立，但店铺配置可以关闭审批并直接执行，因此不能表述为不可绕过。

3. “连接器同步”
   - 当前 Sync Job 主要做可用性检查，没有数据拉取、映射和持久化业务实体。

4. “Redis and RabbitMQ optional infrastructure”
   - RabbitMQ 有真实代码；Redis 基本停留在依赖和配置层。

5. “完整管理前端”
   - 页面与 API 调用真实存在，不是纯静态 Mock；但安全依赖后端，前端角色字段不可信。

6. “量化验收结果”
   - 文档和结果文件存在，但本阶段没有复跑，不能作为当前版本已验证结果。

## 15. 哪些能力当前不能在简历中宣称

不能直接宣称：

- “生产级安全多租户”或“彻底防止跨租户访问”
- “完善 RBAC/细粒度权限体系”
- “高风险操作强制审批、不可绕过”
- “Exactly-once 消息处理”或“完整幂等保障”
- “基于 Outbox 的事务消息一致性”
- “RabbitMQ 自动重试、死信治理和自动恢复闭环”
- “企业级外部电商平台连接器同步”
- “Redis 缓存、分布式锁或限流已落地”
- “OpenTelemetry 分布式追踪和 Prometheus 监控”
- “真实生产压测/高并发验证”
- “当前版本 88 个测试全部通过”或“14/14 评测通过”，除非在可复现环境重新执行
- “Agent Verifier 完成业务正确性、来源、时效与冲突验证”
- “Agent 自主拥有并执行平台权限”——正确表述应是 Agent 生成计划，平台执行工具；但当前平台权限治理仍需强化

当前可较准确表述：

- 实现了 Spring Boot + MyBatis + React 的电商运营管理平台原型
- 实现任务、工具、报告、审批、审计和 Agent 编排的可运行代码闭环
- 实现规则 Planner、计划校验、顺序工具执行、基础证据验证和一次定向修复
- 实现 MySQL 持久化和可选 RabbitMQ 派发代码
- 实现 MCP 风格工具注册、调用日志和基础风险元数据
- 实现 AES-GCM 凭据加密原型、文件型数据连接器和连接器管理后台

## 16. 后续阶段推荐顺序

1. **阶段 1：认证、租户和授权边界加固**
   - 默认关闭 header dev mode；正式环境 fail-fast；后端绑定 tenant/shop/user；建立 permission code；全面审计 Mapper 和管理 API。

2. **阶段 2：工具治理与不可绕过审批**
   - Agent 无权限原则；风险策略集中化；高风险审批不可被店铺配置关闭；审批输入指纹；执行前重新授权。

3. **阶段 3：写操作幂等和外部状态未知治理**
   - Idempotency key、业务唯一约束、执行记录、超时分类、查询确认、人工介入状态。

4. **阶段 4：任务事务边界与 Outbox**
   - 任务创建与消息事件同事务；Outbox relay；publisher confirm；重复发布安全。

5. **阶段 5：RabbitMQ 消费、重试、DLQ 和自动恢复**
   - 明确 ack/requeue、退避、最大次数、DLQ 消费与回放；RUNNING lease/heartbeat/reaper。

6. **阶段 6：业务平台领域完善**
   - 订单、商品、评论、组织和店铺模块边界、状态机、写服务、事务与审计，进一步弱化 Agent 中心叙事。

7. **阶段 7：连接器真实同步框架**
   - Connector SPI、credential 生命周期、mapping、checkpoint、增量同步、upsert、限流、错误分类和审计。

8. **阶段 8：Agent 编排与验证增强**
   - Planner 合同、Plan Validator 权限与依赖校验、需求级 Verifier、冲突和时效验证、受控 repair。

9. **阶段 9：可观测性与测试基础设施**
   - Actuator、Micrometer、结构化日志脱敏、OTel；Testcontainers MySQL/RabbitMQ；覆盖率门槛和安全回归。

10. **阶段 10：前端平台化和部署加固**
    - 统一导航、后端权限驱动、错误与审批体验；生产配置校验、secret 注入、完整 Compose 和部署说明。

排序理由：先封堵越权与不可绕过审批，再解决重复副作用和消息一致性，之后扩展业务、连接器、Agent 和体验。

## 17. 实际完成范围

- 完成根目录、Maven 模块、启动入口、前端、迁移、配置、Docker、CI、README/文档的基线检查。
- 完成 Auth、Organization、Business、Agent、Tool、Approval、Audit、Connector、Report、Model、Evaluation、Persistence、消息生产消费和测试目录审计。
- 梳理 HTTP、Agent、异步和连接器实际链路。
- 建立能力矩阵与 P0—P3 风险清单。
- 尝试执行后端、前端和 Docker 基线命令并记录环境阻塞。
- 未修改业务代码或测试。

## 18. 未完成范围

- 未在真实 Maven/MySQL/RabbitMQ 环境运行后端测试、迁移和异步集成回归。
- 未成功安装前端依赖，因此未完成可信的前端 TypeScript 构建验证。
- 未运行 Docker Compose 和镜像构建。
- 未做任何阶段 1 安全修复、幂等改造、Outbox 或前端重构。
- 未复验 README 历史性能、评测和批量飞书数据。

## 19. 修改文件清单

无。

## 20. 新增文件清单

- `docs/enterprise-upgrade/phase-0-audit.md`

## 21. 删除文件清单

无。

## 22. 核心设计说明

本阶段没有调整设计。后续应继续保留模块化单体，把安全上下文、权限策略、审批、幂等、事务消息和恢复机制作为平台级基础能力，而不是在 Agent 内部复制实现。Agent 只生成和提交计划；工具目录、授权策略、审批状态机和业务服务决定是否执行。

## 23. 数据库变化

无数据库 Schema 或数据变化。

## 24. 配置变化

无配置变化。

## 25. 已执行测试及结果

- Maven：命令启动失败，环境缺 Maven。
- npm ci：失败，内部 registry 缺 `zrender-5.6.1.tgz`。
- npm build：失败，因为依赖安装未完成。
- Docker：命令启动失败，环境缺 Docker。

没有任何测试被标记为 PASS。

## 26. 未能执行的验证

要完成完整基线复验，需要：

- Maven 3.9.x 或仓库新增 Maven Wrapper
- JDK 17
- 可访问 npm 官方 registry 或包含全部锁定依赖的镜像
- Docker Engine + Compose v2
- MySQL 8.4、RabbitMQ 3.13、Redis 7.4
- 可选飞书测试 Webhook和 OpenAI-compatible 模型端点

## 27. 已知风险

见第 12 节。当前阻断企业级上线的首要风险是开发头认证、审批绕过和默认密钥；首要可靠性风险是无 Outbox、无完整消息确认/恢复及外部写操作幂等不足。

## 28. 下一阶段依赖

阶段 1 开始前应提供或确认：

- 正式/开发 Profile 策略
- 期望的角色与权限点清单
- 平台管理员、租户管理员、店铺管理员、运营人员、审计员的权限边界
- 是否保留开发请求头模式及其严格限定方式
- 可运行的 Maven、MySQL 和测试环境

本阶段到此结束，不自动进入阶段 1。
