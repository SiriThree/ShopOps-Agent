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
  FileTextOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
  ToolOutlined
} from "@ant-design/icons";
import { apiGet, apiPost, readStoredContext, type RequestContext } from "./api";
import { AdminSidebar } from "./AdminSidebar";
import type { McpTool, PageResult, ToolCallLog, ToolInvokeResult } from "./types";
import { numberText } from "./utils";
import { ToolStatusChart } from "./ToolStatusChart";
import "./styles.css";

const { Header, Content } = Layout;
const { Paragraph, Text, Title } = Typography;


type ToolFilters = {
  logId?: string;
  taskId?: string;
  status?: string;
  toolCode?: string;
};

type InvokeForm = {
  toolCode?: string;
  approvalId?: string;
  payload?: string;
};

const statusOptions = ["SUCCESS", "FAILED", "APPROVAL_REQUIRED", "RUNNING"];

function ToolsApp() {
  const storedContext = readStoredContext();
  const [filterForm] = Form.useForm<ToolFilters>();
  const [invokeForm] = Form.useForm<InvokeForm>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [tools, setTools] = useState<McpTool[]>([]);
  const [logs, setLogs] = useState<ToolCallLog[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [selectedToolCode, setSelectedToolCode] = useState("");
  const [selectedTool, setSelectedTool] = useState<McpTool | null>(null);
  const [selectedLog, setSelectedLog] = useState<ToolCallLog | null>(null);
  const [invokeResult, setInvokeResult] = useState<ToolInvokeResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [toolLoading, setToolLoading] = useState(false);
  const [logLoading, setLogLoading] = useState(false);
  const [invoking, setInvoking] = useState(false);
  const [statusLine, setStatusLine] = useState("工具日志中心已就绪。");

  const metrics = useMemo(() => buildMetrics(logs, tools), [logs, tools]);

  const buildQuery = useCallback(
    (nextPageNum = pageNum, nextPageSize = pageSize) => {
      const values = filterForm.getFieldsValue();
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "logId", values.logId);
      add(params, "taskId", values.taskId);
      add(params, "status", values.status);
      add(params, "toolCode", values.toolCode || selectedToolCode);
      return params;
    },
    [filterForm, pageNum, pageSize, selectedToolCode]
  );

  const syncUrl = useCallback((params: URLSearchParams) => {
    window.history.replaceState(null, "", `${window.location.pathname}?${params.toString()}`);
  }, []);

  const loadToolDetail = useCallback(
    async (toolCode: string) => {
      if (!toolCode) {
        setSelectedTool(null);
        return;
      }
      setToolLoading(true);
      try {
        const tool = await apiGet<McpTool>(`/api/tools/${encodeURIComponent(toolCode)}`, context);
        setSelectedTool(tool);
        setSelectedToolCode(toolCode);
        invokeForm.setFieldsValue({ toolCode });
        filterForm.setFieldsValue({ toolCode });
        setStatusLine(`已选中工具 ${toolCode}。`);
      } catch (error) {
        message.error(errorMessage(error));
      } finally {
        setToolLoading(false);
      }
    },
    [context, filterForm, invokeForm]
  );

  const loadTools = useCallback(async () => {
    setLoading(true);
    try {
      const list = await apiGet<McpTool[]>("/api/tools", context);
      setTools(list || []);
      const initial = selectedToolCode || list?.[0]?.toolCode || "";
      if (initial) {
        void loadToolDetail(initial);
      }
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [context, loadToolDetail, selectedToolCode]);

  const loadLogs = useCallback(
    async (nextPageNum = pageNum, nextPageSize = pageSize) => {
      setLogLoading(true);
      const params = buildQuery(nextPageNum, nextPageSize);
      syncUrl(params);
      try {
        const page = await apiGet<PageResult<ToolCallLog> & { pageNum?: number; pageSize?: number }>(
          `/api/tools/call-logs?${params.toString()}`,
          context
        );
        const list = page.list || [];
        setLogs(list);
        setTotal(Number(page.total || 0));
        setPageNum(Number(page.pageNum || nextPageNum));
        setPageSize(Number(page.pageSize || nextPageSize));
        setSelectedLog(list[0] || null);
        setStatusLine("工具调用日志已刷新。");
      } catch (error) {
        message.error(errorMessage(error));
        setStatusLine(`工具调用日志加载失败：${errorMessage(error)}`);
      } finally {
        setLogLoading(false);
      }
    },
    [buildQuery, context, pageNum, pageSize, syncUrl]
  );

  const invokeTool = useCallback(async () => {
    const values = invokeForm.getFieldsValue();
    const toolCode = values.toolCode || selectedToolCode;
    if (!toolCode) {
      message.warning("请选择工具");
      return;
    }
    let payload: Record<string, unknown>;
    try {
      payload = values.payload ? JSON.parse(values.payload) : {};
    } catch {
      message.error("Payload 必须是合法 JSON");
      return;
    }
    if (values.approvalId) {
      payload.approvalId = Number(values.approvalId);
    }
    setInvoking(true);
    try {
      const result = await apiPost<ToolInvokeResult>(`/api/tools/${encodeURIComponent(toolCode)}/invoke`, payload, context);
      setInvokeResult(result);
      setStatusLine(`工具 ${toolCode} 调用完成：${result.status || "-"}`);
      await loadLogs(1, pageSize);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setInvoking(false);
    }
  }, [context, invokeForm, loadLogs, pageSize, selectedToolCode]);

  const applyQuickFilter = useCallback(
    (kind: "failed" | "approval") => {
      filterForm.setFieldsValue({
        status: kind === "failed" ? "FAILED" : "APPROVAL_REQUIRED"
      });
      void loadLogs(1, pageSize);
    },
    [filterForm, loadLogs, pageSize]
  );

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const initial: ToolFilters = {};
    ["logId", "taskId", "status", "toolCode"].forEach((key) => {
      const value = params.get(key);
      if (value) {
        initial[key as keyof ToolFilters] = value;
      }
    });
    filterForm.setFieldsValue(initial);
    if (initial.toolCode) {
      setSelectedToolCode(initial.toolCode);
      invokeForm.setFieldsValue({ toolCode: initial.toolCode });
    }
    setPageNum(positiveInt(params.get("pageNum"), 1));
    setPageSize(positiveInt(params.get("pageSize"), 20));
  }, [filterForm, invokeForm]);

  useEffect(() => {
    void loadTools();
    void loadLogs(pageNum, pageSize);
  }, [loadTools]);

  return (
    <Layout className="app-shell">
      <AdminSidebar active="tools" />
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>MCP 工具调用日志</Title>
            <Text type="secondary">统一展示工具注册、手动调用、审批拦截、失败原因和审计链路</Text>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => { void loadTools(); void loadLogs(1, pageSize); }}>
              刷新
            </Button>
            <Button href={selectedLog?.taskId ? `/admin/audit.html?source=TOOL&toolCode=${selectedLog.toolCode}` : "/admin/audit.html"} icon={<AuditOutlined />}>
              审计
            </Button>
          </Space>
        </Header>
        <Content className="content">
          <Alert className="status-alert" message={statusLine} type="info" showIcon />
          <Row gutter={[16, 16]}>
            <MetricCard title="注册工具" value={metrics.toolTotal} />
            <MetricCard title="日志总数" value={total} />
            <MetricCard title="失败/审批" value={metrics.failedOrApproval} />
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={14}>
              <Card title="工具注册表" className="section-card" loading={loading}>
                <Table
                  rowKey={(record) => record.toolCode || ""}
                  dataSource={tools}
                  pagination={false}
                  onRow={(record) => ({ onClick: () => { void loadToolDetail(record.toolCode || ""); void loadLogs(1, pageSize); } })}
                  rowClassName={(record) => record.toolCode === selectedToolCode ? "selected-row clickable" : "clickable"}
                  columns={[
                    { title: "工具编码", dataIndex: "toolCode", width: 220 },
                    { title: "名称", dataIndex: "toolName" },
                    { title: "分类", dataIndex: "category", width: 120 },
                    { title: "风险", dataIndex: "riskLevel", width: 100, render: riskTag },
                    { title: "审批", dataIndex: "needApproval", width: 90, render: (value: boolean) => <Tag color={value ? "purple" : "green"}>{value ? "需要" : "无需"}</Tag> },
                    { title: "状态", dataIndex: "enabled", width: 90, render: (value: boolean) => <Tag color={value ? "green" : "default"}>{value ? "启用" : "停用"}</Tag> }
                  ]}
                />
              </Card>
            </Col>
            <Col xs={24} xl={10}>
              <Card title="工具状态分布" className="section-card">
                <ToolStatusChart logs={logs} />
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={12}>
              <Card title="手动调用" className="section-card" loading={toolLoading}>
                <Descriptions size="small" bordered column={1} id="toolSummary">
                  <Descriptions.Item label="工具">{selectedTool?.toolCode || "-"}</Descriptions.Item>
                  <Descriptions.Item label="名称">{selectedTool?.toolName || "-"}</Descriptions.Item>
                  <Descriptions.Item label="权限">{selectedTool?.permissionCode || "-"}</Descriptions.Item>
                  <Descriptions.Item label="版本">{selectedTool?.version || "-"}</Descriptions.Item>
                </Descriptions>
                <Form form={invokeForm} layout="vertical" className="form-stack">
                  <Form.Item label="工具编码" name="toolCode">
                    <Select
                      showSearch
                      options={tools.map((tool) => ({ value: tool.toolCode, label: tool.toolCode }))}
                      onChange={(value) => { void loadToolDetail(value); }}
                    />
                  </Form.Item>
                  <Form.Item label="审批 ID" name="approvalId">
                    <Input allowClear placeholder="需要人工审批后的高风险工具可填 approvalId" />
                  </Form.Item>
                  <Form.Item label="Payload JSON" name="payload" initialValue={"{}"}>
                    <Input.TextArea rows={8} spellCheck={false} />
                  </Form.Item>
                  <Space wrap>
                    <Button id="invokeSubmit" type="primary" icon={<PlayCircleOutlined />} loading={invoking} onClick={invokeTool}>
                      调用工具
                    </Button>
                    <Button id="copyToolDetail" icon={<CopyOutlined />} onClick={() => copyJson(selectedTool)}>
                      复制工具
                    </Button>
                  </Space>
                </Form>
              </Card>
            </Col>
            <Col xs={24} xl={12}>
              <Card
                title="调用结果"
                className="section-card"
                extra={<Button id="copyInvokeResult" icon={<CopyOutlined />} onClick={() => copyJson(invokeResult)}>复制</Button>}
              >
                {invokeResult ? (
                  <Space direction="vertical" className="full-width">
                    <Descriptions size="small" bordered column={1}>
                      <Descriptions.Item label="状态">{statusTag(invokeResult.status)}</Descriptions.Item>
                      <Descriptions.Item label="Log ID">{invokeResult.toolCallLogId || "-"}</Descriptions.Item>
                      <Descriptions.Item label="审批 ID">{invokeResult.approvalId || "-"}</Descriptions.Item>
                      <Descriptions.Item label="错误">{invokeResult.errorMessage || "-"}</Descriptions.Item>
                    </Descriptions>
                    <JsonBlock title="结果数据" value={invokeResult.data} />
                  </Space>
                ) : (
                  <div className="empty-state">暂无调用结果</div>
                )}
              </Card>
            </Col>
          </Row>

          <Card title="调用日志" className="section-card">
            <Form form={filterForm} layout="vertical" onFinish={() => loadLogs(1, pageSize)}>
              <Row gutter={12}>
                <Col xs={24} md={6}><Form.Item label="Log ID" name="logId"><Input allowClear /></Form.Item></Col>
                <Col xs={24} md={6}><Form.Item label="任务 ID" name="taskId"><Input allowClear /></Form.Item></Col>
                <Col xs={24} md={6}><Form.Item label="工具编码" name="toolCode"><Input allowClear /></Form.Item></Col>
                <Col xs={24} md={6}>
                  <Form.Item label="状态" name="status">
                    <Select allowClear options={statusOptions.map((value) => ({ value, label: value }))} />
                  </Form.Item>
                </Col>
              </Row>
              <Flex justify="space-between" wrap="wrap" gap={12}>
                <Space>
                  <Button id="toolFilterSubmit" type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
                  <Button onClick={() => { filterForm.resetFields(); setSelectedToolCode(""); void loadLogs(1, pageSize); }}>重置</Button>
                  <Button data-quick-filter="failed" onClick={() => applyQuickFilter("failed")}>失败日志</Button>
                  <Button data-quick-filter="approval" onClick={() => applyQuickFilter("approval")}>审批 required</Button>
                </Space>
                <ContextInputs context={context} onChange={setContext} />
              </Flex>
            </Form>
            <Table
              className="spaced-table"
              rowKey={(record) => String(record.id)}
              loading={logLoading}
              dataSource={logs}
              pagination={{
                current: pageNum,
                pageSize,
                total,
                showSizeChanger: true,
                onChange: (nextPage, nextSize) => loadLogs(nextPage, nextSize)
              }}
              onRow={(record) => ({ onClick: () => setSelectedLog(record) })}
              rowClassName={(record) => record.id === selectedLog?.id ? "selected-row clickable" : "clickable"}
              columns={[
                { title: "Log ID", dataIndex: "id", width: 90 },
                { title: "时间", dataIndex: "createdAt", width: 170, render: formatTime },
                { title: "工具", dataIndex: "toolCode", width: 210 },
                { title: "状态", dataIndex: "status", width: 140, render: statusTag },
                { title: "风险", dataIndex: "riskLevel", width: 100, render: riskTag },
                { title: "任务", dataIndex: "taskId", width: 100 },
                { title: "耗时", dataIndex: "latencyMs", width: 90, render: (value: number) => `${value || 0} ms` },
                { title: "错误", dataIndex: "errorMessage", ellipsis: true }
              ]}
            />
          </Card>

          <Card
            title="日志详情"
            className="section-card"
            extra={<Button id="copyLogPayload" icon={<CopyOutlined />} onClick={() => copyJson(selectedLog)}>复制</Button>}
          >
            {selectedLog ? (
              <Space direction="vertical" className="full-width">
                <Descriptions size="small" bordered column={1} id="logSummary">
                  <Descriptions.Item label="Log ID">{selectedLog.id || "-"}</Descriptions.Item>
                  <Descriptions.Item label="Trace">{selectedLog.traceId || "-"}</Descriptions.Item>
                  <Descriptions.Item label="Span">{selectedLog.spanId || "-"}</Descriptions.Item>
                  <Descriptions.Item label="审批">{selectedLog.approvalId || "-"}</Descriptions.Item>
                  <Descriptions.Item label="错误">{selectedLog.errorCode || "-"} {selectedLog.errorMessage || ""}</Descriptions.Item>
                </Descriptions>
                <Space wrap>
                  <Button id="openTask" href={selectedLog.taskId ? `/admin/tasks.html?taskId=${selectedLog.taskId}` : "/admin/tasks.html"} icon={<AuditOutlined />}>任务队列</Button>
                  <Button id="openAudit" href={`/admin/audit.html?source=TOOL&toolCode=${selectedLog.toolCode || ""}`} icon={<ApiOutlined />}>审计中心</Button>
                  <Button id="openReports" href={selectedLog.taskId ? `/admin/reports.html?taskId=${selectedLog.taskId}` : "/admin/reports.html"} icon={<FileTextOutlined />}>报告</Button>
                </Space>
                <JsonBlock title="Input" value={selectedLog.input} />
                <JsonBlock title="Output" value={selectedLog.output} />
              </Space>
            ) : (
              <div className="empty-state">暂无日志详情</div>
            )}
          </Card>
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

function buildMetrics(logs: ToolCallLog[], tools: McpTool[]) {
  return {
    toolTotal: tools.length,
    failedOrApproval: logs.filter((log) => log.status === "FAILED" || log.status === "APPROVAL_REQUIRED").length
  };
}

function statusTag(value?: string) {
  const color = value === "SUCCESS" ? "green" : value === "FAILED" ? "red" : value === "APPROVAL_REQUIRED" ? "purple" : "blue";
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

function positiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function formatTime(value?: string) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
}

async function copyJson(value: unknown) {
  const text = JSON.stringify(value || {}, null, 2);
  try {
    await navigator.clipboard.writeText(text);
    message.success("已复制");
  } catch {
    fallbackCopy(text);
    message.success("已复制");
  }
}

function fallbackCopy(text: string) {
  const textarea = document.createElement("textarea");
  textarea.dataset.copyMode = "fallbackCopy";
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
      <ToolsApp />
    </ConfigProvider>
  </React.StrictMode>
);
