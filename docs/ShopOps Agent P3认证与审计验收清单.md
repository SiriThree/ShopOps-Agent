# ShopOps Agent P3 认证与审计验收清单

本文档用于验收 P3 阶段已经落地的认证、授权、Token 生命周期与审计能力。

## 1. P3 能力范围

P3 当前覆盖：

- 管理端登录：`POST /api/admin/auth/login`
- 当前用户信息：`GET /api/admin/auth/me`
- Bearer Token 鉴权
- Header 开发模式与严格 Bearer 模式切换
- 角色权限控制：`VIEWER` / `OPERATOR` / `ADMIN`
- 数据库角色解析：`user_account` / `tenant_member` / `shop_member`
- Token 会话表：`auth_token_session`
- 登出失效：`POST /api/admin/auth/logout`
- 认证审计表：`auth_audit_event`
- 审计查询接口：`GET /api/admin/auth/audit-events`

## 2. 默认测试账号

开发环境内置账号如下，默认密码都是 `shopops123`。

| username | password | role |
| --- | --- | --- |
| admin | shopops123 | ADMIN |
| operator | shopops123 | OPERATOR |
| viewer | shopops123 | VIEWER |

## 3. 启动 JDBC 环境

在项目根目录执行：

```powershell
docker compose -f deploy/docker-compose.dev.yml up -d
docker compose -f deploy/docker-compose.dev.yml ps
```

启动后端：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot'
$env:Path="$env:JAVA_HOME\bin;C:\Tools\apache-maven-3.9.16\bin;$env:Path"

mvn clean install -DskipTests
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.profiles=dev"
```

`dev` profile 会启用 JDBC、Flyway、Redis、RabbitMQ。当前 P3 需要 Flyway 至少迁移到 V7。

## 4. 登录并保存 Token

另开一个 PowerShell 终端执行：

```powershell
$loginBody = @{
  username = "admin"
  password = "shopops123"
  tenantId = 1
  shopId = 1
} | ConvertTo-Json -Depth 5

$loginResult = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/admin/auth/login" `
  -ContentType "application/json; charset=utf-8" `
  -Body $loginBody

$adminToken = $loginResult.data.accessToken
$adminToken
```

预期结果：

- `code = 200`
- 返回 `tokenType = Bearer`
- 返回 `accessToken`
- 返回 `expiresAt`
- `user.username = admin`
- `user.roles` 包含 `ADMIN`

## 5. Bearer Token 访问当前用户

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/auth/me" `
  -Headers @{
    Authorization = "Bearer $adminToken"
  }
```

预期结果：

- `code = 200`
- `data.username = admin`
- `data.authType = BEARER`
- `data.authenticated = true`

## 6. Header 开发模式

默认配置：

```yaml
shopops:
  auth:
    header-dev-mode: true
```

在默认模式下，本地开发仍可使用请求头模拟身份：

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/auth/me" `
  -Headers @{
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Roles" = "ADMIN"
  }
```

严格模式启动方式：

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--shopops.auth.header-dev-mode=false"
```

严格模式下，除登录接口外，业务接口必须携带合法 Bearer Token；伪造 Header 身份应返回 401。

## 7. 角色权限验收

登录 viewer：

```powershell
$viewerBody = @{
  username = "viewer"
  password = "shopops123"
  tenantId = 1
  shopId = 1
} | ConvertTo-Json -Depth 5

$viewerLogin = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/admin/auth/login" `
  -ContentType "application/json; charset=utf-8" `
  -Body $viewerBody

$viewerToken = $viewerLogin.data.accessToken
```

viewer 读取工具列表应成功：

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/tools" `
  -Headers @{
    Authorization = "Bearer $viewerToken"
  }
```

viewer 创建 Agent 任务应失败：

```powershell
$taskBody = @{
  taskType = "daily_review"
  userInput = "daily review"
  dateRange = @{
    start = "2026-07-18"
    end = "2026-07-18"
  }
} | ConvertTo-Json -Depth 5

try {
  Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/agent/tasks" `
    -Headers @{
      Authorization = "Bearer $viewerToken"
    } `
    -ContentType "application/json; charset=utf-8" `
    -Body $taskBody
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

预期结果：`403`。

## 8. 登出与 Token 失效

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/admin/auth/logout" `
  -Headers @{
    Authorization = "Bearer $viewerToken"
  }
```

预期结果：

- `code = 200`
- `data.status = REVOKED`

再次使用同一个 token：

```powershell
try {
  Invoke-RestMethod `
    -Method Get `
    -Uri "http://localhost:8080/api/admin/auth/me" `
    -Headers @{
      Authorization = "Bearer $viewerToken"
    }
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

预期结果：`401`。

## 9. 审计查询

查询登录成功事件：

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/auth/audit-events?eventType=LOGIN&eventStatus=SUCCESS&pageNum=1&pageSize=10" `
  -Headers @{
    Authorization = "Bearer $adminToken"
  }
```

查询权限拒绝事件：

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/auth/audit-events?eventType=ACCESS_DENIED&eventStatus=FAILURE&pageNum=1&pageSize=10" `
  -Headers @{
    Authorization = "Bearer $adminToken"
  }
```

查询登出事件：

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/admin/auth/audit-events?eventType=LOGOUT&eventStatus=SUCCESS&pageNum=1&pageSize=10" `
  -Headers @{
    Authorization = "Bearer $adminToken"
  }
```

审计事件字段包括：

- `tenantId`
- `shopId`
- `userId`
- `username`
- `eventType`
- `eventStatus`
- `authType`
- `requestId`
- `clientIp`
- `userAgent`
- `failureReason`
- `createdAt`

## 10. 数据库核验

进入 MySQL 容器：

```powershell
docker exec -it shopops-mysql mysql -uroot -proot shopops_agent
```

常用 SQL：

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT token_id, tenant_id, shop_id, user_id, username, status, issued_at, expires_at, revoked_at
FROM auth_token_session
ORDER BY id DESC
LIMIT 10;

SELECT event_type, event_status, auth_type, username, failure_reason, created_at
FROM auth_audit_event
ORDER BY id DESC
LIMIT 20;
```

预期结果：

- Flyway 已包含 `V6__p3_auth_audit.sql`
- Flyway 已包含 `V7__p3_token_session.sql`
- 登录后 `auth_token_session` 有 `ACTIVE` 记录
- 登出后对应 token session 变为 `REVOKED`
- 登录、权限拒绝、登出事件写入 `auth_audit_event`

## 11. 自动化测试命令

常规测试：

```powershell
mvn -pl shopops-admin -am test
```

JDBC 集成测试：

```powershell
mvn -pl shopops-admin test "-Dshopops.jdbc.it=true" "-Dtest=AgentTaskJdbcFlowIntegrationTest,AgentTaskJdbcFailureIntegrationTest,AuthAuditJdbcIntegrationTest,AuthTokenLifecycleJdbcIntegrationTest"
```

完整构建：

```powershell
mvn clean install
```

## 12. P3 验收结论

P3 已经从简单 Header 模拟身份推进到企业级认证基础能力：

- 本地开发仍可快速调试
- 严格模式支持 Bearer-only
- Token 可撤销
- 用户角色来自数据库成员关系
- 权限拒绝可审计
- 登录、失败、登出可追踪
- 认证链路有内存测试与 JDBC 集成测试覆盖

后续可在此基础上继续扩展刷新 token、多端设备会话、强制下线、登录失败锁定、密码重置、操作审计中心等能力。
