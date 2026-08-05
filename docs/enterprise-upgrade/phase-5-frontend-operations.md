# Phase 5 — 企业运营前端体验改造

执行日期：2026-08-05

## 1. 阶段审计结论

当前前端是 Vite + React 19 + TypeScript + Ant Design 的多入口管理端，而不是单页路由应用。每个 HTML 入口对应一个 TSX 页面，包括 Dashboard、任务、审批、报告、审计、工具、连接器、Prompt、组织、认证和 Agent 工作台。

实际问题：

1. `AdminSidebar` 将 Agent 工作台放在首位，品牌副标题为“Agent 运营平台”，与 ShopOps 的全栈运营平台定位不一致。
2. 各页面自行构造 `RequestContext` 和顶部上下文输入，存在重复实现。
3. Bearer Token 请求仍同时发送 `X-Tenant-Id`、`X-User-Id` 和角色 Header，虽然后端阶段 1 已收紧，但前端语义仍会误导并扩大攻击面。
4. 菜单不读取 `/auth/me` 返回的 permissions，所有登录用户看到相同入口。
5. 店铺切换没有统一缓存清理协议，页面间通过 localStorage 用户对象间接共享上下文。
6. API 错误没有统一 401 会话清理和回登录行为。
7. Dashboard 已有指标和失败事件，但待审批、需人工处理任务、连接器异常没有形成行动入口。
8. 前端为多入口静态页面，没有统一 React Router、Query Cache 或全局 Store。阶段内未强行引入大型框架，避免大规模重写。
9. 当前依赖未安装，无法证明类型检查和构建通过。

## 2. 实际完成范围

### 2.1 平台定位与导航

- 品牌副标题调整为“多店铺运营管理平台”。
- 导航顺序调整为：运营总览、任务中心、审批中心、报告中心、连接器、自动化工作台、工具治理、审计中心、组织与店铺、Prompt 配置、身份与会话。
- Agent 由平台主入口降为业务上下文中的自动化能力。
- 导航增加图标、移动端折叠和运营工作台分组标签。

### 2.2 前端权限体验

- 新增 `session.ts` 作为会话和权限读取入口。
- 菜单根据后端返回的 `permissions` 进行隐藏。
- ADMIN/TENANT_ADMIN 保留全部菜单体验。
- 前端权限只影响展示，不替代后端授权。

### 2.3 安全 API Client

- Bearer Token 模式不再发送 `X-Tenant-Id`、`X-User-Id` 和 `X-User-Roles`。
- Bearer 模式只发送 `Authorization` 和当前店铺 `X-Shop-Id`，最终店铺授权仍由后端校验。
- 无 Token 时保留开发 Header 上下文，以兼容明确启用的 dev profile。
- 新增统一 API 错误模型。
- 收到 401 时清理 Token、用户、店铺上下文和 sessionStorage，并跳转认证页。

### 2.4 店铺上下文

- 新增统一店铺上下文 Key 和切换函数。
- 切换前根据 `accessibleShopIds` 校验目标店铺。
- 切换店铺后清理 sessionStorage，并广播 `shopops:context-changed` 事件。
- `readStoredContext` 优先读取独立的当前店铺上下文。

### 2.5 行动型 Dashboard

Dashboard 新增“今日待办”，真实请求：

- PENDING 审批；
- NEEDS_MANUAL_ACTION 任务；
- 不可用或失败的连接器；
- 当前店铺自动化任务入口。

所有卡片可跳转到对应运营页面，没有新增伪造业务指标。

### 2.6 响应式与样式

- 导航增加图标与选中状态。
- 顶部栏在窄屏下改为纵向排列。
- 内容区在移动端缩小边距。
- 新增行动卡片样式。

## 3. 未完成范围

1. 各页面仍有重复的顶部 ContextInputs，尚未全部迁移到共享 AppShell。
2. 尚未引入 React Router；仓库仍采用多 HTML 入口。
3. 尚未引入 TanStack Query 或统一 Query Cache。
4. 订单、商品、评论当前没有独立 React 页面，不能在不扩张后端/API 的情况下伪造完成。
5. 任务中心尚未展示完整 attempt 历史、租约和恢复操作者。
6. 审批详情仍依赖后端当前字段，缺少修改前/修改后结构化 diff 和证据模型。
7. Agent 上下文融合主要存在于独立工作台，尚未嵌入订单、商品、评论页面，因为这些页面当前不存在。
8. 403/404 独立页面尚未创建。
9. 没有完成组件测试和 E2E 测试。
10. 前端类型检查与构建未通过环境验证。

## 4. 修改文件清单

- `shopops-admin-ui/src/AdminSidebar.tsx`
- `shopops-admin-ui/src/api.ts`
- `shopops-admin-ui/src/dashboard.tsx`
- `shopops-admin-ui/src/types.ts`
- `shopops-admin-ui/src/styles.css`

## 5. 新增文件清单

- `shopops-admin-ui/src/session.ts`
- `docs/enterprise-upgrade/phase-5-frontend-operations.md`

## 6. 删除文件清单

无。

## 7. 核心设计说明

本阶段没有把 ShopOps 改成聊天页面，也没有引入新的大型状态管理框架。改造重点是先纠正产品信息架构和安全语义：平台业务入口优先，Agent 作为自动化入口；Bearer 身份只由 Token 证明；店铺切换需要在已授权范围内；菜单权限仅提供体验层裁剪；所有后端接口继续作为最终安全边界。

## 8. 数据库变化

无。

## 9. 配置变化

无后端配置变化。前端新增 localStorage Key：

- `shopops.context.shopId`

新增浏览器事件：

- `shopops:context-changed`

## 10. 已执行测试及结果

### `npm run typecheck`

结果：失败。

原因：当前工作目录没有安装 node_modules，TypeScript 无法解析 React、Ant Design、Axios、Vite、ECharts 等依赖。随后产生的大量 JSX 与隐式 any 错误不能用于判断修改后的真实类型状态。

### 静态检查

- 人工核对新增 TS/TSX 文件导入、导出和 JSX 结构。
- 未发现本阶段新增文件的明显括号不平衡。
- 静态检查不等同于 TypeScript 构建通过。

## 11. 未能执行的验证

- `npm ci`：此前阶段已确认内部 npm 镜像缺少 `zrender-5.6.1.tgz`，本阶段未伪造依赖安装成功。
- `npm run build`：依赖未安装，无法得到有效构建结果。
- 前端组件测试：仓库没有测试框架配置。
- E2E：仓库没有 Playwright/Cypress 配置和可运行后端环境。
- 后端 API 联调：当前环境没有启动 ShopOps 后端及基础设施。

## 12. 已知风险

- 前端权限数据依赖本地保存的 `/auth/me` 响应，权限撤销后需要刷新当前用户信息才能更新菜单；后端仍会立即拒绝越权请求。
- 多入口页面尚未统一监听 `shopops:context-changed`，当前切换函数为后续统一 AppShell 提供协议，但未接入全部页面。
- localStorage 本身不是可信安全边界，只用于 UI 上下文；tenant、user 和权限必须由后端重新裁决。
- 未经构建验证，新增代码可能仍存在依赖版本相关类型问题。

## 13. 下一阶段依赖

在进入后续阶段前，应先在可访问 npm registry 的环境执行：

```bash
cd shopops-admin-ui
npm ci
npm run typecheck
npm run build
```

如要继续深化前端，应优先：

1. 抽取统一 AppShell、PageHeader 和 ShopSwitcher；
2. 修复全部 TypeScript strict 错误；
3. 增加订单、商品、评论真实 API 页面；
4. 加入 Query Cache 并在店铺切换时统一清理；
5. 增加任务、审批关键流程的组件测试与 E2E。

## 14. 简历声明边界

可以谨慎声明：

> 重构 ShopOps 企业运营前端的信息架构，将 Dashboard、任务、审批、报告和连接器作为主工作流，并实现基于后端权限点的菜单裁剪、Bearer 请求身份收敛、统一 401 会话处理和店铺上下文清理协议。

不能声明：

- 前端已完整实现订单、商品、评论运营工作台；
- 前端类型检查和构建已经通过；
- 已实现完整 Query Cache、组件测试或 E2E；
- 已完成所有页面的店铺切换防串数据验证；
- 已把 Agent 完整嵌入所有业务页面。
