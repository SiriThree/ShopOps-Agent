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
  AuditOutlined,
  CopyOutlined,
  DownloadOutlined,
  FileTextOutlined,
  ReloadOutlined,
  SearchOutlined,
  ToolOutlined
} from "@ant-design/icons";
import { apiGet, buildHeaders, readStoredContext, type RequestContext } from "./api";
import { AdminSidebar } from "./AdminSidebar";
import type { AuditOverview, AuditRiskSummary, AuditTimelineDetail, AuditTimelineEvent, PageResult } from "./types";
import { numberText } from "./utils";
import { AuditRiskChart } from "./AuditRiskChart";
import "./styles.css";

const { Header, Content } = Layout;
const { Paragraph, Text, Title } = Typography;


type AuditFilters = {
  source?: string;
  eventType?: string;
  eventStatus?: string;
  taskId?: string;
  traceId?: string;
  toolCode?: string;
  riskLevel?: string;
  elevatedRisk?: string;
  username?: string;
};

const sourceOptions = ["AUTH", "TASK", "TOOL", "APPROVAL", "CONNECTOR"];
const statusOptions = ["SUCCESS", "FAILURE", "FAILED", "PENDING", "APPROVED", "REJECTED", "CANCELED"];
const riskOptions = ["LOW", "MEDIUM", "HIGH"];

function AuditApp() {
  const storedContext = readStoredContext();
  const [form] = Form.useForm<AuditFilters>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [overview, setOverview] = useState<AuditOverview | null>(null);
  const [riskSummary, setRiskSummary] = useState<AuditRiskSummary | null>(null);
  const [events, setEvents] = useState<AuditTimelineEvent[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [selected, setSelected] = useState<AuditTimelineDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [statusLine, setStatusLine] = useState("审计中心已就绪。");

  const selectedEvent = selected?.event;
  const configSnapshot = useMemo(() => readRecord(selected?.context, "shopConfigSnapshot"), [selected]);
  const configChange = useMemo(() => readRecord(selected?.context, "recentShopConfigChange"), [selected]);

  const buildQuery = useCallback(
    (nextPageNum = pageNum, nextPageSize = pageSize) => {
      const values = form.getFieldsValue();
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "source", values.source);
      add(params, "eventType", values.eventType);
      add(params, "eventStatus", values.eventStatus);
      add(params, "taskId", values.taskId);
      add(params, "traceId", values.traceId);
      add(params, "toolCode", values.toolCode);
      add(params, "riskLevel", values.riskLevel);
      add(params, "username", values.username);
      if (values.elevatedRisk === "true") {
        params.set("elevatedRisk", "true");
      }
      return params;
    },
    [form, pageNum, pageSize]
  );

  const syncUrl = useCallback((params: URLSearchParams) => {
    window.history.replaceState(null, "", `${window.location.pathname}?${params.toString()}`);
  }, []);

  const loadDetail = useCallback(
    async (event: AuditTimelineEvent) => {
      if (!event.source || !event.resourceId) {
        setSelected({ event });
        return;
      }
      setDetailLoading(true);
      try {
        const detail = await apiGet<AuditTimelineDetail>(
          `/api/admin/audit/timeline/${encodeURIComponent(event.source)}/${encodeURIComponent(event.resourceId)}`,
          context
        );
        setSelected(detail);
        setStatusLine(`已加载 ${event.source} / ${event.resourceId} 的审计详情。`);
      } catch (error) {
        message.error(errorMessage(error));
        setSelected({ event });
      } finally {
        setDetailLoading(false);
      }
    },
    [context]
  );

  const loadTimeline = useCallback(
    async (nextPageNum = pageNum, nextPageSize = pageSize) => {
      setLoading(true);
      const params = buildQuery(nextPageNum, nextPageSize);
      syncUrl(params);
      try {
        const page = await apiGet<PageResult<AuditTimelineEvent> & { pageNum?: number; pageSize?: number }>(
          `/api/admin/audit/timeline?${params.toString()}`,
          context
        );
        const list = page.list || [];
        setEvents(list);
        setTotal(Number(page.total || 0));
        setPageNum(Number(page.pageNum || nextPageNum));
        setPageSize(Number(page.pageSize || nextPageSize));
        setStatusLine("审计时间线已刷新。");
        if (list[0]) {
          void loadDetail(list[0]);
        } else {
          setSelected(null);
        }
      } catch (error) {
        message.error(errorMessage(error));
        setStatusLine(`审计时间线加载失败：${errorMessage(error)}`);
      } finally {
        setLoading(false);
      }
    },
    [buildQuery, context, loadDetail, pageNum, pageSize, syncUrl]
  );

  const refreshSummary = useCallback(async () => {
    try {
      const [nextOverview, nextRisk] = await Promise.all([
        apiGet<AuditOverview>("/api/admin/audit/overview", context),
        apiGet<AuditRiskSummary>("/api/admin/audit/high-risk", context)
      ]);
      setOverview(nextOverview);
      setRiskSummary(nextRisk);
    } catch (error) {
      message.error(errorMessage(error));
    }
  }, [context]);

  const exportCsv = useCallback(async () => {
    setExporting(true);
    try {
      const params = buildQuery(1, 100);
      const response = await fetch(`/api/admin/audit/export.csv?${params.toString()}`, {
        headers: buildHeaders(context)
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `shopops-audit-${new Date().toISOString().slice(0, 10)}.csv`;
      link.click();
      URL.revokeObjectURL(url);
      setStatusLine("CSV 已下载。");
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setExporting(false);
    }
  }, [buildQuery, context]);

  const copyDetail = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(JSON.stringify(selected || {}, null, 2));
      message.success("审计详情已复制");
    } catch {
      fallbackCopy(JSON.stringify(selected || {}, null, 2));
      message.success("审计详情已复制");
    }
  }, [selected]);

  const applyQuickFilter = useCallback(
    (kind: "failed" | "approval" | "risk") => {
      if (kind === "failed") {
        form.setFieldsValue({ eventStatus: "FAILURE", elevatedRisk: undefined });
      }
      if (kind === "approval") {
        form.setFieldsValue({ source: "APPROVAL", elevatedRisk: undefined });
      }
      if (kind === "risk") {
        form.setFieldsValue({ elevatedRisk: "true", eventStatus: undefined });
      }
      void loadTimeline(1, pageSize);
    },
    [form, loadTimeline, pageSize]
  );

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const initial: AuditFilters = {};
    ["source", "eventType", "eventStatus", "taskId", "traceId", "toolCode", "riskLevel", "username"].forEach((key) => {
      const value = params.get(key);
      if (value) {
        initial[key as keyof AuditFilters] = value;
      }
    });
    if (params.get("elevatedRisk") === "true") {
      initial.elevatedRisk = "true";
    }
    form.setFieldsValue(initial);
    setPageNum(positiveInt(params.get("pageNum"), 1));
    setPageSize(positiveInt(params.get("pageSize"), 20));
  }, [form]);

  useEffect(() => {
    void refreshSummary();
    void loadTimeline(pageNum, pageSize);
  }, [refreshSummary]);

  return (
    <Layout className="app-shell">
      <AdminSidebar active="audit" />
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>调用链审计中心</Title>
            <Text type="secondary">把 Agent 任务、工具调用、审批动作和店铺配置快照串成可追踪证据链</Text>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => { void refreshSummary(); void loadTimeline(1, pageSize); }}>
              刷新
            </Button>
            <Button icon={<DownloadOutlined />} loading={exporting} onClick={exportCsv}>
              CSV 下载
            </Button>
          </Space>
        </Header>
        <Content className="content">
          <Alert className="status-alert" message={statusLine} type="info" showIcon />
          <Row gutter={[16, 16]}>
            <Col xs={24} lg={16}>
              <Row gutter={[16, 16]}>
                <MetricCard title="审计事件" value={(overview?.authEventTotal || 0) + (overview?.taskEventTotal || 0) + (overview?.toolCallTotal || 0)} />
                <MetricCard title="失败事件" value={(overview?.authFailureTotal || 0) + (overview?.taskFailureTotal || 0) + (overview?.toolCallFailed || 0)} />
                <MetricCard title="高风险链路" value={riskSummary?.elevatedRiskTotal || 0} />
              </Row>
              <Card title="快捷筛选" className="section-card">
                <Space wrap>
                  <Button data-quick-filter="failed" onClick={() => applyQuickFilter("failed")}>失败链路</Button>
                  <Button data-quick-filter="approval" onClick={() => applyQuickFilter("approval")}>审批事件</Button>
                  <Button data-quick-filter="risk" onClick={() => applyQuickFilter("risk")}>中高风险</Button>
                  <Button
                    data-empty-reset
                    onClick={() => {
                      form.resetFields();
                      void loadTimeline(1, pageSize);
                    }}
                  >
                    重置
                  </Button>
                </Space>
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card title="风险分布" className="section-card">
                <AuditRiskChart breakdown={riskSummary?.riskBreakdown} />
              </Card>
            </Col>
          </Row>

          <Card title="审计筛选" className="section-card">
            <Form form={form} layout="vertical" onFinish={() => loadTimeline(1, pageSize)}>
              <Row gutter={12}>
                <Col xs={24} md={6}>
                  <Form.Item label="来源" name="source">
                    <Select allowClear options={sourceOptions.map((value) => ({ value, label: value }))} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item label="状态" name="eventStatus">
                    <Select allowClear options={statusOptions.map((value) => ({ value, label: value }))} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item label="风险等级" name="riskLevel">
                    <Select allowClear options={riskOptions.map((value) => ({ value, label: value }))} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item label="只看中高风险" name="elevatedRisk">
                    <Select allowClear options={[{ value: "true", label: "是" }]} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item label="任务 ID" name="taskId"><Input allowClear /></Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item label="Trace ID" name="traceId"><Input allowClear /></Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item label="工具编码" name="toolCode"><Input allowClear /></Form.Item>
                </Col>
                <Col xs={24} md={6}>
                  <Form.Item label="用户名" name="username"><Input allowClear /></Form.Item>
                </Col>
              </Row>
              <Flex justify="space-between" wrap="wrap" gap={12}>
                <Space>
                  <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
                  <Button onClick={() => { form.resetFields(); void loadTimeline(1, pageSize); }}>重置</Button>
                </Space>
                <ContextInputs context={context} onChange={setContext} />
              </Flex>
            </Form>
          </Card>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={14}>
              <Card title="审计时间线" className="section-card timeline-column">
                <Table
                  rowKey={(record) => `${record.source}-${record.resourceId || record.eventId}`}
                  loading={loading}
                  dataSource={events}
                  pagination={{
                    current: pageNum,
                    pageSize,
                    total,
                    showSizeChanger: true,
                    onChange: (nextPage, nextSize) => loadTimeline(nextPage, nextSize)
                  }}
                  onRow={(record) => ({ onClick: () => loadDetail(record) })}
                  columns={[
                    { title: "时间", dataIndex: "createdAt", width: 170, render: formatTime },
                    { title: "来源", dataIndex: "source", width: 100, render: sourceTag },
                    { title: "事件", dataIndex: "eventType", width: 170 },
                    { title: "状态", dataIndex: "eventStatus", width: 110, render: statusTag },
                    { title: "风险", dataIndex: "riskLevel", width: 100, render: riskTag },
                    { title: "摘要", dataIndex: "summary", ellipsis: true }
                  ]}
                />
              </Card>
            </Col>
            <Col xs={24} xl={10}>
              <Card
                title="审计详情"
                className="section-card"
                loading={detailLoading}
                extra={<Button id="copyDetail" icon={<CopyOutlined />} onClick={copyDetail}>复制</Button>}
              >
                {selectedEvent ? (
                  <Space direction="vertical" size={16} className="full-width">
                    <Descriptions size="small" column={1} bordered>
                      <Descriptions.Item label="来源">{sourceTag(selectedEvent.source)}</Descriptions.Item>
                      <Descriptions.Item label="资源">{selectedEvent.resourceType || "-"} / {selectedEvent.resourceId || selectedEvent.eventId || "-"}</Descriptions.Item>
                      <Descriptions.Item label="任务">{selectedEvent.taskId || "-"}</Descriptions.Item>
                      <Descriptions.Item label="Trace">{selectedEvent.traceId || "-"}</Descriptions.Item>
                      <Descriptions.Item label="工具">{selectedEvent.toolCode || "-"}</Descriptions.Item>
                      <Descriptions.Item label="摘要">{selectedEvent.summary || "-"}</Descriptions.Item>
                    </Descriptions>
                    <ActionLinks event={selectedEvent} resource={selected?.resource} />
                    <ConfigSnapshot snapshot={configSnapshot} change={configChange} />
                    <JsonBlock title="上下文" value={selected?.context} />
                    <JsonBlock title="资源详情" value={selected?.resource} />
                  </Space>
                ) : (
                  <div className="empty-state">暂无审计事件</div>
                )}
              </Card>
            </Col>
          </Row>
        </Content>
      </Layout>
    </Layout>
  );
}

function MetricCard({ title, value }: { title: string; value: number }) {
  return (
    <Col xs={24} md={8}>
      <Card className="metric-card">
        <Statistic title={title} value={numberText(value)} />
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

function ActionLinks({ event, resource }: { event: AuditTimelineEvent; resource?: Record<string, unknown> }) {
  const reportId = event.detail?.reportId || resource?.reportId || readRecord(resource, "taskDetail")?.reportId;
  return (
    <Space wrap>
      <Button id="openTask" href={event.taskId ? `/admin/tasks.html?taskId=${event.taskId}` : "/admin/tasks.html"} icon={<AuditOutlined />}>任务</Button>
      <Button id="openReport" href={reportId ? `/admin/reports.html?reportId=${reportId}` : "/admin/reports.html"} icon={<FileTextOutlined />}>报告</Button>
      <Button id="openApproval" href="/admin/approvals.html" icon={<ApiOutlined />}>审批</Button>
      <Button id="openToolLogs" href={event.toolCode ? `/admin/tools.html?toolCode=${event.toolCode}` : "/admin/tools.html"} icon={<ToolOutlined />}>工具日志</Button>
    </Space>
  );
}

function ConfigSnapshot({ snapshot, change }: { snapshot?: Record<string, unknown>; change?: Record<string, unknown> }) {
  if (!snapshot && !change) {
    return <Alert id="configSnapshotBox" message="本事件暂无店铺配置快照" type="info" showIcon />;
  }
  return (
    <Space direction="vertical" className="full-width">
      {snapshot && (
        <Card id="configSnapshotBox" size="small" title="本次 Agent 使用的店铺配置快照">
          <Descriptions size="small" column={1}>
            <Descriptions.Item label="退款率预警阈值">{valueText(snapshot.refundRateWarnThreshold)}</Descriptions.Item>
            <Descriptions.Item label="差评预警阈值">{valueText(snapshot.negativeCommentWarnThreshold)}</Descriptions.Item>
            <Descriptions.Item label="高风险工具审批">{String(snapshot.agentToolApprovalEnabled ?? "-")}</Descriptions.Item>
            <Descriptions.Item label="模型策略">{valueText(snapshot.agentModelPolicy)}</Descriptions.Item>
          </Descriptions>
        </Card>
      )}
      {change && (
        <Card id="configChangeBox" size="small" title="最近配置变更">
          <Paragraph>{valueText(change.message)}</Paragraph>
          <JsonBlock title="变更详情" value={change} />
        </Card>
      )}
    </Space>
  );
}

function JsonBlock({ title, value }: { title: string; value?: unknown }) {
  if (!value) {
    return null;
  }
  return (
    <div>
      <Text strong>{title}</Text>
      <pre className="json-block">{JSON.stringify(value, null, 2)}</pre>
    </div>
  );
}

function sourceTag(value?: string) {
  return <Tag color={value === "TOOL" ? "blue" : value === "APPROVAL" ? "purple" : "default"}>{value || "-"}</Tag>;
}

function statusTag(value?: string) {
  const color = value === "SUCCESS" || value === "APPROVED" ? "green" : value === "FAILURE" || value === "FAILED" || value === "REJECTED" ? "red" : "gold";
  return <Tag color={color}>{value || "-"}</Tag>;
}

function riskTag(value?: string) {
  const color = value === "HIGH" ? "red" : value === "MEDIUM" ? "orange" : value === "LOW" ? "green" : "default";
  return <Tag color={color}>{value || "-"}</Tag>;
}

function add(params: URLSearchParams, key: string, value?: string) {
  if (value && value.trim()) {
    params.set(key, value.trim());
  }
}

function readRecord(source: unknown, key: string): Record<string, unknown> | undefined {
  if (!source || typeof source !== "object") {
    return undefined;
  }
  const value = (source as Record<string, unknown>)[key];
  return value && typeof value === "object" ? (value as Record<string, unknown>) : undefined;
}

function positiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function valueText(value: unknown) {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  return String(value);
}

function formatTime(value?: string) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 19);
}

function fallbackCopy(text: string) {
  const textarea = document.createElement("textarea");
  textarea.value = text;
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand("copy");
  document.body.removeChild(textarea);
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <AuditApp />
    </ConfigProvider>
  </React.StrictMode>
);
