# ShopOps 企业级改造阶段 1：多租户、安全边界与权限治理

- 执行日期：2026-08-05
- 输入基线：阶段 0 审计后的仓库
- 阶段范围：可信身份、多租户/店铺授权、权限点、Agent/Tool/异步继承、生产安全配置

## 1. 阶段审计结论

原实现的 Bearer Token 包含 tenant/shop/user/roles，但请求时仅校验 Token Session，没有重新核验用户当前是否仍属于该租户和店铺；开发 Header 认证默认开启，并可由请求直接提交 tenant/shop/user/roles。Controller 普遍从 RequestContext 取 tenant/shop，Mapper 大多带 tenant/shop 条件，但缺少统一授权服务。Tool Gateway 只检查工具元数据和审批，不检查主体权限点；高风险审批可被店铺配置关闭。RabbitMQ 消费者直接使用消息身份字段，未与持久化任务身份比对，也不检查执行时最新权限。

## 2. 实际完成范围

1. 新增统一 `AuthorizationService`，从租户/店铺成员关系解析可访问店铺、角色、权限点和数据范围。
2. Bearer Token 请求每次重新核验当前 tenant/shop/user 的有效成员关系，不再直接信任 Token 内旧角色。
3. Header 开发认证仍仅用于 dev/test，但角色不再取自 `X-User-Roles`，而是从持久化成员关系解析。
4. `RequestContext` 增加 `accessibleShopIds`、`permissions`、`dataScope`、`traceId`。
5. 新增统一 HTTP 权限拦截器，对组织、审计、连接器、审批、工具、Agent、报表、Dashboard、模型/Prompt 等入口实施权限点检查。
6. Tool Gateway 在执行前根据工具 `permissionCode` 重新授权。
7. HIGH/CRITICAL 工具即使店铺关闭审批配置，也不能绕过审批。
8. RabbitMQ Worker 将消息身份与持久化任务身份比对，并在执行前重新检查最新 `agent:execute` 权限；权限撤销后任务转失败。
9. 默认配置关闭 Header 开发认证；新增 dev/test/prod profile。
10. prod profile 启动时拒绝 Header 开发认证、默认密钥、空密钥和短于 32 字符的密钥。
11. `/auth/me` 返回权限、可访问店铺和数据范围快照。

## 3. 未完成范围

- 尚未建立数据库化的 role-permission/resource-policy 表；当前角色到权限点映射在授权服务中集中维护。
- `SELF_CREATED` 数据范围已有枚举，但当前业务表尚未统一具备 created_by 字段，未形成完整查询策略。
- 未对每个 Mapper 做自动 SQL 注入式租户插件；仍依赖现有 tenant/shop 条件和统一入口授权，后续需要持续静态审计。
- 未新增缓存，因此不存在新增缓存串租户问题；未来缓存 key 必须含 tenant/shop。
- 未实现异步授权版本号或权限快照差异审计表，本阶段采用执行时实时重新授权。
- 前端仍保留 Tenant/Shop 输入框用于开发体验；Bearer 模式下后端忽略其身份声明，店铺切换仍需要重新登录/签发 Token。

## 4. 修改文件

- `application.yml`
- `.github/workflows/ci.yml`
- `RequestContext.java`
- `RequestContextResolver.java`
- `CurrentUserDto.java`
- `AuthUserMapper.java`
- `WebMvcConfig.java`
- `DefaultToolGatewayService.java`
- `JdbcAgentTaskExecutionWorker.java`

## 5. 新增文件

- `00_全局执行约束.md`
- `application-dev.yml`
- `application-test.yml`
- `application-prod.yml`
- `PermissionCode.java`
- `DataScope.java`
- `AuthorizationService.java`
- `JdbcAuthorizationService.java`
- `InMemoryAuthorizationService.java`
- `PermissionAuthorizationInterceptor.java`
- `ProductionSecurityConfigurationValidator.java`
- 两个安全单元测试
- 本交接文档

## 6. 删除文件

无。

## 7. 核心设计说明

可信身份来源为已验证 Bearer Token 或仅在 dev/test profile 开启的 Header 开发模式。无论哪种入口，tenant/shop/user 都必须通过 `AuthorizationService.resolve` 验证成员关系。权限点由角色集中映射，HTTP 入口先校验权限，Tool Gateway 再根据工具注册的 permissionCode 二次校验。Agent 仅产生计划，工具执行仍由平台权限和审批裁决。异步消息不是权威身份来源，Worker 以持久化任务为事实并实时检查最新权限。

数据范围当前支持 `ALL_TENANT`、`ASSIGNED_SHOPS`、`SELF_CREATED` 三种表达；ADMIN 映射为 ALL_TENANT，OPERATOR/VIEWER 映射为 ASSIGNED_SHOPS。

## 8. 数据库变化

无新增迁移。复用 `tenant_member`、`shop_member`、`shop`、`user_account` 和工具 `permission_code` 字段。

## 9. 配置变化

- 基础 `shopops.auth.header-dev-mode`：`true` 改为 `false`。
- dev profile：显式开启 Header 开发认证。
- test profile：显式开启 Header 开发认证并使用独立测试密钥。
- prod profile：只允许环境变量提供 Token/Credential Secret，并启用启动安全校验。
- CI 后端测试显式使用 `test` profile。

## 10. 已执行测试及结果

- `mvn test`：未执行，环境没有 Maven，命令退出码 127。
- `mvn package -DskipTests`：未执行，环境没有 Maven，命令退出码 127。
- `npm ci`：失败；内部 npm 镜像返回 `zrender-5.6.1.tgz` 404。
- `npm run typecheck`：失败；仓库没有 `typecheck` script。
- `npm run build`：失败；`npm ci` 未完成，缺少 TypeScript/Vite 依赖。
- `docker compose -f deploy/docker-compose.demo.yml config`：未执行，环境没有 Docker，退出码 127。

新增测试代码未被表述为已通过。

## 11. 未能执行的验证

需要 Maven 3.9+、可访问 Maven Central/镜像的网络环境、可提供 zrender 5.6.1 的 npm registry，以及 Docker Engine/Compose。必须在这些环境中执行完整后端测试、打包、前端构建、Compose 校验和数据库集成测试。

## 12. 已知风险

- P0：数据库 Mapper 仍需后续逐表自动化审计，防止新代码遗漏 tenant/shop 条件。
- P1：RabbitMQ 消息与数据库仍无 Outbox；本阶段只解决身份继承，不解决消息一致性。
- P1：权限撤销导致任务失败，但尚未形成专门的“授权已变化”状态和审计字段。
- P2：权限映射尚未数据库化，新增角色/权限需改代码。
- P2：前端未根据 `/auth/me` 权限快照完成所有按钮级体验控制；后端已作为最终裁决者。

## 13. 下一阶段依赖

在进入下一阶段前，必须先在具备 Maven、MySQL、RabbitMQ、npm 与 Docker 的环境中完成本阶段安全集成测试，重点验证跨租户 ID、未分配店铺、工具未授权、审批跨租户、RabbitMQ 身份伪造和 prod 默认密钥拒绝启动。

## 14. 阶段交接结论

本阶段已将身份、店铺成员、权限点、Tool 和异步 Worker 的授权边界接入真实主流程，并关闭生产默认开发认证与危险密钥。但由于当前执行环境缺少 Maven/Docker 且 npm 镜像不完整，尚不能声明跨租户安全测试和原有核心回归已真实通过。不得自动进入阶段 2。
