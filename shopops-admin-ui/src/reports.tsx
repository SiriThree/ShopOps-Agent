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
  Tabs,
  Tag,
  Typography,
  message
} from "antd";
import { ApiOutlined, CopyOutlined, FileTextOutlined, ReloadOutlined, SearchOutlined, ToolOutlined } from "@ant-design/icons";
import { apiGet, readStoredContext, type RequestContext } from "./api";
import { AdminSidebar } from "./AdminSidebar";
import type { DataSourceEvidence, OperationReport, PageResult } from "./types";
import { moneyText, normalizeEvidence, numberText, percentText } from "./utils";
import { ReportStatusChart } from "./ReportStatusChart";
import "./styles.css";

const { Header, Content } = Layout;
const { Paragraph, Text, Title } = Typography;


type ReportFilters = {
  status?: string;
  reportType?: string;
  reportNo?: string;
  taskId?: string;
  traceId?: string;
  createdBy?: string;
};

function ReportsApp() {
  const storedContext = readStoredContext();
  const [form] = Form.useForm<ReportFilters>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [reports, setReports] = useState<OperationReport[]>([]);
  const [selectedReportId, setSelectedReportId] = useState<string>("");
  const [selectedReport, setSelectedReport] = useState<OperationReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [statusLine, setStatusLine] = useState("报告中心已就绪。");

  const evidence = useMemo(() => normalizeEvidence(selectedReport?.evidence), [selectedReport]);
  const statusMetrics = useMemo(() => buildStatusMetrics(reports, total), [reports, total]);

  const buildQuery = useCallback(
    (nextPageNum = pageNum, nextPageSize = pageSize) => {
      const values = form.getFieldsValue();
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "status", values.status);
      add(params, "reportType", values.reportType);
      add(params, "reportNo", values.reportNo);
      add(params, "taskId", values.taskId);
      add(params, "traceId", values.traceId);
      add(params, "createdBy", values.createdBy);
      add(params, "reportId", selectedReportId);
      return params;
    },
    [form, pageNum, pageSize, selectedReportId]
  );

  const syncUrl = useCallback(
    (params: URLSearchParams) => {
      window.history.replaceState(null, "", `${window.location.pathname}?${params.toString()}`);
    },
    []
  );

  const loadDetail = useCallback(
    async (reportId: string | number) => {
      setDetailLoading(true);
      try {
        const report = await apiGet<OperationReport>(`/api/reports/${encodeURIComponent(reportId)}`, context);
        setSelectedReport(report);
        setSelectedReportId(String(report.reportId || reportId));
        setStatusLine(`已加载报告 ${report.reportNo || report.reportId}`);
      } catch (error) {
        message.error(errorMessage(error));
        setStatusLine(`报告详情加载失败：${errorMessage(error)}`);
      } finally {
        setDetailLoading(false);
      }
    },
    [context]
  );

  const loadReports = useCallback(
    async (nextPageNum = pageNum, nextPageSize = pageSize) => {
      setLoading(true);
      const params = buildQuery(nextPageNum, nextPageSize);
      syncUrl(params);
      try {
        const page = await apiGet<PageResult<OperationReport> & { pageNum?: number; pageSize?: number }>(`/api/reports?${params.toString()}`, context);
        const list = page.list || [];
        setReports(list);
        setTotal(Number(page.total || 0));
        setPageNum(Number(page.pageNum || nextPageNum));
        setPageSize(Number(page.pageSize || nextPageSize));
        setStatusLine("报告列表已刷新。");
        if (selectedReportId) {
          void loadDetail(selectedReportId);
        } else if (list[0]?.reportId) {
          void loadDetail(list[0].reportId);
        }
      } catch (error) {
        setReports([]);
        setTotal(0);
        setStatusLine(`报告加载失败：${errorMessage(error)}`);
      } finally {
        setLoading(false);
      }
    },
    [buildQuery, context, loadDetail, pageNum, pageSize, selectedReportId, syncUrl]
  );

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    form.setFieldsValue({
      status: params.get("status") || undefined,
      reportType: params.get("reportType") || undefined,
      reportNo: params.get("reportNo") || undefined,
      taskId: params.get("taskId") || undefined,
      traceId: params.get("traceId") || undefined,
      createdBy: params.get("createdBy") || undefined
    });
    const initialReportId = params.get("reportId") || "";
    const initialPageNum = positiveInt(params.get("pageNum"), 1);
    const initialPageSize = positiveInt(params.get("pageSize"), 20);
    setSelectedReportId(initialReportId);
    setPageNum(initialPageNum);
    setPageSize(initialPageSize);
    void loadReports(initialPageNum, initialPageSize);
  }, []);

  function submitFilters() {
    setPageNum(1);
    void loadReports(1, pageSize);
  }

  function applyQuickFilter(name: string) {
    if (name === "clear") {
      form.resetFields();
    } else if (name === "success") {
      form.setFieldsValue({ status: "SUCCESS", reportType: undefined });
    } else if (name === "daily") {
      form.setFieldsValue({ reportType: "daily_review", status: undefined });
    }
    setPageNum(1);
    void loadReports(1, pageSize);
  }

  return (
    <Layout className="app-shell">
      <AdminSidebar active="reports" />
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>报告中心</Title>
            <Text type="secondary">查看 Agent 产出的运营日报、证据快照、数据来源和配置快照。</Text>
          </div>
          <Space wrap>
            <Input addonBefore="租户" value={context.tenantId} onChange={(event) => setContext({ ...context, tenantId: event.target.value })} />
            <Input addonBefore="店铺" value={context.shopId} onChange={(event) => setContext({ ...context, shopId: event.target.value })} />
            <Input addonBefore="用户" value={context.userId} onChange={(event) => setContext({ ...context, userId: event.target.value })} />
            <Input addonBefore="角色" value={context.roles} onChange={(event) => setContext({ ...context, roles: event.target.value })} />
          </Space>
        </Header>
        <Content className="content">
          <Row gutter={[16, 16]}>
            <Col xs={24} xl={15}>
              <Space direction="vertical" className="full" size={16}>
                <Alert showIcon type="info" message={statusLine} />
                <Card title="报告筛选" extra={<Button icon={<ReloadOutlined />} loading={loading} onClick={() => loadReports()}>刷新</Button>}>
                  <Form form={form} layout="vertical" onFinish={submitFilters}>
                    <Row gutter={10}>
                      <Col xs={24} md={8}>
                        <Form.Item name="status" label="状态">
                          <Select allowClear options={[{ value: "SUCCESS" }, { value: "FAILED" }, { value: "DEGRADED" }]} />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item name="reportType" label="报告类型">
                          <Select allowClear options={[{ value: "daily_review" }, { value: "comment_risk" }, { value: "product_optimization" }, { value: "ad_anomaly" }]} />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item name="reportNo" label="报告编号">
                          <Input placeholder="REPORT-" />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item name="taskId" label="任务 ID">
                          <Input />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item name="traceId" label="Trace ID">
                          <Input />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item name="createdBy" label="创建人">
                          <Input />
                        </Form.Item>
                      </Col>
                    </Row>
                    <Flex justify="space-between" gap={8} wrap="wrap">
                      <Space wrap>
                        <Button data-quick-filter="daily" onClick={() => applyQuickFilter("daily")}>日报</Button>
                        <Button onClick={() => applyQuickFilter("success")}>成功报告</Button>
                        <Button onClick={() => applyQuickFilter("clear")}>清空</Button>
                      </Space>
                      <Button id="reportFilterSubmit" type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
                    </Flex>
                  </Form>
                </Card>
                <Card title="报告列表">
                  <Table
                    rowKey={(row) => String(row.reportId)}
                    loading={loading}
                    dataSource={reports}
                    pagination={{
                      current: pageNum,
                      pageSize,
                      total,
                      showSizeChanger: true,
                      onChange: (nextPage, nextPageSize) => loadReports(nextPage, nextPageSize)
                    }}
                    onRow={(row) => ({
                      onClick: () => row.reportId && loadDetail(row.reportId)
                    })}
                    rowClassName={(row) => String(row.reportId) === String(selectedReportId) ? "selected-row" : "clickable"}
                    columns={[
                      { title: "报告编号", dataIndex: "reportNo", render: (value, row) => value || row.reportId },
                      { title: "标题", dataIndex: "title", ellipsis: true },
                      { title: "类型", dataIndex: "reportType", width: 150 },
                      { title: "状态", dataIndex: "status", width: 110, render: (value) => <StatusTag status={value} /> },
                      { title: "任务", dataIndex: "taskId", width: 100 },
                      { title: "Trace", dataIndex: "traceId", ellipsis: true },
                      { title: "创建时间", dataIndex: "createdAt", width: 180 }
                    ]}
                  />
                </Card>
              </Space>
            </Col>

            <Col xs={24} xl={9}>
              <Space direction="vertical" className="full" size={16}>
                <Card title="报告概览">
                  <Row gutter={[12, 12]}>
                    <Col span={8}><Statistic title="总数" value={total} /></Col>
                    <Col span={8}><Statistic title="本页" value={reports.length} /></Col>
                    <Col span={8}><Statistic title="成功" value={statusMetrics.success} /></Col>
                  </Row>
                  <ReportStatusChart success={statusMetrics.success} failed={statusMetrics.failed} other={statusMetrics.other} />
                </Card>
                <Card
                  title="报告详情"
                  loading={detailLoading}
                  extra={
                    <Space>
                      <Button id="openTask" href={selectedReport?.taskId ? `/admin/tasks.html?taskId=${selectedReport.taskId}` : "/admin/tasks.html"} icon={<FileTextOutlined />}>任务</Button>
                      <Button id="openAudit" href={auditHref(selectedReport)} icon={<ApiOutlined />}>审计</Button>
                      <Button id="openToolLogs" href={toolHref(selectedReport)} icon={<ToolOutlined />}>工具</Button>
                    </Space>
                  }
                >
                  {selectedReport ? (
                    <Tabs
                      items={[
                        {
                          key: "preview",
                          label: "报告预览",
                          children: <Paragraph className="markdown-preview">{selectedReport.markdown || selectedReport.summary || "暂无 Markdown"}</Paragraph>
                        },
                        {
                          key: "evidence",
                          label: "证据",
                          children: (
                            <Space direction="vertical" className="full">
                              <Descriptions size="small" column={1} bordered items={summaryItems(selectedReport, evidence)} />
                              <DataSourceSnapshot dataSources={evidence.dataSources} />
                              <ConfigSnapshot snapshot={evidence.shopConfig} />
                              <Button id="copyEvidence" icon={<CopyOutlined />} onClick={() => copyText(JSON.stringify(evidence, null, 2))}>复制证据</Button>
                              <pre className="json-box">{JSON.stringify(evidence, null, 2)}</pre>
                            </Space>
                          )
                        }
                      ]}
                    />
                  ) : (
                    <div className="empty-state">
                      <strong>暂无报告</strong>
                      <span>选择左侧报告，或从 Agent 工作台生成一份新的运营日报。</span>
                    </div>
                  )}
                </Card>
              </Space>
            </Col>
          </Row>
        </Content>
      </Layout>
    </Layout>
  );
}

function DataSourceSnapshot({ dataSources }: { dataSources?: DataSourceEvidence }) {
  if (!dataSources) {
    return null;
  }
  const order = dataSources.orderSummary || {};
  const comments = dataSources.negativeComments || {};
  const products = dataSources.productCandidates || {};
  return (
    <Descriptions title="数据来源" size="small" column={1} bordered>
      <Descriptions.Item label="订单 Connector">{`${order.connectorCode || "-"} / GMV ${moneyText(order.metrics?.gmv)} / 订单 ${numberText(order.metrics?.orderCount)}`}</Descriptions.Item>
      <Descriptions.Item label="评价 Connector">{`${comments.connectorCode || "-"} / 风险评价 ${numberText(comments.metrics?.negativeCount)}`}</Descriptions.Item>
      <Descriptions.Item label="商品 Connector">{`${products.connectorCode || "-"} / 商品候选 ${numberText(products.metrics?.candidateCount)}`}</Descriptions.Item>
    </Descriptions>
  );
}

function ConfigSnapshot({ snapshot }: { snapshot?: Record<string, unknown> }) {
  if (!snapshot) {
    return null;
  }
  return (
    <Descriptions title="店铺配置快照" size="small" column={1} bordered>
      <Descriptions.Item label="退款率阈值">{percentText(snapshot.refundRateWarnThreshold)}</Descriptions.Item>
      <Descriptions.Item label="差评阈值">{numberText(snapshot.negativeCommentWarnThreshold)}</Descriptions.Item>
      <Descriptions.Item label="高风险工具审批">{approvalText(snapshot.agentToolApprovalEnabled)}</Descriptions.Item>
      <Descriptions.Item label="模型策略">{String(snapshot.agentModelPolicy || "-")}</Descriptions.Item>
    </Descriptions>
  );
}

function summaryItems(report: OperationReport, evidence: ReturnType<typeof normalizeEvidence>) {
  return [
    { key: "report", label: "报告", children: report.reportNo || report.reportId || "-" },
    { key: "status", label: "状态", children: <StatusTag status={report.status} /> },
    { key: "task", label: "任务", children: report.taskId || "-" },
    { key: "trace", label: "Trace", children: report.traceId || "-" },
    { key: "type", label: "类型", children: report.reportType || "-" },
    { key: "evidence", label: "证据", children: evidenceSummary(evidence) }
  ];
}

function evidenceSummary(evidence: ReturnType<typeof normalizeEvidence>) {
  const parts = [];
  if (Array.isArray(evidence.toolCodes)) {
    parts.push(`${evidence.toolCodes.length} 个工具`);
  }
  if (Array.isArray(evidence.productIds)) {
    parts.push(`${evidence.productIds.length} 个商品`);
  }
  if (Array.isArray(evidence.riskCommentIds)) {
    parts.push(`${evidence.riskCommentIds.length} 条风险评价`);
  }
  return parts.join(" / ") || "可用";
}

function StatusTag({ status }: { status?: string }) {
  const value = String(status || "-").toUpperCase();
  const color = value === "SUCCESS" ? "success" : value === "FAILED" || value === "DEGRADED" ? "error" : value === "-" ? "default" : "processing";
  return <Tag color={color}>{value}</Tag>;
}

function buildStatusMetrics(reports: OperationReport[], total: number) {
  const success = reports.filter((report) => report.status === "SUCCESS").length;
  const failed = reports.filter((report) => report.status === "FAILED").length;
  return {
    success,
    failed,
    other: Math.max(total - success - failed, 0)
  };
}

function auditHref(report: OperationReport | null) {
  if (report?.taskId) {
    return `/admin/audit.html?source=TASK&taskId=${report.taskId}`;
  }
  return report?.traceId ? `/admin/audit.html?traceId=${report.traceId}` : "/admin/audit.html";
}

function toolHref(report: OperationReport | null) {
  if (report?.taskId) {
    return `/admin/tools.html?taskId=${report.taskId}`;
  }
  return report?.traceId ? `/admin/tools.html?traceId=${report.traceId}` : "/admin/tools.html";
}

function approvalText(value: unknown) {
  if (value === true || value === "true") {
    return "需要审批";
  }
  if (value === false || value === "false") {
    return "配置关闭";
  }
  return "-";
}

function add(params: URLSearchParams, key: string, value: unknown) {
  if (value !== undefined && value !== null && String(value).trim() !== "") {
    params.set(key, String(value).trim());
  }
}

function positiveInt(value: string | null, fallback: number) {
  const numeric = Number(value);
  return Number.isInteger(numeric) && numeric > 0 ? numeric : fallback;
}

async function copyText(value: string) {
  try {
    await navigator.clipboard.writeText(value);
    message.success("已复制");
  } catch {
    message.error("复制失败");
  }
}

function errorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return String(error);
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
      <ReportsApp />
    </ConfigProvider>
  </React.StrictMode>
);
