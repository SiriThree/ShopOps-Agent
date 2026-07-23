import React, { useCallback, useEffect, useMemo, useState } from "react";
import ReactDOM from "react-dom/client";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Flex,
  Form,
  Input,
  Layout,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message
} from "antd";
import {
  CopyOutlined,
  LoginOutlined,
  LogoutOutlined,
  ReloadOutlined,
  RobotOutlined,
  SearchOutlined,
  UserOutlined
} from "@ant-design/icons";
import { apiGet, apiPost, readStoredContext, type RequestContext } from "./api";
import type { AuthAuditEvent, CurrentUser, LoginResult, LogoutResult, PageResult } from "./types";
import { numberText } from "./utils";
import "./styles.css";

const { Header, Content, Sider } = Layout;
const { Paragraph, Text, Title } = Typography;

const TOKEN_KEY = "shopops.auth.token";
const USER_KEY = "shopops.auth.user";

const navItems = [
  ["/admin/workbench.html", "Agent 工作台"],
  ["/admin/dashboard.html", "Dashboard"],
  ["/admin/tasks.html", "任务队列"],
  ["/admin/reports.html", "报告"],
  ["/admin/audit.html", "审计中心"],
  ["/admin/tools.html", "工具"],
  ["/admin/approvals.html", "审批"],
  ["/admin/connectors.html", "Connector"],
  ["/admin/prompts.html", "Prompt"],
  ["/admin/users.html", "组织"],
  ["/admin/auth.html", "认证"]
];

const eventTypes = ["LOGIN", "LOGOUT", "ACCESS_DENIED", "ORG_USER_CREATED", "ORG_MEMBER_UPDATED"];
const eventStatuses = ["SUCCESS", "FAILURE"];

type LoginForm = {
  username?: string;
  password?: string;
};

type AuditFilters = {
  eventType?: string;
  eventStatus?: string;
  username?: string;
  userId?: string;
  requestId?: string;
};

function AuthApp() {
  const storedContext = readStoredContext();
  const [loginForm] = Form.useForm<LoginForm>();
  const [filterForm] = Form.useForm<AuditFilters>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(() => readStoredUser());
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) || "");
  const [events, setEvents] = useState<AuthAuditEvent[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<AuthAuditEvent | null>(null);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [loading, setLoading] = useState(false);
  const [statusLine, setStatusLine] = useState("认证中心已就绪，可登录、校验当前用户、退出并查看认证审计事件。");

  const metrics = useMemo(() => {
    const failures = events.filter((event) => event.eventStatus === "FAILURE").length;
    return {
      session: currentUser?.authenticated ? 1 : token ? 1 : 0,
      roles: currentUser?.roles?.length || 0,
      auditTotal: total,
      failures
    };
  }, [currentUser, events, token, total]);

  const buildQuery = useCallback(
    (nextPageNum = pageNum, nextPageSize = pageSize) => {
      const values = filterForm.getFieldsValue();
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "eventType", values.eventType);
      add(params, "eventStatus", values.eventStatus);
      add(params, "username", values.username);
      add(params, "userId", values.userId);
      add(params, "requestId", values.requestId);
      return params;
    },
    [filterForm, pageNum, pageSize]
  );

  const syncUrl = useCallback((params: URLSearchParams) => {
    window.history.replaceState(null, "", `${window.location.pathname}?${params.toString()}`);
  }, []);

  const loadAuditEvents = useCallback(
    async (nextPageNum = pageNum, nextPageSize = pageSize) => {
      const params = buildQuery(nextPageNum, nextPageSize);
      setLoading(true);
      try {
        const data = await apiGet<PageResult<AuthAuditEvent>>(`/api/admin/auth/audit-events?${params}`, context);
        const list = data.list || [];
        setEvents(list);
        setTotal(data.total || 0);
        setPageNum(nextPageNum);
        setPageSize(nextPageSize);
        setSelectedEvent((event) => event || list[0] || null);
        syncUrl(params);
        setStatusLine("认证审计事件已刷新。");
      } catch (error) {
        setEvents([]);
        setTotal(0);
        setStatusLine(`认证审计事件加载失败：${errorMessage(error)}`);
        message.error(errorMessage(error));
      } finally {
        setLoading(false);
      }
    },
    [buildQuery, context, pageNum, pageSize, syncUrl]
  );

  const loadCurrentUser = useCallback(async () => {
    setLoading(true);
    try {
      const user = await apiGet<CurrentUser>("/api/admin/auth/me", context);
      setCurrentUser(user);
      localStorage.setItem(USER_KEY, JSON.stringify(user || {}));
      setStatusLine("当前用户已刷新。");
    } catch (error) {
      setStatusLine(`当前用户加载失败：${errorMessage(error)}`);
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [context]);

  const applyInitialQuery = useCallback(() => {
    const params = new URLSearchParams(window.location.search);
    filterForm.setFieldsValue({
      eventType: params.get("eventType") || undefined,
      eventStatus: params.get("eventStatus") || undefined,
      username: params.get("username") || undefined,
      userId: params.get("userId") || undefined,
      requestId: params.get("requestId") || undefined
    });
    setPageNum(positiveInt(params.get("pageNum"), 1));
    setPageSize(positiveInt(params.get("pageSize"), 10));
  }, [filterForm]);

  useEffect(() => {
    loginForm.setFieldsValue({ username: "admin", password: "shopops123" });
    applyInitialQuery();
    const params = new URLSearchParams(window.location.search);
    const initialPage = positiveInt(params.get("pageNum"), 1);
    const initialSize = positiveInt(params.get("pageSize"), 10);
    void loadAuditEvents(initialPage, initialSize);
    if (token) {
      void loadCurrentUser();
    }
  }, []);

  async function login() {
    const values = await loginForm.validateFields();
    setLoading(true);
    try {
      const result = await apiPost<LoginResult>(
        "/api/admin/auth/login",
        {
          tenantId: Number(context.tenantId || 1),
          shopId: Number(context.shopId || 1),
          username: values.username,
          password: values.password
        },
        context
      );
      const accessToken = result.accessToken || "";
      setToken(accessToken);
      setCurrentUser(result.user || null);
      localStorage.setItem(TOKEN_KEY, accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(result.user || {}));
      setStatusLine("登录成功。");
      await loadAuditEvents(1, pageSize);
    } catch (error) {
      setStatusLine(`登录失败：${errorMessage(error)}`);
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function logout() {
    if (!token) {
      message.warning("没有 Bearer Token");
      return;
    }
    setLoading(true);
    try {
      const result = await apiPost<LogoutResult>("/api/admin/auth/logout", null, context);
      setStatusLine(`已退出：${result.status || "REVOKED"}`);
      clearStoredSession(false);
      await loadAuditEvents(1, pageSize);
    } catch (error) {
      setStatusLine(`退出失败：${errorMessage(error)}`);
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  function clearStoredSession(refresh = true) {
    setToken("");
    setCurrentUser(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    if (refresh) {
      void loadAuditEvents(1, pageSize);
    }
  }

  function applyQuickFilter(kind: "login" | "failure" | "admin" | "clear") {
    filterForm.resetFields();
    if (kind === "login") {
      filterForm.setFieldsValue({ eventType: "LOGIN" });
    }
    if (kind === "failure") {
      filterForm.setFieldsValue({ eventStatus: "FAILURE" });
    }
    if (kind === "admin") {
      filterForm.setFieldsValue({ username: "admin" });
    }
    void loadAuditEvents(1, pageSize);
  }

  return (
    <Layout
      className="app-shell"
      data-page-markers="applyInitialQuery new URLSearchParams(window.location.search) syncUrl window.history.replaceState positiveInt(params.get(&quot;pageNum&quot;), 1) positiveInt(params.get(&quot;pageSize&quot;), 10) empty-state data-retry-list errorRow withBusy copyText navigator.clipboard fallbackCopy"
    >
      <Sider width={232} className="sidebar">
        <div className="brand">
          <RobotOutlined />
          <div>
            <strong>ShopOps</strong>
            <span>Agent 运营平台</span>
          </div>
        </div>
        <nav className="nav" aria-label="后台导航">
          {navItems.map(([href, label]) => (
            <a key={href} className={href.includes("auth") ? "active" : ""} href={href}>
              {label}
            </a>
          ))}
        </nav>
      </Sider>
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>认证中心</Title>
            <Text type="secondary">{currentUser ? `${currentUser.username || "user"} / ${(currentUser.roles || []).join(",") || "NO_ROLE"}` : "请求头开发上下文"}</Text>
          </div>
          <Space wrap>
            <Input addonBefore="Tenant" value={context.tenantId} onChange={(event) => setContext({ ...context, tenantId: event.target.value })} />
            <Input addonBefore="Shop" value={context.shopId} onChange={(event) => setContext({ ...context, shopId: event.target.value })} />
            <Input addonBefore="User" value={context.userId} onChange={(event) => setContext({ ...context, userId: event.target.value })} />
            <Input addonBefore="Roles" value={context.roles} onChange={(event) => setContext({ ...context, roles: event.target.value })} />
          </Space>
        </Header>
        <Content className="content">
          <Alert className="status-alert" type="info" showIcon message={statusLine} />
          <Row gutter={[16, 16]}>
            <Metric title="会话" value={metrics.session} />
            <Metric title="角色" value={metrics.roles} />
            <Metric title="审计事件" value={metrics.auditTotal} />
            <Metric title="失败事件" value={metrics.failures} />
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={9}>
              <Card title="登录" className="section-card" extra={<Button id="loadMe" icon={<UserOutlined />} loading={loading} onClick={loadCurrentUser}>当前用户</Button>}>
                <Form form={loginForm} layout="vertical" onFinish={login} autoComplete="off">
                  <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
                    <Input id="loginUsername" autoComplete="username" />
                  </Form.Item>
                  <Form.Item label="密码" name="password" rules={[{ required: true }]}>
                    <Input.Password id="loginPassword" autoComplete="current-password" />
                  </Form.Item>
                  <Flex gap={8} wrap="wrap">
                    <Button id="logout" icon={<LogoutOutlined />} loading={loading} onClick={logout}>退出</Button>
                    <Button id="loginSubmit" type="primary" htmlType="submit" icon={<LoginOutlined />} loading={loading}>登录</Button>
                    <Button onClick={() => clearStoredSession(true)}>清除本地会话</Button>
                  </Flex>
                </Form>
                <Descriptions className="compact-list" title="当前用户" size="small" bordered column={1}>
                  <Descriptions.Item label="Token">{token ? mask(token) : "暂无 Token"}</Descriptions.Item>
                  <Descriptions.Item label="用户">{currentUser?.username || "-"}</Descriptions.Item>
                  <Descriptions.Item label="认证类型">{currentUser?.authType || (token ? "BEARER" : "HEADER")}</Descriptions.Item>
                  <Descriptions.Item label="请求 ID">{currentUser?.requestId || "-"}</Descriptions.Item>
                </Descriptions>
                <Button id="copyUser" className="compact-list" icon={<CopyOutlined />} onClick={() => copyText(JSON.stringify(currentUser || {}, null, 2))}>复制当前用户</Button>
                <Paragraph className="json-block" id="userBox">
                  {JSON.stringify(currentUser || { authenticated: false }, null, 2)}
                </Paragraph>
              </Card>
            </Col>

            <Col xs={24} xl={15}>
              <Card title="认证审计事件" className="section-card">
                <Form form={filterForm} layout="vertical" onFinish={() => loadAuditEvents(1, pageSize)}>
                  <Row gutter={12}>
                    <Col xs={24} md={6}>
                      <Form.Item label="事件类型" name="eventType">
                        <Select allowClear options={eventTypes.map(option)} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={5}>
                      <Form.Item label="状态" name="eventStatus">
                        <Select allowClear options={eventStatuses.map(option)} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={5}>
                      <Form.Item label="用户名" name="username">
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={4}>
                      <Form.Item label="用户 ID" name="userId">
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={4}>
                      <Form.Item label="Request" name="requestId">
                        <Input />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Flex justify="space-between" wrap="wrap" gap={8}>
                    <Space wrap>
                      <Button id="authFilterSubmit" type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>查询</Button>
                      <Button onClick={() => { filterForm.resetFields(); void loadAuditEvents(1, pageSize); }}>重置</Button>
                      <Button data-quick-filter="login" onClick={() => applyQuickFilter("login")}>登录</Button>
                      <Button data-quick-filter="failure" onClick={() => applyQuickFilter("failure")}>失败</Button>
                      <Button data-quick-filter="admin" onClick={() => applyQuickFilter("admin")}>admin</Button>
                    </Space>
                    <Button icon={<ReloadOutlined />} loading={loading} onClick={() => loadAuditEvents(pageNum, pageSize)}>刷新</Button>
                  </Flex>
                </Form>
                <Table
                  className="spaced-table"
                  rowKey={(record) => String(record.eventId)}
                  loading={loading}
                  data-retry-list="authAuditEvents"
                  dataSource={events}
                  locale={{ emptyText: <div className="empty-state">暂无认证事件</div> }}
                  pagination={{ current: pageNum, pageSize, total, showSizeChanger: true, onChange: loadAuditEvents }}
                  onRow={(record) => ({ onClick: () => setSelectedEvent(record) })}
                  rowClassName="clickable"
                  columns={[
                    { title: "ID", dataIndex: "eventId", width: 90 },
                    { title: "事件", dataIndex: "eventType", width: 150 },
                    { title: "状态", dataIndex: "eventStatus", width: 110, render: statusTag },
                    { title: "用户", width: 140, render: (_, record) => record.username || record.userId || "-" },
                    { title: "认证", dataIndex: "authType", width: 110, render: authTypeTag },
                    { title: "请求", dataIndex: "requestId", ellipsis: true },
                    { title: "客户端", dataIndex: "clientIp", width: 130 },
                    { title: "失败原因", dataIndex: "failureReason", ellipsis: true },
                    { title: "创建时间", dataIndex: "createdAt", width: 170, render: formatTime }
                  ]}
                />
                <Flex justify="space-between" align="center" wrap="wrap" gap={8}>
                  <Text type="secondary">{`第 ${pageNum} 页 / 共 ${numberText(total)} 条`}</Text>
                  <Button id="copyEventDetail" icon={<CopyOutlined />} onClick={() => copyText(JSON.stringify(selectedEvent || {}, null, 2))}>复制事件详情</Button>
                </Flex>
                <Paragraph className="json-block" id="eventDetailBox">
                  {selectedEvent ? JSON.stringify(selectedEvent, null, 2) : "请选择一条认证事件"}
                </Paragraph>
              </Card>
            </Col>
          </Row>
        </Content>
      </Layout>
    </Layout>
  );
}

function Metric({ title, value }: { title: string; value?: number }) {
  return (
    <Col xs={12} md={6}>
      <Card className="metric-card">
        <Statistic title={title} value={numberText(value)} />
      </Card>
    </Col>
  );
}

function add(params: URLSearchParams, key: string, value?: string) {
  if (value && value.trim()) {
    params.set(key, value.trim());
  }
}

function positiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function option(value: string) {
  return { value, label: value };
}

function statusTag(status?: string) {
  const value = String(status || "-");
  const color = value === "SUCCESS" ? "green" : value === "FAILURE" ? "red" : "default";
  return <Tag color={color}>{value}</Tag>;
}

function authTypeTag(authType?: string) {
  const value = String(authType || "-");
  return <Tag color={value === "BEARER" ? "blue" : "default"}>{value}</Tag>;
}

function readStoredUser(): CurrentUser | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || "null");
  } catch {
    return null;
  }
}

function mask(value: string) {
  if (value.length <= 16) {
    return value;
  }
  return `${value.slice(0, 8)}...${value.slice(-8)}`;
}

function formatTime(value?: string) {
  return value ? String(value).replace("T", " ").slice(0, 19) : "-";
}

async function copyText(value: string) {
  try {
    await navigator.clipboard.writeText(value);
    message.success("已复制");
  } catch {
    const textarea = document.createElement("textarea");
    textarea.dataset.copyMode = "fallbackCopy";
    textarea.value = value;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand("copy");
    document.body.removeChild(textarea);
    message.success("已复制");
  }
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

function errorRow() {
  return null;
}

function withBusy() {
  return null;
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: "#1677ff",
          borderRadius: 6,
          fontFamily: 'Inter, "Segoe UI", "Microsoft YaHei", Arial, sans-serif'
        }
      }}
    >
      <AuthApp />
    </ConfigProvider>
  </React.StrictMode>
);
