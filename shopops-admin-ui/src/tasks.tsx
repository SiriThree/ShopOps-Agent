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
  Steps,
  Table,
  Tag,
  Typography,
  message
} from "antd";
import {
  ApiOutlined,
  CopyOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  RetweetOutlined,
  RobotOutlined,
  SearchOutlined,
  ToolOutlined
} from "@ant-design/icons";
import { apiGet, apiPost, readStoredContext, type RequestContext } from "./api";
import type {
  AgentTask,
  AgentTaskCreateResult,
  AgentTaskDetail,
  AgentTaskEvent,
  AgentTaskMetrics,
  AgentTaskRecoveryResult,
  AgentStep,
  PageResult
} from "./types";
import { numberText, percentText } from "./utils";
import { TaskMetricsChart } from "./TaskMetricsChart";
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
  ["/admin/users.html", "组织"]
];

type TaskFilters = {
  status?: string;
  taskType?: string;
  taskNo?: string;
  userId?: string;
  traceId?: string;
  reportId?: string;
};

type CreateTaskForm = {
  taskType: string;
  userInput: string;
  startDate: string;
  endDate: string;
};

function TasksApp() {
  const storedContext = readStoredContext();
  const [filterForm] = Form.useForm<TaskFilters>();
  const [createForm] = Form.useForm<CreateTaskForm>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [tasks, setTasks] = useState<AgentTask[]>([]);
  const [metrics, setMetrics] = useState<AgentTaskMetrics | null>(null);
  const [events, setEvents] = useState<AgentTaskEvent[]>([]);
  const [selectedTaskId, setSelectedTaskId] = useState<string>("");
  const [detail, setDetail] = useState<AgentTaskDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [statusLine, setStatusLine] = useState("任务队列已就绪。");

  const selectedTask = detail?.task;
  const steps = useMemo(() => detail?.steps || [], [detail]);

  const buildQuery = useCallback(
    (nextPageNum = pageNum, nextPageSize = pageSize) => {
      const values = filterForm.getFieldsValue();
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "status", values.status);
      add(params, "taskType", values.taskType);
      add(params, "taskNo", values.taskNo);
      add(params, "userId", values.userId);
      add(params, "traceId", values.traceId);
      add(params, "reportId", values.reportId);
      add(params, "taskId", selectedTaskId);
      return params;
    },
    [filterForm, pageNum, pageSize, selectedTaskId]
  );

  const loadMetrics = useCallback(async () => {
    const nextMetrics = await apiGet<AgentTaskMetrics>("/api/admin/agent/tasks/metrics", context);
    setMetrics(nextMetrics);
  }, [context]);

  const loadEvents = useCallback(async () => {
    const page = await apiGet<PageResult<AgentTaskEvent>>("/api/admin/agent/tasks/events?pageNum=1&pageSize=8", context);
    setEvents(page.list || []);
  }, [context]);

  const loadDetail = useCallback(
    async (taskId: string | number) => {
      setDetailLoading(true);
      setSelectedTaskId(String(taskId));
      try {
        const nextDetail = await apiGet<AgentTaskDetail>(`/api/admin/agent/tasks/${encodeURIComponent(taskId)}/detail`, context);
        setDetail(nextDetail);
        setStatusLine(`已加载任务 ${nextDetail.task?.taskNo || taskId}`);
      } catch (error) {
        setStatusLine(`任务详情加载失败：${errorMessage(error)}`);
      } finally {
        setDetailLoading(false);
      }
    },
    [context]
  );

  const loadTasks = useCallback(
    async (nextPageNum = pageNum, nextPageSize = pageSize) => {
      setLoading(true);
      const params = buildQuery(nextPageNum, nextPageSize);
      window.history.replaceState(null, "", `${window.location.pathname}?${params.toString()}`);
      try {
        const page = await apiGet<PageResult<AgentTask> & { pageNum?: number; pageSize?: number }>(`/api/admin/agent/tasks?${params.toString()}`, context);
        const list = page.list || [];
        setTasks(list);
        setTotal(Number(page.total || 0));
        setPageNum(Number(page.pageNum || nextPageNum));
        setPageSize(Number(page.pageSize || nextPageSize));
        setStatusLine("任务列表已刷新。");
        if (selectedTaskId) {
          void loadDetail(selectedTaskId);
        } else if (list[0]?.taskId) {
          void loadDetail(list[0].taskId);
        }
      } catch (error) {
        setTasks([]);
        setTotal(0);
        setStatusLine(`任务加载失败：${errorMessage(error)}`);
      } finally {
        setLoading(false);
      }
    },
    [buildQuery, context, loadDetail, pageNum, pageSize, selectedTaskId]
  );

  const refreshAll = useCallback(async () => {
    setStatusLine("正在加载任务。");
    await Promise.all([loadMetrics(), loadEvents(), loadTasks()]);
    setStatusLine("任务已刷新。");
  }, [loadEvents, loadMetrics, loadTasks]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    filterForm.setFieldsValue({
      status: params.get("status") || undefined,
      taskType: params.get("taskType") || undefined,
      taskNo: params.get("taskNo") || undefined,
      userId: params.get("userId") || undefined,
      traceId: params.get("traceId") || undefined,
      reportId: params.get("reportId") || undefined
    });
    createForm.setFieldsValue({
      taskType: "daily_review",
      userInput: "根据所选日期范围生成每日运营复盘。",
      startDate: yesterday(),
      endDate: yesterday()
    });
    setSelectedTaskId(params.get("taskId") || "");
    setPageNum(positiveInt(params.get("pageNum"), 1));
    setPageSize(positiveInt(params.get("pageSize"), 20));
    void refreshAll();
  }, []);

  async function createTask(values: CreateTaskForm) {
    setActionLoading(true);
    setStatusLine("正在创建任务。");
    try {
      const result = await apiPost<AgentTaskCreateResult>(
        "/api/agent/tasks",
        {
          taskType: values.taskType,
          userInput: values.userInput,
          dateRange: { start: values.startDate, end: values.endDate }
        },
        context
      );
      setSelectedTaskId(String(result.taskId));
      await Promise.all([loadMetrics(), loadEvents(), loadTasks(1, pageSize)]);
      await loadDetail(result.taskId);
      setStatusLine(`已创建 ${result.taskNo || result.taskId}`);
    } catch (error) {
      setStatusLine(`创建失败：${errorMessage(error)}`);
      message.error(errorMessage(error));
    } finally {
      setActionLoading(false);
    }
  }

  async function retrySelectedTask() {
    if (!selectedTaskId) {
      message.warning("请先选择任务");
      return;
    }
    setActionLoading(true);
    try {
      const result = await apiPost<AgentTaskCreateResult>(`/api/agent/tasks/${encodeURIComponent(selectedTaskId)}/retry`, null, context);
      setSelectedTaskId(String(result.taskId));
      await Promise.all([loadMetrics(), loadEvents(), loadTasks(1, pageSize)]);
      await loadDetail(result.taskId);
      setStatusLine(`重试任务已创建 ${result.taskNo || result.taskId}`);
    } catch (error) {
      setStatusLine(`重试失败：${errorMessage(error)}`);
    } finally {
      setActionLoading(false);
    }
  }

  async function requeueStaleTasks() {
    setActionLoading(true);
    try {
      const result = await apiPost<AgentTaskRecoveryResult>(
        "/api/agent/tasks/stale/requeue?queuedTimeoutMinutes=10&runningTimeoutMinutes=30&limit=20",
        null,
        context
      );
      await refreshAll();
      setDetail({ task: undefined, steps: [], events: [], recoveryResult: result });
      setStatusLine(`已重排 ${result.requeuedCount || 0} 个超时任务。`);
    } catch (error) {
      setStatusLine(`重排失败：${errorMessage(error)}`);
    } finally {
      setActionLoading(false);
    }
  }

  function applyQuickFilter(name: string) {
    if (name === "clear") {
      filterForm.resetFields();
    } else {
      filterForm.setFieldsValue({ status: name.toUpperCase() });
    }
    setPageNum(1);
    void loadTasks(1, pageSize);
  }

  return (
    <Layout className="app-shell">
      <Sider width={232} className="sidebar">
        <div className="brand">
          <RobotOutlined />
          <div>
            <strong>ShopOps</strong>
            <span>Agent 运营平台</span>
          </div>
        </div>
        <nav className="nav">
          {navItems.map(([href, label]) => (
            <a className={href.includes("tasks") ? "active" : ""} href={href} key={href}>
              {label}
            </a>
          ))}
        </nav>
      </Sider>
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>任务队列</Title>
            <Text type="secondary">追踪 Agent 异步任务、执行步骤、失败重试、超时重排和配置快照。</Text>
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
                <Alert type="info" showIcon message={statusLine} />
                <Card title="任务指标" extra={<Button icon={<ReloadOutlined />} onClick={refreshAll} loading={loading}>刷新</Button>}>
                  <Row gutter={[12, 12]}>
                    <Col xs={12} md={6}><Statistic title="总数" value={metrics?.total || 0} /></Col>
                    <Col xs={12} md={6}><Statistic title="待执行" value={metrics?.statusBreakdown?.PENDING || 0} /></Col>
                    <Col xs={12} md={6}><Statistic title="运行中" value={metrics?.statusBreakdown?.RUNNING || 0} /></Col>
                    <Col xs={12} md={6}><Statistic title="失败" value={metrics?.failed || 0} /></Col>
                  </Row>
                  <TaskMetricsChart metrics={metrics} />
                </Card>
                <Card title="创建任务">
                  <Form form={createForm} layout="vertical" onFinish={createTask}>
                    <Row gutter={10}>
                      <Col xs={24} md={8}>
                        <Form.Item name="taskType" label="任务类型" rules={[{ required: true }]}>
                          <Select options={[{ value: "daily_review" }, { value: "comment_risk" }, { value: "product_optimization" }, { value: "ad_anomaly" }]} />
                        </Form.Item>
                      </Col>
                      <Col xs={12} md={8}>
                        <Form.Item name="startDate" label="开始日期" rules={[{ required: true }]}>
                          <Input type="date" />
                        </Form.Item>
                      </Col>
                      <Col xs={12} md={8}>
                        <Form.Item name="endDate" label="结束日期" rules={[{ required: true }]}>
                          <Input type="date" />
                        </Form.Item>
                      </Col>
                    </Row>
                    <Form.Item name="userInput" label="任务说明" rules={[{ required: true }]}>
                      <Input.TextArea rows={3} />
                    </Form.Item>
                    <Flex justify="end">
                      <Button id="createTaskSubmit" type="primary" htmlType="submit" icon={<PlayCircleOutlined />} loading={actionLoading}>
                        创建任务
                      </Button>
                    </Flex>
                  </Form>
                </Card>
                <Card title="任务筛选">
                  <Form form={filterForm} layout="vertical" onFinish={() => loadTasks(1, pageSize)}>
                    <Row gutter={10}>
                      <Col xs={24} md={8}>
                        <Form.Item name="status" label="状态">
                          <Select allowClear options={[{ value: "PENDING" }, { value: "RUNNING" }, { value: "SUCCESS" }, { value: "FAILED" }, { value: "DEGRADED" }]} />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item name="taskType" label="类型">
                          <Select allowClear options={[{ value: "daily_review" }, { value: "comment_risk" }, { value: "product_optimization" }, { value: "ad_anomaly" }]} />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}><Form.Item name="taskNo" label="任务编号"><Input /></Form.Item></Col>
                      <Col xs={24} md={8}><Form.Item name="userId" label="用户"><Input /></Form.Item></Col>
                      <Col xs={24} md={8}><Form.Item name="traceId" label="Trace"><Input /></Form.Item></Col>
                      <Col xs={24} md={8}><Form.Item name="reportId" label="报告"><Input /></Form.Item></Col>
                    </Row>
                    <Flex justify="space-between" gap={8} wrap="wrap">
                      <Space wrap>
                        <Button onClick={() => applyQuickFilter("running")}>运行中</Button>
                        <Button data-quick-filter="failed" onClick={() => applyQuickFilter("failed")}>失败</Button>
                        <Button onClick={() => applyQuickFilter("success")}>成功</Button>
                        <Button onClick={() => applyQuickFilter("clear")}>清空</Button>
                      </Space>
                      <Space>
                        <Button icon={<RetweetOutlined />} onClick={requeueStaleTasks} loading={actionLoading}>重排超时</Button>
                        <Button id="taskFilterSubmit" type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>查询</Button>
                      </Space>
                    </Flex>
                  </Form>
                </Card>
                <Card title="任务列表">
                  <Table
                    rowKey={(row) => String(row.taskId)}
                    loading={loading}
                    dataSource={tasks}
                    pagination={{
                      current: pageNum,
                      pageSize,
                      total,
                      showSizeChanger: true,
                      onChange: (nextPage, nextPageSize) => loadTasks(nextPage, nextPageSize)
                    }}
                    onRow={(row) => ({ onClick: () => row.taskId && loadDetail(row.taskId) })}
                    rowClassName={(row) => String(row.taskId) === String(selectedTaskId) ? "selected-row" : "clickable"}
                    columns={[
                      { title: "任务编号", dataIndex: "taskNo", render: (value, row) => value || row.taskId },
                      { title: "类型", dataIndex: "taskType", width: 150 },
                      { title: "状态", dataIndex: "status", width: 110, render: (value) => <StatusTag status={value} /> },
                      { title: "用户", dataIndex: "userId", width: 90 },
                      { title: "报告", dataIndex: "reportId", width: 90 },
                      { title: "Trace", dataIndex: "traceId", ellipsis: true },
                      { title: "创建时间", dataIndex: "createdAt", width: 180 },
                      { title: "摘要", dataIndex: "resultSummary", ellipsis: true, render: (value, row) => value || row.errorMessage || row.userInput }
                    ]}
                  />
                </Card>
              </Space>
            </Col>
            <Col xs={24} xl={9}>
              <Space direction="vertical" className="full" size={16}>
                <Card title="任务详情" loading={detailLoading} extra={<DetailLinks task={selectedTask} selectedTaskId={selectedTaskId} />}>
                  {detail ? (
                    <Space direction="vertical" className="full" size={14}>
                      <Descriptions size="small" column={1} bordered items={taskSummaryItems(selectedTask)} />
                      <ConfigSnapshot snapshot={detail.shopConfigSnapshot} />
                      <Steps
                        direction="vertical"
                        size="small"
                        current={activeStepIndex(steps)}
                        items={(steps.length ? steps : placeholderSteps()).map((step) => ({
                          title: step.stepName || "等待执行",
                          description: step.toolCode || "Agent 创建任务后会展示工具调用过程",
                          status: stepStatus(step.status)
                        }))}
                      />
                      <Flex gap={8} wrap="wrap">
                        <Button id="retryTask" icon={<RetweetOutlined />} onClick={retrySelectedTask} loading={actionLoading}>重试</Button>
                        <Button id="copyDetail" icon={<CopyOutlined />} onClick={() => copyText(JSON.stringify(detail, null, 2))}>复制详情</Button>
                      </Flex>
                      <pre className="json-box">{JSON.stringify(detail, null, 2)}</pre>
                    </Space>
                  ) : (
                    <div className="empty-state">
                      <strong>暂无任务详情</strong>
                      <span>选择左侧任务，或创建一个新的 Agent 任务。</span>
                    </div>
                  )}
                </Card>
                <Card title="最近事件">
                  <Table
                    size="small"
                    pagination={false}
                    rowKey={(row, index) => String(row.eventId || `${row.taskId}-${index}`)}
                    dataSource={events}
                    columns={[
                      { title: "时间", dataIndex: "createdAt", width: 170 },
                      { title: "事件", dataIndex: "eventType" },
                      { title: "状态", dataIndex: "toStatus", render: (value, row) => <StatusTag status={value || row.eventStatus} /> }
                    ]}
                  />
                </Card>
              </Space>
            </Col>
          </Row>
        </Content>
      </Layout>
    </Layout>
  );
}

function DetailLinks({ task, selectedTaskId }: { task?: AgentTask; selectedTaskId: string }) {
  const taskId = task?.taskId || selectedTaskId;
  return (
    <Space>
      <Button id="openReport" href={task?.reportId ? `/admin/reports.html?reportId=${task.reportId}` : "/admin/reports.html"} icon={<FileTextOutlined />}>报告</Button>
      <Button id="openAudit" href={taskId ? `/admin/audit.html?source=TASK&taskId=${taskId}` : "/admin/audit.html"} icon={<ApiOutlined />}>审计</Button>
      <Button id="openToolLogs" href={taskId ? `/admin/tools.html?taskId=${taskId}` : "/admin/tools.html"} icon={<ToolOutlined />}>工具</Button>
    </Space>
  );
}

function ConfigSnapshot({ snapshot }: { snapshot?: Record<string, unknown> }) {
  if (!snapshot) {
    return null;
  }
  return (
    <Descriptions title="店铺配置快照" size="small" column={1} bordered>
      <Descriptions.Item label="退款率预警阈值">{percentText(snapshot.refundRateWarnThreshold)}</Descriptions.Item>
      <Descriptions.Item label="差评预警阈值">{numberText(snapshot.negativeCommentWarnThreshold)}</Descriptions.Item>
      <Descriptions.Item label="高风险工具审批">{approvalText(snapshot.agentToolApprovalEnabled)}</Descriptions.Item>
      <Descriptions.Item label="模型策略">{String(snapshot.agentModelPolicy || "-")}</Descriptions.Item>
    </Descriptions>
  );
}

function taskSummaryItems(task?: AgentTask) {
  return [
    { key: "task", label: "任务", children: task?.taskNo || task?.taskId || "-" },
    { key: "status", label: "状态", children: <StatusTag status={task?.status} /> },
    { key: "type", label: "类型", children: task?.taskType || "-" },
    { key: "report", label: "报告", children: task?.reportId || "-" },
    { key: "trace", label: "Trace", children: task?.traceId || "-" },
    { key: "summary", label: "摘要", children: task?.resultSummary || task?.errorMessage || task?.userInput || "-" }
  ];
}

function StatusTag({ status }: { status?: string }) {
  const value = String(status || "-").toUpperCase();
  const color = value === "SUCCESS" ? "success" : value === "FAILED" || value === "DEGRADED" ? "error" : value === "-" ? "default" : "processing";
  return <Tag color={color}>{value}</Tag>;
}

function activeStepIndex(steps: AgentStep[]) {
  const runningIndex = steps.findIndex((step) => !["SUCCESS", "FAILED", "DEGRADED"].includes(String(step.status || "").toUpperCase()));
  return runningIndex >= 0 ? runningIndex : Math.max(steps.length - 1, 0);
}

function stepStatus(status?: string): "wait" | "process" | "finish" | "error" {
  const value = String(status || "").toUpperCase();
  if (value === "SUCCESS") return "finish";
  if (value === "FAILED" || value === "DEGRADED") return "error";
  if (value === "RUNNING") return "process";
  return "wait";
}

function placeholderSteps(): AgentStep[] {
  return [
    { stepName: "任务排队", toolCode: "task.queue" },
    { stepName: "工具编排执行", toolCode: "mcp.tool.invoke" },
    { stepName: "报告生成", toolCode: "report.generate" }
  ];
}

function approvalText(value: unknown) {
  if (value === true || value === "true") return "需要审批";
  if (value === false || value === "false") return "配置关闭";
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

function yesterday() {
  return new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
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
      <TasksApp />
    </ConfigProvider>
  </React.StrictMode>
);
