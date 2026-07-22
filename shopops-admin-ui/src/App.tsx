import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
  List,
  Row,
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
  BarChartOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  RobotOutlined,
  ThunderboltOutlined
} from "@ant-design/icons";
import { apiGet, apiPost, readStoredContext, type RequestContext } from "./api";
import type { AgentStep, AgentTask, DataSourceEvidence, NaturalLanguageResult, OperationReport, PageResult } from "./types";
import { isTerminalStatus, moneyText, normalizeEvidence, numberText, parseOutput, percentText } from "./utils";
import { MetricsChart } from "./MetricsChart";

const { Header, Content, Sider } = Layout;
const { Text, Title, Paragraph } = Typography;

const OLIST_DEMO_DATE = "2018-08-07";
const OLIST_DEMO_PROMPT =
  "基于 Olist 真实订单和评价数据，生成 2018-08-07 店铺运营日报，重点分析 GMV、退款风险、差评原因和商品优化建议。";

const quickPrompts = [
  { title: "经营日报", prompt: "生成今天店铺运营日报，给出 GMV、退款、差评和商品建议。" },
  { title: "差评专项", prompt: "分析最近差评原因，找出高风险评价和需要优先处理的问题。" },
  { title: "商品优化", prompt: "找出低点击商品并给出标题、库存和运营优化建议。" },
  { title: "投放异常", prompt: "检查高消耗低转化投放计划，输出异常原因和调整建议。" }
];

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

export default function App() {
  const storedContext = readStoredContext();
  const [form] = Form.useForm();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [statusLine, setStatusLine] = useState("准备接收自然语言运营任务。");
  const [datasetMode, setDatasetMode] = useState("默认业务数据");
  const [submitting, setSubmitting] = useState(false);
  const [tracking, setTracking] = useState(false);
  const [understanding, setUnderstanding] = useState<NaturalLanguageResult | null>(null);
  const [task, setTask] = useState<AgentTask | null>(null);
  const [steps, setSteps] = useState<AgentStep[]>([]);
  const [report, setReport] = useState<OperationReport | null>(null);
  const [recentTasks, setRecentTasks] = useState<AgentTask[]>([]);
  const timerRef = useRef<number | null>(null);

  const selectedTaskId = task?.taskId || understanding?.task?.taskId || "";
  const selectedReportId = task?.reportId || report?.reportId || "";
  const evidence = useMemo(() => normalizeEvidence(report?.evidence), [report]);
  const toolOutputs = useMemo(() => outputByTool(steps), [steps]);
  const metrics = useMemo(() => buildMetrics(toolOutputs, evidence.dataSources), [toolOutputs, evidence.dataSources]);

  const loadRecentTasks = useCallback(async () => {
    try {
      const page = await apiGet<PageResult<AgentTask>>("/api/admin/agent/tasks?pageNum=1&pageSize=5", context);
      setRecentTasks(page.list || []);
    } catch (error) {
      console.warn(error);
    }
  }, [context]);

  const loadSteps = useCallback(
    async (taskId: string | number) => {
      const nextSteps = await apiGet<AgentStep[]>(`/api/agent/tasks/${encodeURIComponent(taskId)}/steps`, context);
      setSteps(nextSteps || []);
      return nextSteps || [];
    },
    [context]
  );

  const loadReport = useCallback(
    async (reportId: string | number) => {
      const nextReport = await apiGet<OperationReport>(`/api/reports/${encodeURIComponent(reportId)}`, context);
      setReport(nextReport);
      return nextReport;
    },
    [context]
  );

  const loadTask = useCallback(
    async (taskId: string | number) => {
      const nextTask = await apiGet<AgentTask>(`/api/agent/tasks/${encodeURIComponent(taskId)}`, context);
      setTask(nextTask);
      await loadSteps(taskId);
      if (nextTask.reportId) {
        await loadReport(nextTask.reportId);
      }
      if (isTerminalStatus(nextTask.status)) {
        setTracking(false);
        await loadRecentTasks();
      }
      return nextTask;
    },
    [context, loadRecentTasks, loadReport, loadSteps]
  );

  const stopTracking = useCallback(() => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }
    setTracking(false);
  }, []);

  const startTracking = useCallback(
    (taskId: string | number) => {
      stopTracking();
      setTracking(true);
      timerRef.current = window.setInterval(async () => {
        try {
          const nextTask = await loadTask(taskId);
          if (isTerminalStatus(nextTask.status)) {
            stopTracking();
          }
        } catch (error) {
          stopTracking();
          setStatusLine(`结果追踪中断：${errorMessage(error)}`);
        }
      }, 2000);
    },
    [loadTask, stopTracking]
  );

  useEffect(() => {
    form.setFieldsValue({ userInput: quickPrompts[0].prompt, startDate: today(), endDate: today() });
    void loadRecentTasks();
    return stopTracking;
  }, [form, loadRecentTasks, stopTracking]);

  async function submitNaturalLanguageTask(values: { userInput: string; startDate: string; endDate: string }) {
    setSubmitting(true);
    setStatusLine("Agent 正在理解诉求并创建任务。");
    try {
      const result = await apiPost<NaturalLanguageResult>(
        "/api/agent/tasks/natural-language",
        {
          userInput: values.userInput,
          dateRange: { start: values.startDate, end: values.endDate }
        },
        context
      );
      setUnderstanding(result);
      setTask(result.task);
      setReport(null);
      setStatusLine(`已路由到 ${result.taskType}：${result.routedReason || "规则路由完成"}`);
      const nextTask = await loadTask(result.task.taskId);
      if (!isTerminalStatus(nextTask.status)) {
        startTracking(result.task.taskId);
      }
      await loadRecentTasks();
    } catch (error) {
      setStatusLine(`任务创建失败：${errorMessage(error)}`);
      message.error(errorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  function applyOlistDemoMode() {
    form.setFieldsValue({
      startDate: OLIST_DEMO_DATE,
      endDate: OLIST_DEMO_DATE,
      userInput: OLIST_DEMO_PROMPT
    });
    setDatasetMode("Olist 真实数据演示");
    setStatusLine("已切换到 Olist 演示日期 2018-08-07。默认 Connector 会读取 Olist 订单、评价和商品候选数据。");
  }

  function selectRecentTask(nextTask: AgentTask) {
    setTask(nextTask);
    setUnderstanding(null);
    setReport(null);
    if (nextTask.taskId) {
      void loadTask(nextTask.taskId);
    }
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
            <a className={href.includes("workbench") ? "active" : ""} href={href} key={href}>
              {label}
            </a>
          ))}
        </nav>
      </Sider>
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>Agent 工作台</Title>
            <Text type="secondary">用自然语言发起日常运营任务，追踪工具编排、量化结果、报告与审计链路。</Text>
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
            <Col xs={24} xl={10}>
              <Space direction="vertical" size={16} className="full">
                <Card title="一句话发起任务" extra={<Tag color="blue">{datasetMode}</Tag>}>
                  <Form form={form} layout="vertical" onFinish={submitNaturalLanguageTask}>
                    <Form.Item name="userInput" label="自然语言任务" rules={[{ required: true, message: "请输入运营任务" }]}>
                      <Input.TextArea rows={6} placeholder="例如：生成今天店铺运营日报" />
                    </Form.Item>
                    <Row gutter={10}>
                      <Col span={12}>
                        <Form.Item name="startDate" label="开始日期" rules={[{ required: true }]}>
                          <Input type="date" />
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item name="endDate" label="结束日期" rules={[{ required: true }]}>
                          <Input type="date" />
                        </Form.Item>
                      </Col>
                    </Row>
                    <Flex justify="space-between" gap={8} wrap="wrap">
                      <Button id="useOlistDemo" icon={<ThunderboltOutlined />} onClick={applyOlistDemoMode}>
                        Olist 演示数据
                      </Button>
                      <Space>
                        <Button
                          icon={<ReloadOutlined />}
                          disabled={!selectedTaskId}
                          loading={tracking}
                          onClick={() => selectedTaskId && loadTask(selectedTaskId)}
                        >
                          刷新结果
                        </Button>
                        <Button type="primary" htmlType="submit" icon={<PlayCircleOutlined />} loading={submitting}>
                          启动 Agent
                        </Button>
                      </Space>
                    </Flex>
                  </Form>
                </Card>

                <Card title="快捷任务">
                  <Row gutter={[8, 8]}>
                    {quickPrompts.map((item) => (
                      <Col span={12} key={item.title}>
                        <Button block className="quick-button" onClick={() => form.setFieldsValue({ userInput: item.prompt })}>
                          <strong>{item.title}</strong>
                          <span>{item.prompt}</span>
                        </Button>
                      </Col>
                    ))}
                  </Row>
                </Card>

                <Card title="最近任务">
                  <List
                    dataSource={recentTasks}
                    locale={{ emptyText: "暂无最近任务" }}
                    renderItem={(item) => (
                      <List.Item className="clickable" onClick={() => selectRecentTask(item)}>
                        <List.Item.Meta title={item.taskNo || item.taskId} description={item.resultSummary || item.userInput || item.taskType} />
                        <StatusTag status={item.status} />
                      </List.Item>
                    )}
                  />
                </Card>
              </Space>
            </Col>

            <Col xs={24} xl={14}>
              <Space direction="vertical" size={16} className="full">
                <Alert type="info" showIcon message={statusLine} />
                <WorkbenchSummary understanding={understanding} task={task} report={report} evidence={evidence} />
                <Card title="Agent 执行步骤" extra={<StatusTag status={task?.status} />}>
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
                </Card>
                <Card title="量化结果" extra={<BarChartOutlined />}>
                  <Row gutter={[12, 12]}>
                    <Col xs={12} md={6}>
                      <Statistic title="GMV" value={moneyText(metrics.gmv)} />
                    </Col>
                    <Col xs={12} md={6}>
                      <Statistic title="退款率" value={percentText(metrics.refundRate)} />
                    </Col>
                    <Col xs={12} md={6}>
                      <Statistic title="风险评价" value={numberText(metrics.negativeCount)} />
                    </Col>
                    <Col xs={12} md={6}>
                      <Statistic title="商品候选" value={numberText(metrics.candidateCount)} />
                    </Col>
                  </Row>
                  <MetricsChart metrics={metrics} />
                  <SpecializedTables outputs={toolOutputs} />
                </Card>
                <Card
                  title="最终报告与建议"
                  extra={
                    <Space>
                      <Button href={selectedReportId ? `/admin/reports.html?reportId=${selectedReportId}` : "/admin/reports.html"} icon={<FileTextOutlined />}>
                        报告
                      </Button>
                      <Button href={selectedTaskId ? `/admin/audit.html?source=TASK&taskId=${selectedTaskId}` : "/admin/audit.html"} icon={<ApiOutlined />}>
                        审计
                      </Button>
                    </Space>
                  }
                >
                  <Paragraph className="markdown-preview">{report?.markdown || task?.resultSummary || "任务完成后会展示 Markdown 报告摘要和运营建议。"}</Paragraph>
                </Card>
              </Space>
            </Col>
          </Row>
        </Content>
      </Layout>
    </Layout>
  );
}

function WorkbenchSummary({
  understanding,
  task,
  report,
  evidence
}: {
  understanding: NaturalLanguageResult | null;
  task: AgentTask | null;
  report: OperationReport | null;
  evidence: ReturnType<typeof normalizeEvidence>;
}) {
  const dataSourceText = dataSourceEvidenceSummary(evidence.dataSources);
  return (
    <Card title="Agent 理解结果">
      <Descriptions size="small" column={{ xs: 1, md: 2 }} bordered>
        <Descriptions.Item label="意图">{understanding?.intentLabel || intentTitle(evidence.intent) || understanding?.intent || "-"}</Descriptions.Item>
        <Descriptions.Item label="置信度">{understanding ? `${(understanding.confidence * 100).toFixed(0)}%` : "-"}</Descriptions.Item>
        <Descriptions.Item label="任务">{task?.taskNo || task?.taskId || "-"}</Descriptions.Item>
        <Descriptions.Item label="报告">{report?.title || report?.reportId || "-"}</Descriptions.Item>
        <Descriptions.Item label="关注点" span={2}>
          <TagList values={understanding?.focusAreas} fallback="默认经营复盘" />
        </Descriptions.Item>
        <Descriptions.Item label="数据来源" span={2}>
          {dataSourceText}
        </Descriptions.Item>
        <Descriptions.Item label="建议动作" span={2}>
          <TagList values={understanding?.recommendedActions} fallback="等待 Agent 输出" />
        </Descriptions.Item>
      </Descriptions>
    </Card>
  );
}

function SpecializedTables({ outputs }: { outputs: Record<string, Record<string, unknown>> }) {
  const comments = outputs["comment.query_negative"] || {};
  const products = outputs["product.query_candidates"] || {};
  const ads = outputs["ad.query_performance"] || {};
  const riskComments = Array.isArray(comments.riskComments) ? comments.riskComments.slice(0, 3) : [];
  const productRows = Array.isArray(products.products) ? products.products.slice(0, 5) : [];
  const campaigns = Array.isArray(ads.campaigns) ? ads.campaigns.slice(0, 5) : [];

  return (
    <Space direction="vertical" size={14} className="full result-tables">
      {riskComments.length > 0 && (
        <Table
          size="small"
          pagination={false}
          title={() => "差评风险样本"}
          dataSource={riskComments as Record<string, unknown>[]}
          rowKey={(_, index) => `comment-${index}`}
          columns={[
            { title: "商品", dataIndex: "productName" },
            { title: "星级", dataIndex: "star", width: 80 },
            { title: "内容", dataIndex: "content" }
          ]}
        />
      )}
      {productRows.length > 0 && (
        <Table
          size="small"
          pagination={false}
          title={() => "商品优化候选"}
          dataSource={productRows as Record<string, unknown>[]}
          rowKey={(_, index) => `product-${index}`}
          columns={[
            { title: "商品", dataIndex: "productName" },
            { title: "评分", dataIndex: "score", width: 80 },
            { title: "库存", dataIndex: "stock", width: 80 },
            { title: "原因", dataIndex: "reason" }
          ]}
        />
      )}
      {campaigns.length > 0 && (
        <Table
          size="small"
          pagination={false}
          title={() => "投放计划表现"}
          dataSource={campaigns as Record<string, unknown>[]}
          rowKey={(_, index) => `campaign-${index}`}
          columns={[
            { title: "计划", dataIndex: "campaignName" },
            { title: "消耗", dataIndex: "spend", render: moneyText },
            { title: "ROI", dataIndex: "roi" }
          ]}
        />
      )}
    </Space>
  );
}

function TagList({ values, fallback }: { values?: string[]; fallback: string }) {
  const nextValues = values && values.length ? values : [fallback];
  return (
    <Space wrap>
      {nextValues.map((value) => (
        <Tag key={value}>{value}</Tag>
      ))}
    </Space>
  );
}

function StatusTag({ status }: { status?: string }) {
  const value = String(status || "-").toUpperCase();
  const color = value === "SUCCESS" ? "success" : value === "FAILED" || value === "DEGRADED" ? "error" : value === "-" ? "default" : "processing";
  return <Tag color={color}>{value}</Tag>;
}

function outputByTool(steps: AgentStep[]) {
  return steps.reduce<Record<string, Record<string, unknown>>>((outputs, step) => {
    if (step.toolCode) {
      outputs[step.toolCode] = parseOutput(step.output);
    }
    return outputs;
  }, {});
}

function buildMetrics(outputs: Record<string, Record<string, unknown>>, dataSources?: DataSourceEvidence) {
  const order = outputs["order.query_summary"] || dataSources?.orderSummary?.metrics || {};
  const comments = outputs["comment.query_negative"] || dataSources?.negativeComments?.metrics || {};
  const products = outputs["product.query_candidates"] || dataSources?.productCandidates?.metrics || {};
  const ads = outputs["ad.query_performance"] || dataSources?.adPerformance?.metrics || {};
  return {
    gmv: order.gmv,
    refundRate: order.refundRate,
    negativeCount: comments.negativeCount,
    candidateCount: products.candidateCount,
    roi: ads.roi
  };
}

function dataSourceEvidenceSummary(dataSources?: DataSourceEvidence) {
  if (!dataSources) {
    return "暂无数据来源快照";
  }
  const order = dataSources.orderSummary || {};
  const comments = dataSources.negativeComments || {};
  const products = dataSources.productCandidates || {};
  return [
    `${order.connectorCode || "order.unknown"}：GMV ${moneyText(order.metrics?.gmv)}，订单 ${numberText(order.metrics?.orderCount)}`,
    `${comments.connectorCode || "comment.unknown"}：风险评价 ${numberText(comments.metrics?.negativeCount)}`,
    `${products.connectorCode || "product.unknown"}：商品候选 ${numberText(products.metrics?.candidateCount)}`
  ].join("；");
}

function activeStepIndex(steps: AgentStep[]) {
  const runningIndex = steps.findIndex((step) => !isTerminalStatus(step.status));
  return runningIndex >= 0 ? runningIndex : Math.max(steps.length - 1, 0);
}

function stepStatus(status?: string): "wait" | "process" | "finish" | "error" {
  const value = String(status || "").toUpperCase();
  if (value === "SUCCESS") {
    return "finish";
  }
  if (value === "FAILED" || value === "DEGRADED") {
    return "error";
  }
  if (value === "RUNNING") {
    return "process";
  }
  return "wait";
}

function placeholderSteps(): AgentStep[] {
  return [
    { stepName: "自然语言路由", toolCode: "intent.route" },
    { stepName: "工具编排执行", toolCode: "mcp.tool.invoke" },
    { stepName: "报告生成", toolCode: "report.generate" }
  ];
}

function intentTitle(intent?: string) {
  if (intent === "comment_risk") return "店铺差评风险专项分析";
  if (intent === "product_optimization") return "店铺低点击商品优化专项";
  if (intent === "ad_anomaly") return "店铺投放异常专项检查";
  if (intent === "daily_review") return "店铺每日经营复盘";
  return "";
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function errorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return String(error);
}
