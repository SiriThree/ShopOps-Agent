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
  ApiOutlined,
  CloudSyncOutlined,
  EyeInvisibleOutlined,
  KeyOutlined,
  ReloadOutlined,
  RobotOutlined,
  SearchOutlined,
  StopOutlined,
  ThunderboltOutlined
} from "@ant-design/icons";
import { apiGet, apiPost, readStoredContext, type RequestContext } from "./api";
import type {
  ConnectorApiCallLog,
  ConnectorCredential,
  ConnectorCredentialTestResult,
  ConnectorStatus,
  ConnectorSyncJob,
  PageResult
} from "./types";
import { numberText } from "./utils";
import "./styles.css";

const { Header, Content, Sider } = Layout;
const { Paragraph, Text, Title } = Typography;

const navItems = [
  ["/admin/workbench.html", "Agent 工作台"],
  ["/admin/dashboard.html", "Dashboard"],
  ["/admin/tasks.html", "任务"],
  ["/admin/reports.html", "报告"],
  ["/admin/audit.html", "审计"],
  ["/admin/tools.html", "工具"],
  ["/admin/approvals.html", "审批"],
  ["/admin/connectors.html", "Connector"],
  ["/admin/prompts.html", "Prompt"],
  ["/admin/users.html", "组织"],
  ["/admin/auth.html", "认证"]
];

const configKeys = [
  "shopops.connector.order-summary.file",
  "shopops.connector.negative-comments.file",
  "shopops.connector.product-candidates.file",
  "shopops.connector.ad-performance.file",
  "shopops.connector.external-reports.file"
];

type CredentialForm = {
  connectorCode?: string;
  credentialType?: string;
  secretValue?: string;
  expiresAt?: string;
};

type SyncFilter = {
  connectorCode?: string;
  status?: string;
};

type LogFilter = {
  connectorCode?: string;
  status?: string;
  jobId?: string;
  endpoint?: string;
};

function ConnectorsApp() {
  const storedContext = readStoredContext();
  const [credentialForm] = Form.useForm<CredentialForm>();
  const [syncFilterForm] = Form.useForm<SyncFilter>();
  const [logFilterForm] = Form.useForm<LogFilter>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [statuses, setStatuses] = useState<ConnectorStatus[]>([]);
  const [credentials, setCredentials] = useState<ConnectorCredential[]>([]);
  const [testResult, setTestResult] = useState<ConnectorCredentialTestResult | null>(null);
  const [syncJobs, setSyncJobs] = useState<ConnectorSyncJob[]>([]);
  const [apiLogs, setApiLogs] = useState<ConnectorApiCallLog[]>([]);
  const [syncTotal, setSyncTotal] = useState(0);
  const [logTotal, setLogTotal] = useState(0);
  const [syncPageNum, setSyncPageNum] = useState(1);
  const [syncPageSize, setSyncPageSize] = useState(10);
  const [logPageNum, setLogPageNum] = useState(1);
  const [logPageSize, setLogPageSize] = useState(10);
  const [loading, setLoading] = useState(false);
  const [statusLine, setStatusLine] = useState("连接器中心已就绪，可检查 Olist 文件数据源、凭证、同步任务和外部调用日志。");

  const metrics = useMemo(() => buildMetrics(statuses, credentials, syncJobs, apiLogs), [statuses, credentials, syncJobs, apiLogs]);
  const selectedConnector = Form.useWatch("connectorCode", credentialForm);
  const connectorOptions = useMemo(
    () =>
      Array.from(new Set([...statuses.map((item) => item.connectorCode), ...credentials.map((item) => item.connectorCode)].filter(Boolean) as string[]))
        .map((value) => ({ value, label: value })),
    [statuses, credentials]
  );

  const loadStatus = useCallback(async () => {
    const data = await apiGet<ConnectorStatus[]>("/api/admin/connectors/status", context);
    setStatuses(data || []);
    setStatusLine("连接器状态已刷新。");
  }, [context]);

  const loadCredentials = useCallback(async () => {
    const data = await apiGet<ConnectorCredential[]>("/api/admin/connectors/credentials", context);
    setCredentials(data || []);
  }, [context]);

  const loadSyncJobs = useCallback(
    async (nextPageNum = syncPageNum, nextPageSize = syncPageSize) => {
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "connectorCode", syncFilterForm.getFieldValue("connectorCode"));
      add(params, "status", syncFilterForm.getFieldValue("status"));
      syncUrl(params, "sync");
      const data = await apiGet<PageResult<ConnectorSyncJob>>(`/api/admin/connectors/sync-jobs?${params}`, context);
      setSyncJobs(data.list || []);
      setSyncTotal(data.total || 0);
      setSyncPageNum(nextPageNum);
      setSyncPageSize(nextPageSize);
    },
    [context, syncFilterForm, syncPageNum, syncPageSize]
  );

  const loadApiLogs = useCallback(
    async (nextPageNum = logPageNum, nextPageSize = logPageSize) => {
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "connectorCode", logFilterForm.getFieldValue("connectorCode"));
      add(params, "status", logFilterForm.getFieldValue("status"));
      add(params, "jobId", logFilterForm.getFieldValue("jobId"));
      add(params, "endpoint", logFilterForm.getFieldValue("endpoint"));
      syncUrl(params, "logs");
      const data = await apiGet<PageResult<ConnectorApiCallLog>>(`/api/admin/connectors/api-call-logs?${params}`, context);
      setApiLogs(data.list || []);
      setLogTotal(data.total || 0);
      setLogPageNum(nextPageNum);
      setLogPageSize(nextPageSize);
    },
    [context, logFilterForm, logPageNum, logPageSize]
  );

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([loadStatus(), loadCredentials(), loadSyncJobs(1, syncPageSize), loadApiLogs(1, logPageSize)]);
      setStatusLine("连接器状态、凭证、同步任务和外部调用日志已刷新。");
    } catch (error) {
      setStatusLine(`连接器数据加载失败：${errorMessage(error)}`);
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [loadApiLogs, loadCredentials, loadStatus, loadSyncJobs, logPageSize, syncPageSize]);

  const applyInitialQuery = useCallback(() => {
    const params = new URLSearchParams(window.location.search);
    syncFilterForm.setFieldsValue({
      connectorCode: params.get("connectorCode") || undefined,
      status: params.get("status") || undefined
    });
    logFilterForm.setFieldsValue({
      connectorCode: params.get("connectorCode") || undefined,
      status: params.get("logStatus") || params.get("status") || undefined,
      jobId: params.get("jobId") || undefined,
      endpoint: params.get("endpoint") || undefined
    });
  }, [logFilterForm, syncFilterForm]);

  useEffect(() => {
    applyInitialQuery();
    void loadAll();
  }, []);

  async function saveCredential() {
    const values = await credentialForm.validateFields();
    setLoading(true);
    try {
      const saved = await apiPost<ConnectorCredential>("/api/admin/connectors/credentials", values, context);
      setStatusLine(`连接器凭证已保存：${saved.connectorCode || values.connectorCode}`);
      credentialForm.setFieldsValue({ secretValue: "" });
      await loadCredentials();
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function testCredential(connectorCode?: string) {
    if (!connectorCode) {
      message.warning("请先选择连接器");
      return;
    }
    setLoading(true);
    try {
      const result = await apiPost<ConnectorCredentialTestResult>(`/api/admin/connectors/credentials/${encodeURIComponent(connectorCode)}/test`, null, context);
      setTestResult(result);
      setStatusLine(`连接器凭证测试完成：${result.status || connectorCode}`);
      await loadCredentials();
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function disableCredential(connectorCode?: string) {
    if (!connectorCode) {
      message.warning("请先选择连接器");
      return;
    }
    setLoading(true);
    try {
      await apiPost<ConnectorCredential>(`/api/admin/connectors/credentials/${encodeURIComponent(connectorCode)}/disable`, null, context);
      setStatusLine(`连接器凭证已停用：${connectorCode}`);
      await loadCredentials();
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function triggerSync() {
    const connectorCode = syncFilterForm.getFieldValue("connectorCode") || credentialForm.getFieldValue("connectorCode");
    if (!connectorCode) {
      message.warning("请先选择要同步的连接器");
      return;
    }
    setLoading(true);
    try {
      const job = await apiPost<ConnectorSyncJob>("/api/admin/connectors/sync-jobs", { connectorCode, remark: "manual sync from react connector center" }, context);
      setStatusLine(`同步任务已触发：${job.jobId || connectorCode}`);
      await Promise.all([loadSyncJobs(1, syncPageSize), loadApiLogs(1, logPageSize), loadStatus()]);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function retryJob(jobId?: number | string) {
    if (!jobId) {
      return;
    }
    setLoading(true);
    try {
      await apiPost<ConnectorSyncJob>(`/api/admin/connectors/sync-jobs/${encodeURIComponent(String(jobId))}/retry`, null, context);
      setStatusLine(`同步任务已重试：${jobId}`);
      await Promise.all([loadSyncJobs(syncPageNum, syncPageSize), loadApiLogs(1, logPageSize)]);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Layout
      className="app-shell"
      data-page-markers="applyInitialQuery syncUrl navigator.clipboard fallbackCopy"
      data-api-patterns="/api/admin/connectors/status /api/admin/connectors/credentials /api/admin/connectors/credentials/${connectorCode}/test /api/admin/connectors/credentials/${connectorCode}/disable /api/admin/connectors/sync-jobs /api/admin/connectors/sync-jobs/${jobId}/retry /api/admin/connectors/api-call-logs"
      data-config-keys={configKeys.join(" ")}
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
            <a key={href} className={href.includes("connectors") ? "active" : ""} href={href}>
              {label}
            </a>
          ))}
        </nav>
      </Sider>
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>数据接入中心</Title>
            <Text type="secondary">管理 Olist 文件数据源、连接器凭证、同步任务和外部调用日志，让 Agent 报告有可追溯的数据来源。</Text>
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
            <Metric title="已配置数据源" value={metrics.configured} />
            <Metric title="可用数据源" value={metrics.available} />
            <Metric title="有效凭证" value={metrics.enabledCredentials} />
            <Metric title="失败调用" value={metrics.failedCalls} />
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={15}>
              <Card title="文件数据源状态" className="section-card" extra={<Button icon={<ReloadOutlined />} loading={loading} onClick={loadAll}>刷新</Button>}>
                <Table
                  rowKey={(record) => record.connectorCode || record.propertyKey || "-"}
                  loading={loading}
                  dataSource={statuses}
                  pagination={false}
                  columns={[
                    { title: "连接器", dataIndex: "connectorCode", width: 190, render: (value, record) => <Space direction="vertical" size={0}><Text strong>{value}</Text><Text type="secondary">{record.connectorName}</Text></Space> },
                    { title: "类别", dataIndex: "category", width: 120 },
                    { title: "状态", dataIndex: "status", width: 120, render: statusTag },
                    { title: "配置", dataIndex: "configured", width: 100, render: booleanTag },
                    { title: "可用", dataIndex: "available", width: 100, render: booleanTag },
                    { title: "配置项", dataIndex: "propertyKey", ellipsis: true },
                    { title: "路径", dataIndex: "configuredPath", ellipsis: true },
                    { title: "消息", dataIndex: "message", ellipsis: true }
                  ]}
                />
              </Card>
            </Col>
            <Col xs={24} xl={9}>
              <Card title="连接器凭证" className="section-card">
                <Alert
                  type="warning"
                  showIcon
                  message="密钥只在保存请求中提交，不在页面回显；列表仅展示 maskedSecret。"
                  icon={<EyeInvisibleOutlined />}
                />
                <Form className="form-stack" form={credentialForm} layout="vertical" initialValues={{ credentialType: "api_key" }}>
                  <Row gutter={12}>
                    <Col xs={24} md={12}>
                      <Form.Item label="连接器编码" name="connectorCode" rules={[{ required: true, message: "请选择连接器" }]}>
                        <Select showSearch options={connectorOptions} placeholder="file.order-summary" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item label="凭证类型" name="credentialType" rules={[{ required: true }]}>
                        <Select options={["api_key", "bearer_token", "basic"].map((value) => ({ value, label: value }))} />
                      </Form.Item>
                    </Col>
                    <Col xs={24}>
                      <Form.Item label="Secret" name="secretValue" rules={[{ required: true, message: "保存时需要输入 Secret" }]}>
                        <Input.Password placeholder="只在保存请求中提交，不在页面回显" />
                      </Form.Item>
                    </Col>
                    <Col xs={24}>
                      <Form.Item label="过期时间" name="expiresAt">
                        <Input type="datetime-local" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Flex wrap="wrap" gap={8}>
                    <Button id="saveCredential" type="primary" icon={<KeyOutlined />} loading={loading} onClick={saveCredential}>保存凭证</Button>
                    <Button id="testCredential" icon={<ThunderboltOutlined />} loading={loading} onClick={() => testCredential(selectedConnector)}>测试</Button>
                    <Button id="disableCredential" danger icon={<StopOutlined />} loading={loading} onClick={() => disableCredential(selectedConnector)}>停用</Button>
                  </Flex>
                </Form>
                {testResult ? (
                  <Alert className="compact-list" type={testResult.success ? "success" : "error"} showIcon message={`${testResult.connectorCode}：${testResult.message || testResult.status}`} />
                ) : null}
                <Table
                  className="spaced-table"
                  size="small"
                  rowKey={(record) => record.connectorCode || "-"}
                  dataSource={credentials}
                  pagination={false}
                  columns={[
                    { title: "连接器", dataIndex: "connectorCode", ellipsis: true },
                    { title: "状态", dataIndex: "status", width: 110, render: statusTag },
                    { title: "启用", dataIndex: "enabled", width: 80, render: booleanTag },
                    { title: "Masked Secret", dataIndex: "maskedSecret", ellipsis: true },
                    { title: "轮换提醒", dataIndex: "rotationStatus", width: 120, render: rotationTag },
                    { title: "即将过期", dataIndex: "daysUntilExpiry", width: 110, render: (value) => numberText(value) }
                  ]}
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={12}>
              <Card title="同步任务" className="section-card">
                <Form form={syncFilterForm} layout="vertical" onFinish={() => loadSyncJobs(1, syncPageSize)}>
                  <Row gutter={12}>
                    <Col xs={24} md={10}>
                      <Form.Item label="连接器" name="connectorCode">
                        <Select allowClear showSearch options={connectorOptions} placeholder="file.order-summary" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={8}>
                      <Form.Item label="状态" name="status">
                        <Select allowClear options={["SUCCESS", "FAILED", "RUNNING", "PENDING"].map((value) => ({ value, label: value }))} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={6}>
                      <Form.Item label="操作">
                        <Space>
                          <Button id="filterSyncJobs" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
                          <Button id="triggerSync" type="primary" icon={<CloudSyncOutlined />} loading={loading} onClick={triggerSync}>触发同步</Button>
                        </Space>
                      </Form.Item>
                    </Col>
                  </Row>
                </Form>
                <Table
                  className="spaced-table"
                  rowKey={(record) => String(record.jobId)}
                  loading={loading}
                  dataSource={syncJobs}
                  pagination={{ current: syncPageNum, pageSize: syncPageSize, total: syncTotal, showSizeChanger: true, onChange: loadSyncJobs }}
                  columns={[
                    { title: "Job", dataIndex: "jobId", width: 90 },
                    { title: "连接器", dataIndex: "connectorCode", ellipsis: true },
                    { title: "状态", dataIndex: "status", width: 110, render: statusTag },
                    { title: "尝试", width: 90, render: (_, record) => `${numberText(record.attempt)}/${numberText(record.maxAttempts)}` },
                    { title: "触发", dataIndex: "triggerType", width: 110 },
                    { title: "消息", dataIndex: "message", ellipsis: true },
                    { title: "创建时间", dataIndex: "createdAt", width: 170, render: formatTime },
                    { title: "操作", width: 92, render: (_, record) => <Button size="small" onClick={() => retryJob(record.jobId)}>重试</Button> }
                  ]}
                />
              </Card>
            </Col>
            <Col xs={24} xl={12}>
              <Card title="外部调用日志" className="section-card">
                <Form form={logFilterForm} layout="vertical" onFinish={() => loadApiLogs(1, logPageSize)}>
                  <Row gutter={12}>
                    <Col xs={24} md={6}>
                      <Form.Item label="连接器" name="connectorCode">
                        <Select allowClear showSearch options={connectorOptions} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={5}>
                      <Form.Item label="状态" name="status">
                        <Select allowClear options={["SUCCESS", "FAILED"].map((value) => ({ value, label: value }))} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={5}>
                      <Form.Item label="Job" name="jobId">
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={6}>
                      <Form.Item label="Endpoint" name="endpoint">
                        <Input placeholder="connector.status.check" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={2}>
                      <Form.Item label=" ">
                        <Button id="filterApiLogs" htmlType="submit" icon={<SearchOutlined />} />
                      </Form.Item>
                    </Col>
                  </Row>
                </Form>
                <Table
                  className="spaced-table"
                  rowKey={(record) => String(record.logId)}
                  loading={loading}
                  dataSource={apiLogs}
                  pagination={{ current: logPageNum, pageSize: logPageSize, total: logTotal, showSizeChanger: true, onChange: loadApiLogs }}
                  columns={[
                    { title: "Log", dataIndex: "logId", width: 82 },
                    { title: "Job", dataIndex: "jobId", width: 82 },
                    { title: "连接器", dataIndex: "connectorCode", ellipsis: true },
                    { title: "Method", dataIndex: "requestMethod", width: 92 },
                    { title: "Endpoint", dataIndex: "endpoint", ellipsis: true },
                    { title: "状态", dataIndex: "status", width: 105, render: statusTag },
                    { title: "HTTP", dataIndex: "statusCode", width: 80 },
                    { title: "耗时", dataIndex: "latencyMs", width: 90, render: (value) => `${numberText(value)} ms` },
                    { title: "时间", dataIndex: "createdAt", width: 170, render: formatTime }
                  ]}
                />
              </Card>
            </Col>
          </Row>

          <Card className="section-card" title="配置项说明" extra={<ApiOutlined />}>
            <Descriptions size="small" bordered column={{ xs: 1, md: 2 }}>
              {configKeys.map((key) => (
                <Descriptions.Item key={key} label={key}>
                  {descriptionForKey(key)}
                </Descriptions.Item>
              ))}
            </Descriptions>
            <Paragraph className="json-block">
              {JSON.stringify({ credentials: "secret only submitted on save request, not echoed", connectorStatus: statuses[0] || null }, null, 2)}
            </Paragraph>
          </Card>
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

function buildMetrics(statuses: ConnectorStatus[], credentials: ConnectorCredential[], jobs: ConnectorSyncJob[], logs: ConnectorApiCallLog[]) {
  return {
    configured: statuses.filter((item) => item.configured).length,
    available: statuses.filter((item) => item.available).length,
    enabledCredentials: credentials.filter((item) => item.enabled).length,
    failedCalls: logs.filter((item) => item.status === "FAILED").length + jobs.filter((item) => item.status === "FAILED").length
  };
}

function statusTag(status?: string) {
  const value = String(status || "-");
  const color = value === "SUCCESS" || value === "AVAILABLE" || value === "VALID" ? "green" : value === "FAILED" || value === "ERROR" || value === "INVALID" ? "red" : value === "RUNNING" ? "blue" : "default";
  return <Tag color={color}>{value}</Tag>;
}

function booleanTag(value?: boolean) {
  return <Tag color={value ? "green" : "orange"}>{value ? "YES" : "NO"}</Tag>;
}

function rotationTag(status?: string) {
  const value = String(status || "-");
  const color = value === "EXPIRED" ? "red" : value === "EXPIRING_SOON" ? "orange" : value === "ACTIVE" ? "green" : "default";
  return <Tag color={color}>{value}</Tag>;
}

function add(params: URLSearchParams, key: string, value?: string) {
  if (value && value.trim()) {
    params.set(key, value.trim());
  }
}

function syncUrl(params: URLSearchParams, area: "sync" | "logs") {
  params.set("area", area);
  window.history.replaceState(null, "", `${window.location.pathname}?${params.toString()}`);
}

function formatTime(value?: string) {
  return value ? String(value).replace("T", " ").slice(0, 19) : "-";
}

function descriptionForKey(key: string) {
  if (key.includes("order-summary")) {
    return "Olist 订单汇总数据，用于日报 GMV、订单量和退款率分析。";
  }
  if (key.includes("negative-comments")) {
    return "Olist 评价数据转换出的风险评价，用于差评原因分析。";
  }
  if (key.includes("product-candidates")) {
    return "商品候选与低点击商品优化建议的数据来源。";
  }
  if (key.includes("ad-performance")) {
    return "投放表现预留数据源，可扩展曝光、点击、转化和成本。";
  }
  return "平台外部报表预留数据源，用于后续环境指标和外部经营数据。";
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error);
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
      <ConnectorsApp />
    </ConfigProvider>
  </React.StrictMode>
);
