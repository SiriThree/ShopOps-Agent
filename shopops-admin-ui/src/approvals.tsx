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
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message
} from "antd";
import type { TableRowSelection } from "antd/es/table/interface";
import {
  AuditOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  RobotOutlined,
  SearchOutlined,
  StopOutlined,
  ToolOutlined
} from "@ant-design/icons";
import { apiGet, apiPost, readStoredContext, type RequestContext } from "./api";
import type { ApprovalBatchDecisionResult, ApprovalRequest, PageResult } from "./types";
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

const statusOptions = ["PENDING", "APPROVED", "REJECTED", "WITHDRAWN", "EXPIRED"];
const riskOptions = ["HIGH", "MEDIUM", "LOW"];

type ApprovalFilters = {
  status?: string;
  riskLevel?: string;
  toolCode?: string;
  taskId?: string;
  traceId?: string;
  approvalNo?: string;
  approvalId?: string;
};

type DecisionForm = {
  comment?: string;
  confirmText?: string;
};

function ApprovalsApp() {
  const storedContext = readStoredContext();
  const [filterForm] = Form.useForm<ApprovalFilters>();
  const [decisionForm] = Form.useForm<DecisionForm>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
  const [selectedApproval, setSelectedApproval] = useState<ApprovalRequest | null>(null);
  const [selectedIds, setSelectedIds] = useState<React.Key[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [deciding, setDeciding] = useState(false);
  const [statusLine, setStatusLine] = useState("审批中心已就绪。");

  const metrics = useMemo(() => buildMetrics(approvals), [approvals]);

  const buildQuery = useCallback(
    (nextPageNum = pageNum, nextPageSize = pageSize) => {
      const values = filterForm.getFieldsValue();
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "status", values.status);
      add(params, "riskLevel", values.riskLevel);
      add(params, "toolCode", values.toolCode);
      add(params, "taskId", values.taskId);
      add(params, "traceId", values.traceId);
      add(params, "approvalNo", values.approvalNo);
      add(params, "approvalId", values.approvalId);
      return params;
    },
    [filterForm, pageNum, pageSize]
  );

  const syncUrl = useCallback((params: URLSearchParams) => {
    window.history.replaceState(null, "", `${window.location.pathname}?${params.toString()}`);
  }, []);

  const loadDetail = useCallback(
    async (approvalId?: number | string) => {
      if (!approvalId) {
        setSelectedApproval(null);
        return;
      }
      setDetailLoading(true);
      try {
        const item = await apiGet<ApprovalRequest>(`/api/admin/approvals/${encodeURIComponent(String(approvalId))}`, context);
        setSelectedApproval(item);
        decisionForm.setFieldsValue({ comment: item.decisionComment || "", confirmText: "" });
        setStatusLine(`审批详情已加载：${item.approvalNo || item.approvalId}`);
      } catch (error) {
        message.error(errorMessage(error));
      } finally {
        setDetailLoading(false);
      }
    },
    [context, decisionForm]
  );

  const loadApprovals = useCallback(
    async (nextPageNum = pageNum, nextPageSize = pageSize) => {
      setLoading(true);
      const params = buildQuery(nextPageNum, nextPageSize);
      try {
        const data = await apiGet<PageResult<ApprovalRequest>>(`/api/admin/approvals?${params}`, context);
        const list = data.list || [];
        setApprovals(list);
        setTotal(data.total || 0);
        setPageNum(nextPageNum);
        setPageSize(nextPageSize);
        setSelectedIds((ids) => ids.filter((id) => list.some((item) => item.approvalId === id && item.status === "PENDING")));
        syncUrl(params);
        const requestedId = filterForm.getFieldValue("approvalId");
        if (requestedId) {
          void loadDetail(requestedId);
        } else if (!selectedApproval && list[0]?.approvalId) {
          void loadDetail(list[0].approvalId);
        }
        setStatusLine("审批单已刷新。");
      } catch (error) {
        setStatusLine(`审批单加载失败：${errorMessage(error)}`);
        message.error(errorMessage(error));
      } finally {
        setLoading(false);
      }
    },
    [buildQuery, context, filterForm, loadDetail, pageNum, pageSize, selectedApproval, syncUrl]
  );

  const applyInitialQuery = useCallback(() => {
    const params = new URLSearchParams(window.location.search);
    filterForm.setFieldsValue({
      status: params.get("status") || undefined,
      riskLevel: params.get("riskLevel") || undefined,
      toolCode: params.get("toolCode") || undefined,
      taskId: params.get("taskId") || undefined,
      traceId: params.get("traceId") || undefined,
      approvalNo: params.get("approvalNo") || undefined,
      approvalId: params.get("approvalId") || undefined
    });
    const approvalId = params.get("approvalId");
    if (approvalId) {
      void loadDetail(approvalId);
    }
  }, [filterForm, loadDetail]);

  useEffect(() => {
    applyInitialQuery();
    void loadApprovals(positiveInt(new URLSearchParams(window.location.search).get("pageNum"), 1), positiveInt(new URLSearchParams(window.location.search).get("pageSize"), 20));
  }, []);

  function applyQuickFilter(kind: "pending" | "high" | "refund") {
    if (kind === "pending") {
      filterForm.setFieldsValue({ status: "PENDING" });
    }
    if (kind === "high") {
      filterForm.setFieldsValue({ riskLevel: "HIGH" });
    }
    if (kind === "refund") {
      filterForm.setFieldsValue({ toolCode: "order.refund_execute" });
    }
    void loadApprovals(1, pageSize);
  }

  async function decide(action: "approve" | "reject" | "withdraw") {
    if (!selectedApproval?.approvalId) {
      message.warning("请先选择一条审批单");
      return;
    }
    setDeciding(true);
    try {
      const payload = decisionForm.getFieldsValue();
      const item = await apiPost<ApprovalRequest>(
        `/api/admin/approvals/${encodeURIComponent(String(selectedApproval.approvalId))}/${action}`,
        payload,
        context
      );
      setSelectedApproval(item);
      setStatusLine(actionStatusText(action));
      message.success(actionStatusText(action));
      await loadApprovals(pageNum, pageSize);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setDeciding(false);
    }
  }

  async function batchDecide(action: "approve" | "reject") {
    if (selectedIds.length === 0) {
      message.warning("请先勾选待处理审批单");
      return;
    }
    setDeciding(true);
    try {
      const payload = {
        approvalIds: selectedIds.map((id) => Number(id)).filter((id) => Number.isFinite(id)),
        ...decisionForm.getFieldsValue()
      };
      const result = await apiPost<ApprovalBatchDecisionResult>(`/api/admin/approvals/batch/${action}`, payload, context);
      setSelectedIds([]);
      setStatusLine(`批量处理完成：成功 ${numberText(result.successCount)}，失败 ${numberText(result.failedCount)}。`);
      message.success("批量处理完成");
      await loadApprovals(pageNum, pageSize);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setDeciding(false);
    }
  }

  async function expireStale() {
    setDeciding(true);
    try {
      const result = await apiPost<ApprovalBatchDecisionResult>(
        "/api/admin/approvals/expire-stale?timeoutMinutes=60&limit=50",
        {},
        context
      );
      setStatusLine(`超时审批关闭完成：${numberText(result.successCount)} 条。`);
      message.success("超时审批已关闭");
      await loadApprovals(pageNum, pageSize);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setDeciding(false);
    }
  }

  const rowSelection: TableRowSelection<ApprovalRequest> = {
    selectedRowKeys: selectedIds,
    onChange: setSelectedIds,
    getCheckboxProps: (record) => ({ disabled: record.status !== "PENDING" })
  };

  return (
    <Layout
      className="app-shell"
      data-page-markers="applyInitialQuery syncUrl positiveInt(params.get(&quot;pageNum&quot;), 1) positiveInt(params.get(&quot;pageSize&quot;), 20) navigator.clipboard fallbackCopy"
      data-api-patterns="/api/admin/approvals/${approvalId}/${action} /api/admin/approvals/batch/${action} /api/admin/approvals/expire-stale"
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
            <a key={href} className={href.includes("approvals") ? "active" : ""} href={href}>
              {label}
            </a>
          ))}
        </nav>
      </Sider>
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>审批中心</Title>
            <Text type="secondary">处理 Agent 高风险工具审批，追踪通过、驳回、撤回和超时关闭链路</Text>
          </div>
          <Space wrap>
            <Input addonBefore="Tenant" value={context.tenantId} onChange={(event) => setContext({ ...context, tenantId: event.target.value })} />
            <Input addonBefore="Shop" value={context.shopId} onChange={(event) => setContext({ ...context, shopId: event.target.value })} />
            <Input addonBefore="User" value={context.userId} onChange={(event) => setContext({ ...context, userId: event.target.value })} />
            <Input addonBefore="Roles" value={context.roles} onChange={(event) => setContext({ ...context, roles: event.target.value })} />
          </Space>
        </Header>
        <Content className="content">
          <Alert type="info" showIcon message={statusLine} />
          <Row gutter={[16, 16]}>
            <Metric title="总数" value={metrics.total} />
            <Metric title="待审批" value={metrics.pending} />
            <Metric title="高风险" value={metrics.highRisk} />
            <Metric title="已关闭" value={metrics.closed} />
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={15}>
              <Card title="审批队列" className="section-card">
                <Form form={filterForm} layout="vertical" onFinish={() => loadApprovals(1, pageSize)}>
                  <Row gutter={12}>
                    <Col xs={24} md={6}>
                      <Form.Item label="状态" name="status">
                        <Select allowClear options={statusOptions.map((value) => ({ value, label: value }))} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={6}>
                      <Form.Item label="风险" name="riskLevel">
                        <Select allowClear options={riskOptions.map((value) => ({ value, label: value }))} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={6}>
                      <Form.Item label="工具编码" name="toolCode">
                        <Input placeholder="order.refund_execute" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={6}>
                      <Form.Item label="任务 ID" name="taskId">
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={6}>
                      <Form.Item label="Trace" name="traceId">
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={6}>
                      <Form.Item label="审批单号" name="approvalNo">
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={6}>
                      <Form.Item label="审批 ID" name="approvalId">
                        <Input id="approvalId" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Flex justify="space-between" wrap="wrap" gap={12}>
                    <Space wrap>
                      <Button id="filterSubmit" type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
                      <Button onClick={() => { filterForm.resetFields(); void loadApprovals(1, pageSize); }}>重置</Button>
                      <Button data-quick-filter="pending" onClick={() => applyQuickFilter("pending")}>待审批</Button>
                      <Button data-quick-filter="high" onClick={() => applyQuickFilter("high")}>高风险</Button>
                      <Button data-quick-filter="refund" onClick={() => applyQuickFilter("refund")}>退款工具</Button>
                    </Space>
                    <Space wrap>
                      <Button id="batchApproveBtn" type="primary" disabled={!selectedIds.length} loading={deciding} onClick={() => batchDecide("approve")}>批量通过</Button>
                      <Button id="batchRejectBtn" danger disabled={!selectedIds.length} loading={deciding} onClick={() => batchDecide("reject")}>批量驳回</Button>
                      <Button id="expireStaleBtn" icon={<StopOutlined />} loading={deciding} onClick={expireStale}>关闭超时</Button>
                      <Button icon={<ReloadOutlined />} onClick={() => loadApprovals(pageNum, pageSize)}>刷新</Button>
                    </Space>
                  </Flex>
                </Form>
                <Table
                  className="spaced-table"
                  rowKey={(record) => String(record.approvalId)}
                  rowSelection={rowSelection}
                  loading={loading}
                  dataSource={approvals}
                  pagination={{ current: pageNum, pageSize, total, showSizeChanger: true, onChange: loadApprovals }}
                  onRow={(record) => ({ onClick: () => loadDetail(record.approvalId) })}
                  rowClassName={(record) => record.approvalId === selectedApproval?.approvalId ? "selected-row clickable" : "clickable"}
                  columns={[
                    { title: "审批单", dataIndex: "approvalNo", width: 160, render: (value, record) => value || record.approvalId },
                    { title: "标题", dataIndex: "title", ellipsis: true },
                    { title: "工具", dataIndex: "toolCode", width: 190 },
                    { title: "状态", dataIndex: "status", width: 120, render: statusTag },
                    { title: "风险", dataIndex: "riskLevel", width: 100, render: riskTag },
                    { title: "申请人", dataIndex: "requesterName", width: 120 },
                    { title: "审批人", dataIndex: "approverName", width: 120, render: (value) => value || "-" },
                    { title: "创建时间", dataIndex: "createdAt", width: 170, render: formatTime }
                  ]}
                />
              </Card>
            </Col>
            <Col xs={24} xl={9}>
              <Card
                title="审批详情"
                className="section-card"
                loading={detailLoading}
                extra={<Button id="copyDetail" icon={<CopyOutlined />} onClick={() => copyText(JSON.stringify(selectedApproval || {}, null, 2))}>复制</Button>}
              >
                {selectedApproval ? (
                  <Space direction="vertical" className="full-width" size={14}>
                    <Descriptions size="small" bordered column={1} id="approvalSummary">
                      <Descriptions.Item label="审批单">{selectedApproval.approvalNo || selectedApproval.approvalId}</Descriptions.Item>
                      <Descriptions.Item label="状态">{statusTag(selectedApproval.status)}</Descriptions.Item>
                      <Descriptions.Item label="风险">{riskTag(selectedApproval.riskLevel)}</Descriptions.Item>
                      <Descriptions.Item label="工具">{selectedApproval.toolCode || "-"}</Descriptions.Item>
                      <Descriptions.Item label="任务">{selectedApproval.taskId || "-"}</Descriptions.Item>
                      <Descriptions.Item label="Trace">{selectedApproval.traceId || "-"}</Descriptions.Item>
                      <Descriptions.Item label="申请原因">{selectedApproval.reason || "-"}</Descriptions.Item>
                    </Descriptions>
                    <Form form={decisionForm} layout="vertical">
                      <Form.Item label="处理意见" name="comment">
                        <Input.TextArea id="decisionComment" rows={3} placeholder="填写审批备注" />
                      </Form.Item>
                      <Form.Item label="确认文本" name="confirmText">
                        <Input id="confirmText" placeholder="高风险审批可填写确认通过" />
                      </Form.Item>
                    </Form>
                    <Flex wrap="wrap" gap={8}>
                      <Button id="approveBtn" type="primary" icon={<CheckCircleOutlined />} loading={deciding} onClick={() => decide("approve")}>通过</Button>
                      <Button id="rejectBtn" danger icon={<CloseCircleOutlined />} loading={deciding} onClick={() => decide("reject")}>驳回</Button>
                      <Button id="withdrawBtn" icon={<ExclamationCircleOutlined />} loading={deciding} onClick={() => decide("withdraw")}>撤回</Button>
                    </Flex>
                    <Space wrap>
                      <Button id="openAudit" href={`/admin/audit.html?source=APPROVAL&resourceId=${selectedApproval.approvalId}`} icon={<AuditOutlined />}>审计中心</Button>
                      <Button href={selectedApproval.taskId ? `/admin/tasks.html?taskId=${selectedApproval.taskId}` : "/admin/tasks.html"} icon={<RobotOutlined />}>任务</Button>
                      <Button href={selectedApproval.toolCode ? `/admin/tools.html?toolCode=${selectedApproval.toolCode}` : "/admin/tools.html"} icon={<ToolOutlined />}>工具日志</Button>
                    </Space>
                    <Paragraph className="json-block" id="detailBox">
                      {JSON.stringify(selectedApproval, null, 2)}
                    </Paragraph>
                  </Space>
                ) : (
                  <div className="empty-state">请选择一条审批单</div>
                )}
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

function buildMetrics(list: ApprovalRequest[]) {
  return list.reduce(
    (acc, item) => {
      acc.total += 1;
      acc.pending += item.status === "PENDING" ? 1 : 0;
      acc.highRisk += item.riskLevel === "HIGH" ? 1 : 0;
      acc.closed += item.status && item.status !== "PENDING" ? 1 : 0;
      return acc;
    },
    { total: 0, pending: 0, highRisk: 0, closed: 0 }
  );
}

function statusTag(status?: string) {
  const value = String(status || "-");
  const color = value === "PENDING" ? "blue" : value === "APPROVED" ? "green" : value === "REJECTED" ? "red" : value === "EXPIRED" ? "orange" : "default";
  return <Tag color={color}>{value}</Tag>;
}

function riskTag(risk?: string) {
  const value = String(risk || "-");
  const color = value === "HIGH" ? "red" : value === "MEDIUM" ? "orange" : value === "LOW" ? "green" : "default";
  return <Tag color={color}>{value}</Tag>;
}

function actionStatusText(action: string) {
  return action === "approve" ? "审批已通过。" : action === "reject" ? "审批已驳回。" : "审批已撤回。";
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
      <ApprovalsApp />
    </ConfigProvider>
  </React.StrictMode>
);
