import React, { useCallback, useEffect, useMemo, useState } from "react";
import ReactDOM from "react-dom/client";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  Flex,
  Form,
  Input,
  Layout,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message
} from "antd";
import {
  AuditOutlined,
  CheckCircleOutlined,
  CopyOutlined,
  DashboardOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  RobotOutlined,
  SaveOutlined,
  SearchOutlined
} from "@ant-design/icons";
import { apiGet, apiPost, readStoredContext, type RequestContext } from "./api";
import type { ModelCallLog, PageResult, PromptRenderResult, PromptTemplate } from "./types";
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
  ["/admin/users.html", "组织"]
];

const promptQuickOptions = [
  { code: "daily_review.plan", name: "日报任务规划", taskType: "daily_review" },
  { code: "daily_review.report", name: "日报报告生成", taskType: "daily_review" },
  { code: "daily_review.summary", name: "经营摘要", taskType: "daily_review" }
];

type PromptFilters = {
  promptCode?: string;
  taskType?: string;
  status?: string;
};

type VersionForm = {
  promptCode?: string;
  promptName?: string;
  version?: string;
  taskType?: string;
  templateContent?: string;
  active?: boolean;
};

type RenderForm = {
  prompt?: string;
  variables?: string;
};

function PromptsApp() {
  const storedContext = readStoredContext();
  const [filterForm] = Form.useForm<PromptFilters>();
  const [versionForm] = Form.useForm<VersionForm>();
  const [renderForm] = Form.useForm<RenderForm>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [prompts, setPrompts] = useState<PromptTemplate[]>([]);
  const [modelLogs, setModelLogs] = useState<ModelCallLog[]>([]);
  const [selectedPrompt, setSelectedPrompt] = useState<PromptTemplate | null>(null);
  const [renderResult, setRenderResult] = useState<PromptRenderResult | null>(null);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [rendering, setRendering] = useState(false);
  const [logLoading, setLogLoading] = useState(false);
  const [statusLine, setStatusLine] = useState("Prompt 控制台已就绪。");

  const activeCount = useMemo(() => prompts.filter((item) => item.status === "ACTIVE").length, [prompts]);

  const buildQuery = useCallback(
    (nextPageNum = pageNum, nextPageSize = pageSize) => {
      const values = filterForm.getFieldsValue();
      const params = new URLSearchParams();
      params.set("pageNum", String(nextPageNum));
      params.set("pageSize", String(nextPageSize));
      add(params, "promptCode", values.promptCode);
      add(params, "taskType", values.taskType);
      add(params, "status", values.status);
      return params;
    },
    [filterForm, pageNum, pageSize]
  );

  const syncUrl = useCallback((params: URLSearchParams, prompt?: PromptTemplate | null) => {
    const next = new URLSearchParams(params);
    if (prompt?.promptCode) {
      next.set("promptCode", prompt.promptCode);
    }
    if (prompt?.version) {
      next.set("version", prompt.version);
    }
    window.history.replaceState(null, "", `${window.location.pathname}?${next.toString()}`);
  }, []);

  const loadModelLogs = useCallback(
    async (prompt?: PromptTemplate | null) => {
      setLogLoading(true);
      try {
        const params = new URLSearchParams();
        params.set("pageNum", "1");
        params.set("pageSize", "8");
        if (prompt?.promptCode) {
          params.set("promptCode", prompt.promptCode);
        }
        if (prompt?.version) {
          params.set("promptVersion", prompt.version);
        }
        const data = await apiGet<PageResult<ModelCallLog>>(`/api/admin/model-gateway/call-logs?${params}`, context);
        setModelLogs(data.list || []);
      } catch (error) {
        message.error(errorMessage(error));
      } finally {
        setLogLoading(false);
      }
    },
    [context]
  );

  const fillEditor = useCallback(
    (prompt: PromptTemplate) => {
      versionForm.setFieldsValue({
        promptCode: prompt.promptCode,
        promptName: prompt.promptName,
        version: prompt.version,
        taskType: prompt.taskType,
        templateContent: prompt.templateContent,
        active: prompt.status === "ACTIVE"
      });
      renderForm.setFieldsValue({
        prompt: "关注退款风险和差评原因",
        variables: JSON.stringify({ shopName: "杭州一店", date: "2026-07-20" }, null, 2)
      });
    },
    [renderForm, versionForm]
  );

  const loadDetail = useCallback(
    async (promptCode: string, version?: string) => {
      if (!promptCode) {
        return;
      }
      try {
        const suffix = version ? `?version=${encodeURIComponent(version)}` : "";
        const prompt = await apiGet<PromptTemplate>(`/api/admin/prompts/${encodeURIComponent(promptCode)}${suffix}`, context);
        setSelectedPrompt(prompt);
        fillEditor(prompt);
        setStatusLine(`已选中 ${prompt.promptCode}@${prompt.version}。`);
        syncUrl(buildQuery(), prompt);
        void loadModelLogs(prompt);
      } catch (error) {
        message.error(errorMessage(error));
      }
    },
    [buildQuery, context, fillEditor, loadModelLogs, syncUrl]
  );

  const loadPrompts = useCallback(
    async (nextPageNum = pageNum, nextPageSize = pageSize) => {
      setLoading(true);
      const params = buildQuery(nextPageNum, nextPageSize);
      try {
        const data = await apiGet<PageResult<PromptTemplate>>(`/api/admin/prompts?${params}`, context);
        const list = data.list || [];
        setPrompts(list);
        setTotal(data.total || 0);
        setPageNum(nextPageNum);
        setPageSize(nextPageSize);
        syncUrl(params, selectedPrompt);
        if (!selectedPrompt && list[0]?.promptCode) {
          void loadDetail(list[0].promptCode, list[0].version);
        }
      } catch (error) {
        message.error(errorMessage(error));
      } finally {
        setLoading(false);
      }
    },
    [buildQuery, context, loadDetail, pageNum, pageSize, selectedPrompt, syncUrl]
  );

  const applyInitialQuery = useCallback(() => {
    const params = new URLSearchParams(window.location.search);
    const promptCode = params.get("promptCode") || "";
    const version = params.get("version") || "";
    filterForm.setFieldsValue({
      promptCode,
      taskType: params.get("taskType") || undefined,
      status: params.get("status") || undefined
    });
    if (promptCode) {
      void loadDetail(promptCode, version);
    }
  }, [filterForm, loadDetail]);

  useEffect(() => {
    const defaultOption = promptQuickOptions[1];
    versionForm.setFieldsValue({
      promptCode: defaultOption.code,
      promptName: defaultOption.name,
      version: "v1",
      taskType: defaultOption.taskType,
      templateContent: "请基于以下输入生成中文经营复盘：{{prompt}}",
      active: false
    });
    renderForm.setFieldsValue({
      prompt: "关注退款风险和差评原因",
      variables: JSON.stringify({ shopName: "杭州一店", date: "2026-07-20" }, null, 2)
    });
    applyInitialQuery();
    void loadPrompts(1, pageSize);
  }, []);

  async function saveVersion(values: VersionForm) {
    const promptCode = String(values.promptCode || "").trim();
    if (!promptCode) {
      message.error("请填写 Prompt Code");
      return;
    }
    setSaving(true);
    try {
      const payload = {
        promptName: values.promptName,
        taskType: values.taskType,
        templateContent: values.templateContent,
        version: values.version,
        active: Boolean(values.active)
      };
      const saved = await apiPost<PromptTemplate>(`/api/admin/prompts/${encodeURIComponent(promptCode)}/versions`, payload, context);
      setSelectedPrompt(saved);
      fillEditor(saved);
      setStatusLine(`已创建版本 ${saved.promptCode}@${saved.version}。`);
      message.success("版本已保存");
      await loadPrompts(1, pageSize);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  async function enableVersion() {
    const values = versionForm.getFieldsValue();
    const promptCode = String(values.promptCode || "").trim();
    const version = String(values.version || "").trim();
    if (!promptCode || !version) {
      message.error("请先选择或填写版本");
      return;
    }
    setSaving(true);
    try {
      const enabled = await apiPost<PromptTemplate>(
        `/api/admin/prompts/${encodeURIComponent(promptCode)}/enable`,
        { version },
        context
      );
      setSelectedPrompt(enabled);
      fillEditor(enabled);
      setStatusLine(`已启用 ${enabled.promptCode}@${enabled.version}。`);
      message.success("版本已启用");
      await loadPrompts(pageNum, pageSize);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  async function renderTest() {
    const values = versionForm.getFieldsValue();
    const renderValues = renderForm.getFieldsValue();
    const promptCode = String(values.promptCode || "").trim();
    if (!promptCode) {
      message.error("请先选择 Prompt");
      return;
    }
    setRendering(true);
    try {
      const rendered = await apiPost<PromptRenderResult>(
        `/api/admin/prompts/${encodeURIComponent(promptCode)}/render-test`,
        {
          version: values.version,
          prompt: renderValues.prompt,
          variables: parseJson(renderValues.variables || "{}")
        },
        context
      );
      setRenderResult(rendered);
      setStatusLine(`渲染完成：${rendered.promptCode}@${rendered.version}。`);
      message.success("渲染测试完成");
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setRendering(false);
    }
  }

  function selectQuick(option: (typeof promptQuickOptions)[number]) {
    filterForm.setFieldsValue({ promptCode: option.code, taskType: option.taskType });
    versionForm.setFieldsValue({
      promptCode: option.code,
      promptName: option.name,
      taskType: option.taskType
    });
    void loadDetail(option.code);
  }

  function resetFilters() {
    filterForm.resetFields();
    void loadPrompts(1, pageSize);
  }

  return (
    <Layout className="app-shell" data-page-markers="applyInitialQuery syncUrl navigator.clipboard withBusy">
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
            <a key={href} className={href.includes("prompts") ? "active" : ""} href={href}>
              {label}
            </a>
          ))}
        </nav>
      </Sider>
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>提示词与模型网关</Title>
            <Text type="secondary">管理 Agent Planner / Report Prompt 版本，验证渲染结果并查看模型调用日志</Text>
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
            <Col xs={24} xl={14}>
              <Card title="模板版本" className="section-card" extra={<Tag color="blue">ACTIVE {numberText(activeCount)}</Tag>}>
                <Form form={filterForm} layout="vertical" onFinish={() => loadPrompts(1, pageSize)}>
                  <Row gutter={12}>
                    <Col xs={24} md={8}>
                      <Form.Item label="Prompt Code" name="promptCode">
                        <Input placeholder="daily_review.report" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={8}>
                      <Form.Item label="任务类型" name="taskType">
                        <Input placeholder="daily_review" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={8}>
                      <Form.Item label="状态" name="status">
                        <Select allowClear options={["ACTIVE", "INACTIVE"].map((value) => ({ value, label: value }))} />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Flex justify="space-between" wrap="wrap" gap={12}>
                    <Space wrap>
                      <Button id="filterSubmit" type="primary" htmlType="submit" icon={<SearchOutlined />}>
                        查询
                      </Button>
                      <Button onClick={resetFilters}>重置</Button>
                      {promptQuickOptions.map((option) => (
                        <Button key={option.code} data-prompt-quick={option.code} onClick={() => selectQuick(option)}>
                          {option.name}
                        </Button>
                      ))}
                    </Space>
                    <Button icon={<ReloadOutlined />} onClick={() => loadPrompts(pageNum, pageSize)}>
                      刷新
                    </Button>
                  </Flex>
                </Form>
                <Table
                  className="spaced-table"
                  rowKey={(record) => `${record.promptCode}-${record.version}`}
                  loading={loading}
                  dataSource={prompts}
                  pagination={{
                    current: pageNum,
                    pageSize,
                    total,
                    showSizeChanger: true,
                    onChange: loadPrompts
                  }}
                  onRow={(record) => ({
                    onClick: () => loadDetail(record.promptCode || "", record.version)
                  })}
                  rowClassName={(record) =>
                    record.promptCode === selectedPrompt?.promptCode && record.version === selectedPrompt?.version
                      ? "selected-row clickable"
                      : "clickable"
                  }
                  columns={[
                    { title: "Prompt Code", dataIndex: "promptCode" },
                    { title: "名称", dataIndex: "promptName" },
                    { title: "版本", dataIndex: "version", width: 90 },
                    { title: "任务类型", dataIndex: "taskType", width: 140 },
                    { title: "状态", dataIndex: "status", width: 110, render: statusTag },
                    { title: "更新时间", dataIndex: "updatedAt", width: 170, render: formatTime }
                  ]}
                />
              </Card>
            </Col>
            <Col xs={24} xl={10}>
              <Card title="版本编辑" className="section-card">
                <Form form={versionForm} layout="vertical" onFinish={saveVersion}>
                  <Row gutter={12}>
                    <Col xs={24} md={12}>
                      <Form.Item label="Prompt Code" name="promptCode" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item label="版本" name="version" rules={[{ required: true }]}>
                        <Input placeholder="v1" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.Item label="名称" name="promptName" rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                  <Form.Item label="任务类型" name="taskType">
                    <Input />
                  </Form.Item>
                  <Form.Item label="模板内容" name="templateContent" rules={[{ required: true }]}>
                    <Input.TextArea rows={8} spellCheck={false} />
                  </Form.Item>
                  <Flex justify="space-between" wrap="wrap" gap={12}>
                    <Form.Item name="active" valuePropName="checked" className="inline-form-item">
                      <Checkbox>创建后立即启用</Checkbox>
                    </Form.Item>
                    <Space wrap>
                      <Button icon={<CheckCircleOutlined />} loading={saving} onClick={enableVersion}>
                        启用
                      </Button>
                      <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
                        创建版本
                      </Button>
                    </Space>
                  </Flex>
                </Form>
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={12}>
              <Card
                title="渲染测试"
                className="section-card"
                extra={
                  <Button id="copyRendered" icon={<CopyOutlined />} onClick={() => copyText(renderResult?.renderedPrompt || "")}>
                    复制结果
                  </Button>
                }
              >
                <Form form={renderForm} layout="vertical">
                  <Form.Item label="业务输入 prompt" name="prompt">
                    <Input.TextArea rows={4} spellCheck={false} />
                  </Form.Item>
                  <Form.Item label="变量 JSON" name="variables">
                    <Input.TextArea rows={4} spellCheck={false} />
                  </Form.Item>
                  <Button id="renderTest" type="primary" icon={<PlayCircleOutlined />} loading={rendering} onClick={renderTest}>
                    渲染测试
                  </Button>
                </Form>
                <Paragraph className="markdown-preview rendered-preview">
                  {renderResult?.renderedPrompt || "等待渲染结果。"}
                </Paragraph>
              </Card>
            </Col>
            <Col xs={24} xl={12}>
              <Card
                title="模型调用日志"
                className="section-card"
                loading={logLoading}
                extra={<Button href="/admin/reports.html" icon={<FileTextOutlined />}>报告</Button>}
              >
                <Descriptions size="small" bordered column={2} id="selectedPromptSummary">
                  <Descriptions.Item label="Prompt">{selectedPrompt?.promptCode || "-"}</Descriptions.Item>
                  <Descriptions.Item label="版本">{selectedPrompt?.version || "-"}</Descriptions.Item>
                  <Descriptions.Item label="状态">{statusTag(selectedPrompt?.status)}</Descriptions.Item>
                  <Descriptions.Item label="调用数">{numberText(modelLogs.length)}</Descriptions.Item>
                </Descriptions>
                <Table
                  className="spaced-table"
                  rowKey={(record) => String(record.callId || `${record.traceId}-${record.createdAt}`)}
                  pagination={false}
                  dataSource={modelLogs}
                  columns={[
                    { title: "Call ID", dataIndex: "callId", width: 90 },
                    { title: "Provider", dataIndex: "providerCode", width: 110 },
                    { title: "模型", dataIndex: "modelName", width: 130 },
                    { title: "状态", dataIndex: "status", width: 110, render: statusTag },
                    { title: "Token", dataIndex: "totalTokens", width: 90, render: numberText },
                    { title: "耗时", dataIndex: "latencyMs", width: 90, render: (value) => `${numberText(value)} ms` },
                    { title: "时间", dataIndex: "createdAt", width: 170, render: formatTime }
                  ]}
                />
                <Space wrap>
                  <Button href="/admin/dashboard.html" icon={<DashboardOutlined />}>Dashboard</Button>
                  <Button href="/admin/audit.html" icon={<AuditOutlined />}>审计中心</Button>
                </Space>
              </Card>
            </Col>
          </Row>
        </Content>
      </Layout>
    </Layout>
  );
}

function statusTag(status?: string) {
  const value = String(status || "-");
  const color = value === "ACTIVE" || value === "SUCCESS" ? "green" : value === "FAILED" ? "red" : "default";
  return <Tag color={color}>{value}</Tag>;
}

function add(params: URLSearchParams, key: string, value?: string) {
  if (value && value.trim()) {
    params.set(key, value.trim());
  }
}

function parseJson(text: string) {
  if (!text.trim()) {
    return {};
  }
  return JSON.parse(text);
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
      <PromptsApp />
    </ConfigProvider>
  </React.StrictMode>
);
