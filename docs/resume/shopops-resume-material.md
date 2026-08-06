# ShopOps 简历与面试材料

## 1. 30 秒介绍

ShopOps 是我用 Spring Boot、MyBatis、MySQL 和 React/TypeScript 实现的多店铺电商运营管理平台。平台覆盖组织权限、店铺数据、任务、审批、审计、报表和连接器；Agent 只是内部自动化模块。我重点改造了可信租户上下文、权限点、Tool Gateway 二次授权、高风险审批、写操作幂等、RabbitMQ 任务租约、连接器断点同步和 Agent 工作流治理。

## 2. 2 分钟介绍

ShopOps 的目标不是做一个聊天 Agent，而是模拟企业运营团队管理多个店铺时需要的统一后台。后端采用 Spring Boot 模块化单体，前端采用 React 和 TypeScript，数据使用 MySQL 和 Flyway 管理，RabbitMQ 承担异步任务。

我首先建立了可信身份上下文，让 tenantId 和 userId 来自验证后的身份，shopId 必须通过成员关系校验；角色映射为权限点，Controller、Tool Gateway 和 Worker 都会重新授权。其次，我选择退款执行作为旗舰写操作，设计审批参数摘要、数据库幂等键、写操作状态机、外部结果未知回查、Outbox 和对账语义。异步任务增加数据库租约、attempt、错误分类和取消状态，防止重复消息直接造成重复副作用。连接器方面没有堆很多浅层 API，而是把文件订单连接器做成分页、游标、指纹去重和 checkpoint。Agent 侧使用有限工作流模板与 ADVISORY、DRAFT、AUTOMATIC 三种模式，Plan Validator 会检查工具白名单、权限、风险和步骤预算，高风险操作仍由平台审批。

项目目前使用公开数据和模拟外部适配器，没有真实企业客户或生产流量；完整 Maven、Docker、前端和性能验证需要在具备依赖的环境重新执行。

## 3. 系统架构

```text
React Operations Console
→ Spring Boot Controllers
→ Trusted Context / Authorization
→ Business / Task / Approval / Audit / Connector
→ MyBatis + MySQL
→ RabbitMQ Worker
→ Controlled Agent → Plan Validator → Tool Gateway
```

## 4. 后端核心能力

- Spring Boot 模块化单体和 MyBatis 数据访问；
- 多租户、店铺成员关系、权限点和数据范围；
- 审批状态机、参数摘要和条件更新；
- 数据库唯一约束与业务幂等；
- 外部结果未知、回查、Outbox 和对账语义；
- RabbitMQ 消息消费与数据库租约；
- 连接器凭据、分页游标、去重和 checkpoint；
- Actuator、Micrometer、MDC、Audit 和 Trace。

## 5. 全栈能力

- React/TypeScript 运营后台；
- 运营总览、任务、审批、报告、连接器、工具、审计和组织管理导航；
- 基于后端权限点裁剪菜单；
- 统一 401 处理和店铺上下文切换；
- Vite 构建、Docker Compose 和 GitHub Actions。

## 6. Agent 的定位

Agent 负责理解目标、匹配受控模板、生成计划、调用治理后的工具、汇总证据和生成报告。权限、风险、审批、幂等、任务状态和最终业务成功由平台控制，不由模型决定。

## 7. 三个最困难问题

### 7.1 如何防止 Agent 越权

困难不在于 Planner 能不能生成工具名，而在于不能信任它。我的做法是两层校验：计划阶段由模板和 Plan Validator 校验工具白名单、权限、风险和执行模式；真正执行时 Tool Gateway 再次基于可信上下文授权，高风险工具必须审批。

### 7.2 如何处理外部写操作超时

超时不等于失败，直接重试可能重复退款。我把写操作设计成状态机，超时进入 EXTERNAL_UNKNOWN；先用业务幂等键和外部状态查询判断是否成功，再确认、补偿或转人工。

### 7.3 如何处理 RabbitMQ 重复消息和 Worker 崩溃

消息至少一次投递时不能依赖“消息只来一次”。Worker 通过数据库 CAS 获取任务租约，只有租约持有者执行；任务记录 attempt、leaseExpireAt 和错误类型。当前周期心跳和完整自动接管仍需进一步完成和验证。

## 8. 三个工程亮点

1. 将多租户权限从前端 Header 收敛为后端可信上下文，并贯穿 Tool 和 Worker。
2. 将退款写操作从随机模拟结果升级为审批摘要、幂等、未知结果回查和 Outbox 语义。
3. 将 Agent 从自由工具组合收敛为有限模板、执行模式、Plan Validator 和有限修复。

## 9. 五条简历候选描述

1. 基于 Spring Boot、MyBatis、MySQL 与 React/TypeScript 构建多店铺电商运营管理平台，覆盖组织权限、店铺、任务、审批、报表、连接器与审计模块。
2. 设计可信租户上下文与权限点治理，基于用户店铺成员关系校验数据范围，并在 Controller、Tool Gateway 与 RabbitMQ Worker 执行前重新授权。
3. 为高风险退款链路实现审批参数摘要、数据库唯一幂等键、写操作状态机、外部结果未知回查、Outbox 与人工对账语义，避免超时盲目重试。
4. 为异步 Agent 任务引入数据库租约、attempt、错误分类和取消状态，降低重复消息与并发 Worker 导致重复执行的风险。
5. 将 Agent 约束为受控工作流模块，引入 ADVISORY/DRAFT/AUTOMATIC 模式、工具白名单、风险上限、Plan Validator 与有限补证修复。

## 10. 指标定义与使用边界

当前仓库存在历史评测和公开数据基线文件，但本轮没有重新运行完整 Maven 测试、Docker、k6 和 Agent 批量评测。简历不应写未经当前版本复验的通过率、P95、成功率和吞吐量。

只有重新执行后，指标必须注明：分母、数据来源、数据规模、环境、日期、脚本和限制。

## 11. 已知限制

- 未连接真实商业电商平台；
- 退款执行为模拟外部适配器；
- 权限点主要为代码映射；
- RabbitMQ 心跳、DLQ、Outbox 自动发布仍不完整；
- 前端缺少完整订单/商品/评论运营页与 E2E；
- OpenTelemetry、告警和真实性能基线未完成；
- Agent Schema、DAG、Token/成本预算和分层 Verifier 有限。

## 12. 30 个高频追问与回答边界

1. **为什么用模块化单体？** 当前规模先保证事务、权限和模块边界，微服务会增加分布式一致性成本。
2. **tenantId 从哪里来？** 来自验证后的身份，不信任普通请求参数。
3. **shopId 为什么还能由前端传？** 它只是选择上下文，后端必须验证成员关系。
4. **如何防止按 ID 越权？** Repository/Service 查询应同时带 tenant/shop 条件；当前仍需持续 Mapper 审计。
5. **RBAC 是否数据库化？** 当前角色到权限点主要是代码映射，不应说成完整动态 RBAC。
6. **Agent 有权限吗？** 没有独立权限，只继承发起主体并在执行前重新授权。
7. **为什么需要 Plan Validator？** Planner 输出不可信，需要限制模板、工具、风险、权限和预算。
8. **AUTOMATIC 能执行退款吗？** 不能，HIGH/CRITICAL 始终审批。
9. **审批如何防重复？** 状态条件更新/CAS，只允许一次合法决策。
10. **如何保证审批内容没被替换？** 审批和执行都计算规范化参数摘要并比对。
11. **幂等只用 Redis 锁吗？** 不是，使用数据库唯一约束、执行记录和业务状态。
12. **幂等键是什么？** 工具、租户、店铺、业务对象和 operationRequestId。
13. **外部超时怎么办？** 进入 EXTERNAL_UNKNOWN，先回查，不直接重复写。
14. **Outbox 完整吗？** 有表、状态和重放入口，但多实例 claim 与 publisher confirm 仍不完整。
15. **RabbitMQ 重复消息怎么办？** Worker 必须先数据库 CAS 获取租约。
16. **Worker 崩溃能恢复吗？** 有租约过期字段和恢复基础，但周期心跳与完整自动接管未完全验证。
17. **取消能回滚已完成操作吗？** 不能假装回滚，不可逆步骤保留并可能转人工/补偿。
18. **错误为什么分类？** 权限、校验、429、网络超时和外部未知需要不同策略。
19. **连接器为什么选文件？** 当前没有真实商业 API，做深分页、游标、去重和 checkpoint 比伪造平台接入更真实。
20. **凭据安全吗？** 加密存储且 API 不返回明文；真实密钥仍需安全配置来源。
21. **Redis 用在哪里？** 当前主要是依赖和健康检查，不能夸大为完整缓存系统。
22. **Verifier 是 LLM 自评吗？** 不是，主要检查工具结果、报告和证据；但数据库/外部回查覆盖仍有限。
23. **Repair 是自主纠错吗？** 是规则受限的一次补证，不是通用自主纠错。
24. **Agent 成本预算完成了吗？** 没有完整 Token/成本预算执行。
25. **前端如何防串店铺？** 切换时清理旧上下文并广播变化；完整 Query Cache 尚未引入。
26. **为什么前端权限不能保证安全？** 菜单隐藏可绕过，后端才是最终裁决者。
27. **可观测性有哪些？** MDC、Audit、TraceSpan、Tool/Connector 日志、Actuator 和 Micrometer；不是完整 OTel。
28. **测试通过多少？** 本轮环境未完整执行，不能给通过数。
29. **性能是多少？** 有 k6 脚本但本轮未执行，不能给 P95/P99。
30. **项目有没有真实客户？** 没有，使用公开数据、模拟外部适配器和人工构造测试。
