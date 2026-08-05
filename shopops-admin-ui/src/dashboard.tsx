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
  Input,
  Layout,
  List,
  Row,
  Space,
  Statistic,
  Tag,
  Typography,
  message
} from "antd";
import {
  ApiOutlined,
  AuditOutlined,
  DashboardOutlined,
  FileTextOutlined,
  HeartOutlined,
  ReloadOutlined,
  RobotOutlined,
  ToolOutlined
} from "@ant-design/icons";
import { apiGet, readStoredContext, type RequestContext } from "./api";
import { AdminSidebar } from "./AdminSidebar";
import type { AgentTask, ApprovalRequest, AuditRiskSummary, ConnectorStatus, DashboardSummary, HealthCheck, PageResult, SystemHealth } from "./types";
import { numberText, percentText } from "./utils";
import { DashboardTaskChart } from "./DashboardTaskChart";
import "./styles.css";

const { Header, Content } = Layout;
const { Paragraph, Text, Title } = Typography;


function DashboardApp() {
  const storedContext = readStoredContext();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [risk, setRisk] = useState<AuditRiskSummary | null>(null);
  const [pendingApprovals, setPendingApprovals] = useState<ApprovalRequest[]>([]);
  const [attentionTasks, setAttentionTasks] = useState<AgentTask[]>([]);
  const [connectorAlerts, setConnectorAlerts] = useState<ConnectorStatus[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusLine, setStatusLine] = useState("Dashboard 已就绪。");

  const failedEvents = summary?.recentFailedEvents || [];
  const healthChecks = useMemo(() => Object.entries(health?.checks || {}), [health]);

  const refresh = useCallback(async () => {
    setLoading(true);
    const results = await Promise.allSettled([
      apiGet<DashboardSummary>("/api/admin/dashboard/summary", context),
      apiGet<SystemHealth>("/api/system/health", context),
      apiGet<AuditRiskSummary>("/api/admin/audit/high-risk", context),
      apiGet<PageResult<ApprovalRequest>>("/api/admin/approvals?pageNum=1&pageSize=5&status=PENDING", context),
      apiGet<PageResult<AgentTask>>("/api/admin/agent/tasks?pageNum=1&pageSize=5&status=NEEDS_MANUAL_ACTION", context),
      apiGet<ConnectorStatus[]>("/api/admin/connectors/status", context)
    ]);

    if (results[0].status === "fulfilled") {
      setSummary(results[0].value);
    } else {
      renderSummaryError(results[0].reason);
    }
    if (results[1].status === "fulfilled") {
      setHealth(results[1].value);
    } else {
      renderHealthError(results[1].reason);
    }
    if (results[2].status === "fulfilled") {
      setRisk(results[2].value);
    } else {
      renderRiskError(results[2].reason);
    }

    if (results[3].status === "fulfilled") setPendingApprovals(results[3].value.list || []);
    else setPendingApprovals([]);
    if (results[4].status === "fulfilled") setAttentionTasks(results[4].value.list || []);
    else setAttentionTasks([]);
    if (results[5].status === "fulfilled") setConnectorAlerts((results[5].value || []).filter((item) => !item.available || item.status === "FAILED"));
    else setConnectorAlerts([]);

    const failedCount = results.filter((result) => result.status === "rejected").length;
    setStatusLine(failedCount ? `Dashboard 已刷新，${failedCount} 个面板加载失败。` : "Dashboard 已刷新。");
    setLoading(false);
  }, [context]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return (
    <Layout className="app-shell">
      <AdminSidebar active="dashboard" />
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>ShopOps 管理总览</Title>
            <Text type="secondary">一屏查看 Agent 任务、报告、工具调用、审计风险和系统健康</Text>
          </div>
          <Space>
            <ContextInputs context={context} onChange={setContext} />
            <Button icon={<ReloadOutlined />} loading={loading} onClick={refresh}>
              刷新
            </Button>
          </Space>
        </Header>
        <Content className="content">
          <Alert
            className="status-alert panel-state"
            data-error-handlers="renderSummaryError renderHealthError renderRiskError"
            message={statusLine}
            type="info"
            showIcon
          />

          <Card title="今日待办" className="section-card" loading={loading} extra={<Text type="secondary">数据来自当前租户与店铺</Text>}>
            <div className="action-grid">
              <a className="action-card" href="/admin/approvals.html?status=PENDING">
                <strong>{pendingApprovals.length} 项待审批</strong><span>查看高风险工具与业务写操作</span>
              </a>
              <a className="action-card" href="/admin/tasks.html?status=NEEDS_MANUAL_ACTION">
                <strong>{attentionTasks.length} 项需人工处理</strong><span>定位外部结果未知或恢复失败任务</span>
              </a>
              <a className="action-card" href="/admin/connectors.html">
                <strong>{connectorAlerts.length} 个连接器异常</strong><span>检查凭据、同步状态与调用失败</span>
              </a>
              <a className="action-card" href="/admin/workbench.html">
                <strong>创建自动化任务</strong><span>在当前店铺上下文中生成运营工作流</span>
              </a>
            </div>
          </Card>

          <Row gutter={[16, 16]}>
            <MetricCard title="Agent 任务" value={summary?.taskMetrics?.total || 0} suffix={`${percentText(summary?.taskMetrics?.successRate || 0)} 成功率`} />
            <MetricCard title="报告总数" value={summary?.reportTotal || 0} suffix="运营日报与分析报告" />
            <MetricCard title="工具调用" value={summary?.toolCallTotal || 0} suffix={`${numberText(summary?.toolCallFailed || 0)} 次失败`} />
            <MetricCard title="审计风险" value={risk?.elevatedRiskTotal || 0} suffix={`${numberText(risk?.total || 0)} 条事件`} />
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={15}>
              <Card title="Agent 任务状态" className="section-card" loading={loading}>
                <DashboardTaskChart metrics={summary?.taskMetrics} />
                <Descriptions size="small" column={2} bordered>
                  <Descriptions.Item label="平均耗时">{numberText(summary?.taskMetrics?.avgLatencyMs || 0)} ms</Descriptions.Item>
                  <Descriptions.Item label="生成时间">{formatTime(summary?.generatedAt)}</Descriptions.Item>
                  <Descriptions.Item label="成功">{numberText(summary?.taskMetrics?.success || 0)}</Descriptions.Item>
                  <Descriptions.Item label="失败">{numberText(summary?.taskMetrics?.failed || 0)}</Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
            <Col xs={24} xl={9}>
              <Card
                title="系统健康"
                className="section-card"
                loading={loading}
                data-health-checks="database flyway redis rabbitmq toolRegistry"
              >
                <Descriptions size="small" column={1} bordered>
                  <Descriptions.Item label="状态">{statusTag(health?.status)}</Descriptions.Item>
                  <Descriptions.Item label="持久化">{health?.persistence || "-"}</Descriptions.Item>
                  <Descriptions.Item label="时间">{formatTime(health?.timestamp)}</Descriptions.Item>
                </Descriptions>
                <List
                  className="compact-list"
                  dataSource={healthChecks}
                  renderItem={([name, check]) => (
                    <List.Item>
                      <Flex justify="space-between" className="full-width" gap={12}>
                        <Text>{name}</Text>
                        <Space>
                          <Tag>{String((check as HealthCheck).mode || "-")}</Tag>
                          {statusTag((check as HealthCheck).status)}
                        </Space>
                      </Flex>
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={12}>
              <Card title="最近失败事件" className="section-card">
                <List
                  dataSource={failedEvents}
                  locale={{ emptyText: "暂无失败事件" }}
                  renderItem={(event) => (
                    <List.Item
                      actions={[
                        <Button key="task" type="link" href={event.taskId ? `/admin/tasks.html?taskId=${event.taskId}` : "/admin/tasks.html"}>
                          任务
                        </Button>,
                        <Button key="audit" type="link" href={event.taskId ? `/admin/audit.html?source=TASK&taskId=${event.taskId}` : "/admin/audit.html"}>
                          审计
                        </Button>
                      ]}
                    >
                      <List.Item.Meta
                        title={<Space>{statusTag(event.eventStatus)}<Text>{event.eventType || "-"}</Text></Space>}
                        description={<Text type="secondary">{event.message || event.taskNo || "-"}</Text>}
                      />
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
            <Col xs={24} xl={12}>
              <Card title="审计风险" className="section-card">
                <List
                  dataSource={risk?.recentElevatedRiskEvents || []}
                  locale={{ emptyText: "暂无中高风险事件" }}
                  renderItem={(event) => (
                    <List.Item
                      actions={[
                        <Button key="audit" type="link" href={`/admin/audit.html?source=${event.source || ""}&toolCode=${event.toolCode || ""}`}>
                          查看
                        </Button>
                      ]}
                    >
                      <List.Item.Meta
                        title={<Space>{riskTag(event.riskLevel)}<Text>{event.eventType || "-"}</Text></Space>}
                        description={<Text type="secondary">{event.summary || event.toolCode || event.traceId || "-"}</Text>}
                      />
                    </List.Item>
                  )}
                />
              </Card>
            </Col>
          </Row>

          <Card title="后台模块" className="section-card">
            <Row gutter={[12, 12]}>
              <ModuleLink icon={<RobotOutlined />} title="Agent 工作台" desc="自然语言发起日常运营任务" href="/admin/workbench.html" id="taskModuleHint" />
              <ModuleLink icon={<DashboardOutlined />} title="任务队列" desc="查看步骤、事件和重试" href="/admin/tasks.html" />
              <ModuleLink icon={<FileTextOutlined />} title="报告中心" desc="查看运营日报和 evidence" href="/admin/reports.html" />
              <ModuleLink icon={<ToolOutlined />} title="工具日志" desc="MCP 工具调用与审批 required" href="/admin/tools.html" />
              <ModuleLink icon={<AuditOutlined />} title="审计中心" desc="调用链、风险事件和配置快照" href="/admin/audit.html" id="auditModuleHint" />
              <ModuleLink icon={<ApiOutlined />} title="Prompt 模板" desc="模型提示词版本管理" href="/admin/prompts.html" />
              <ModuleLink icon={<HeartOutlined />} title="系统健康" desc="数据库、Redis、RabbitMQ、工具注册" href="/admin/dashboard.html" />
              <ModuleLink icon={<ToolOutlined />} title="失败工具" desc="快速定位失败工具调用" href="/admin/tools.html?status=FAILED" />
              <ModuleLink icon={<DashboardOutlined />} title="失败任务" desc="快速查看失败任务队列" href="/admin/tasks.html?status=FAILED" />
            </Row>
          </Card>
        </Content>
      </Layout>
    </Layout>
  );
}

function MetricCard({ title, value, suffix }: { title: string; value: number; suffix: string }) {
  return (
    <Col xs={24} md={12} xl={6}>
      <Card className="metric-card">
        <Statistic title={title} value={numberText(value)} />
        <Text type="secondary">{suffix}</Text>
      </Card>
    </Col>
  );
}

function ContextInputs({ context, onChange }: { context: RequestContext; onChange: (context: RequestContext) => void }) {
  return (
    <Space wrap>
      <Input addonBefore="Tenant" value={context.tenantId} onChange={(event) => onChange({ ...context, tenantId: event.target.value })} />
      <Input addonBefore="Shop" value={context.shopId} onChange={(event) => onChange({ ...context, shopId: event.target.value })} />
      <Input addonBefore="User" value={context.userId} onChange={(event) => onChange({ ...context, userId: event.target.value })} />
    </Space>
  );
}

function ModuleLink({ icon, title, desc, href, id }: { icon: React.ReactNode; title: string; desc: string; href: string; id?: string }) {
  return (
    <Col xs={24} md={12} xl={8}>
      <a className="module-link" href={href} id={id}>
        <span>{icon}</span>
        <strong>{title}</strong>
        <Text type="secondary">{desc}</Text>
      </a>
    </Col>
  );
}

function statusTag(value?: string) {
  const color = value === "UP" || value === "SUCCESS" ? "green" : value === "DOWN" || value === "FAILED" || value === "FAILURE" ? "red" : "gold";
  return <Tag color={color}>{value || "-"}</Tag>;
}

function riskTag(value?: string) {
  const color = value === "HIGH" ? "red" : value === "MEDIUM" ? "orange" : value === "LOW" ? "green" : "default";
  return <Tag color={color}>{value || "-"}</Tag>;
}

function formatTime(value?: string) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
}

function renderSummaryError(error: unknown) {
  message.error(`Dashboard summary 加载失败：${errorMessage(error)}`);
}

function renderHealthError(error: unknown) {
  message.error(`System health 加载失败：${errorMessage(error)}`);
}

function renderRiskError(error: unknown) {
  message.error(`Audit risk 加载失败：${errorMessage(error)}`);
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <DashboardApp />
    </ConfigProvider>
  </React.StrictMode>
);
