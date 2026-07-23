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
import {
  ReloadOutlined,
  RobotOutlined,
  SaveOutlined,
  ShopOutlined,
  TeamOutlined,
  UserAddOutlined
} from "@ant-design/icons";
import { apiGet, apiPost, readStoredContext, type RequestContext } from "./api";
import type {
  OrganizationOverview,
  OrganizationUser,
  PageResult,
  Shop,
  ShopConfig,
  ShopMember,
  Tenant
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
  ["/admin/users.html", "组织"]
];

const statusOptions = ["ACTIVE", "DISABLED"];
const tenantRoleOptions = ["TENANT_ADMIN", "TENANT_OPERATOR", "TENANT_VIEWER"];
const shopRoleOptions = ["SHOP_ADMIN", "SHOP_OPERATOR", "SHOP_VIEWER"];
const modelPolicyOptions = ["conservative", "balanced", "aggressive"];
const defaultShopConfigs = [
  { configKey: "refund_rate_warn_threshold", configValue: "0.08", valueType: "number" },
  { configKey: "negative_comment_warn_threshold", configValue: "10", valueType: "number" },
  { configKey: "agent_tool_approval_enabled", configValue: "true", valueType: "boolean" },
  { configKey: "agent_model_policy", configValue: "balanced", valueType: "string" }
];

type UserForm = {
  username?: string;
  displayName?: string;
  phone?: string;
  email?: string;
  password?: string;
  tenantRole?: string;
  shopRole?: string;
  status?: string;
};

type PasswordForm = {
  userId?: string;
  password?: string;
};

type TenantForm = {
  tenantId?: string;
  tenantNo?: string;
  tenantName?: string;
  status?: string;
  planType?: string;
  contactName?: string;
  contactPhone?: string;
};

type ShopForm = {
  shopId?: string;
  shopNo?: string;
  shopName?: string;
  platformType?: string;
  ownerId?: string;
  status?: string;
};

type MemberForm = {
  shopId?: string;
  userId?: string;
  roleCode?: string;
  status?: string;
};

type MemberUpdateForm = {
  memberId?: string;
  roleCode?: string;
  status?: string;
};

type ConfigForm = {
  shopId?: string;
  refundRateWarnThreshold?: string;
  negativeCommentWarnThreshold?: string;
  agentToolApprovalEnabled?: string;
  agentModelPolicy?: string;
};

function UsersApp() {
  const storedContext = readStoredContext();
  const [userForm] = Form.useForm<UserForm>();
  const [passwordForm] = Form.useForm<PasswordForm>();
  const [tenantForm] = Form.useForm<TenantForm>();
  const [shopForm] = Form.useForm<ShopForm>();
  const [memberForm] = Form.useForm<MemberForm>();
  const [memberUpdateForm] = Form.useForm<MemberUpdateForm>();
  const [configForm] = Form.useForm<ConfigForm>();
  const [context, setContext] = useState<RequestContext>({
    tenantId: storedContext.tenantId || "1",
    shopId: storedContext.shopId || "1",
    userId: storedContext.userId || "1",
    roles: storedContext.roles || "ADMIN,OPERATOR"
  });
  const [overview, setOverview] = useState<OrganizationOverview>({});
  const [users, setUsers] = useState<OrganizationUser[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [shops, setShops] = useState<Shop[]>([]);
  const [members, setMembers] = useState<ShopMember[]>([]);
  const [configs, setConfigs] = useState<ShopConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusLine, setStatusLine] = useState("组织中心已就绪，可管理用户、租户、店铺、成员权限和店铺配置。");

  const selectedConfigShopId = Form.useWatch("shopId", configForm) || context.shopId;
  const selectedMemberShopId = Form.useWatch("shopId", memberForm) || context.shopId;

  const userOptions = useMemo(
    () => users.map((user) => ({ value: String(user.userId), label: `${user.username || user.userId} / ${user.displayName || "-"}` })),
    [users]
  );
  const shopOptions = useMemo(
    () => shops.map((shop) => ({ value: String(shop.shopId), label: `${shop.shopName || shop.shopId} / ${shop.shopNo || "-"}` })),
    [shops]
  );

  const loadOverview = useCallback(async () => {
    setOverview(await apiGet<OrganizationOverview>("/api/admin/organization/overview", context));
  }, [context]);

  const loadUsers = useCallback(async () => {
    const data = await apiGet<PageResult<OrganizationUser>>("/api/admin/organization/users?pageNum=1&pageSize=50", context);
    setUsers(data.list || []);
  }, [context]);

  const loadTenants = useCallback(async () => {
    const data = await apiGet<PageResult<Tenant>>("/api/admin/organization/tenants?pageNum=1&pageSize=50", context);
    setTenants(data.list || []);
  }, [context]);

  const loadShops = useCallback(async () => {
    const data = await apiGet<PageResult<Shop>>("/api/admin/organization/shops?pageNum=1&pageSize=50", context);
    setShops(data.list || []);
  }, [context]);

  const loadMembers = useCallback(async () => {
    const data = await apiGet<PageResult<ShopMember>>("/api/admin/organization/shop-members?pageNum=1&pageSize=50", context);
    setMembers(data.list || []);
  }, [context]);

  const loadConfigs = useCallback(
    async (shopId = selectedConfigShopId) => {
      if (!shopId) {
        return;
      }
      const data = await apiGet<PageResult<ShopConfig>>(`/api/admin/organization/shops/${encodeURIComponent(String(shopId))}/configs?pageNum=1&pageSize=50`, context);
      const list = data.list || [];
      setConfigs(list);
      configForm.setFieldsValue(formValuesFromConfigs(String(shopId), list));
    },
    [configForm, context, selectedConfigShopId]
  );

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([loadOverview(), loadUsers(), loadTenants(), loadShops(), loadMembers()]);
      await loadConfigs(selectedConfigShopId || "1");
      setStatusLine("组织目录、店铺成员和店铺配置已刷新。");
    } catch (error) {
      setStatusLine(`组织数据加载失败：${errorMessage(error)}`);
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [loadConfigs, loadMembers, loadOverview, loadShops, loadTenants, loadUsers, selectedConfigShopId]);

  useEffect(() => {
    configForm.setFieldsValue({ shopId: context.shopId });
    memberForm.setFieldsValue({ shopId: context.shopId, roleCode: "SHOP_OPERATOR", status: "ACTIVE" });
    userForm.setFieldsValue({ tenantRole: "TENANT_OPERATOR", shopRole: "SHOP_OPERATOR", status: "ACTIVE" });
    tenantForm.setFieldsValue({ status: "ACTIVE", planType: "demo" });
    shopForm.setFieldsValue({ platformType: "olist", status: "ACTIVE" });
    memberUpdateForm.setFieldsValue({ roleCode: "SHOP_OPERATOR", status: "ACTIVE" });
    void loadAll();
  }, []);

  async function createUser() {
    const values = await userForm.validateFields();
    setLoading(true);
    try {
      const user = await apiPost<OrganizationUser>("/api/admin/organization/users", values, context);
      setStatusLine(`用户已创建：${user.username || values.username}`);
      userForm.setFieldsValue({ password: "" });
      await Promise.all([loadOverview(), loadUsers(), loadMembers()]);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function resetPassword() {
    const values = await passwordForm.validateFields();
    setLoading(true);
    try {
      await apiPost<OrganizationUser>(`/api/admin/organization/users/${encodeURIComponent(String(values.userId))}/password`, { password: values.password }, context);
      setStatusLine(`用户密码已重置：${values.userId}`);
      passwordForm.setFieldsValue({ password: "" });
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function saveTenant() {
    const values = await tenantForm.validateFields();
    const tenantId = values.tenantId?.trim();
    setLoading(true);
    try {
      const path = tenantId ? `/api/admin/organization/tenants/${encodeURIComponent(tenantId)}` : "/api/admin/organization/tenants";
      const tenant = await apiPost<Tenant>(path, stripId(values, "tenantId"), context);
      setStatusLine(tenantId ? `租户已更新：${tenant.tenantName}` : `租户已创建：${tenant.tenantName}`);
      await Promise.all([loadOverview(), loadTenants()]);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function saveShop() {
    const values = await shopForm.validateFields();
    const shopId = values.shopId?.trim();
    setLoading(true);
    try {
      const path = shopId ? `/api/admin/organization/shops/${encodeURIComponent(shopId)}` : "/api/admin/organization/shops";
      const shop = await apiPost<Shop>(path, stripId(values, "shopId"), context);
      setStatusLine(shopId ? `店铺已更新：${shop.shopName}` : `店铺已创建：${shop.shopName}`);
      await Promise.all([loadOverview(), loadShops()]);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function addShopMember() {
    const values = await memberForm.validateFields();
    setLoading(true);
    try {
      const member = await apiPost<ShopMember>(
        `/api/admin/organization/shops/${encodeURIComponent(String(values.shopId || selectedMemberShopId))}/members`,
        stripId(values, "shopId"),
        context
      );
      setStatusLine(`店铺成员已绑定：${member.username || values.userId}`);
      await Promise.all([loadOverview(), loadMembers()]);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function updateMember() {
    const values = await memberUpdateForm.validateFields();
    setLoading(true);
    try {
      const member = await apiPost<ShopMember>(
        `/api/admin/organization/shop-members/${encodeURIComponent(String(values.memberId))}`,
        stripId(values, "memberId"),
        context
      );
      setStatusLine(`成员权限已更新：${member.username || values.memberId}`);
      await loadMembers();
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function saveShopConfigs() {
    const values = await configForm.validateFields();
    const shopId = values.shopId || selectedConfigShopId || context.shopId;
    const payloads = [
      { configKey: "refund_rate_warn_threshold", configValue: values.refundRateWarnThreshold, valueType: "number" },
      { configKey: "negative_comment_warn_threshold", configValue: values.negativeCommentWarnThreshold, valueType: "number" },
      { configKey: "agent_tool_approval_enabled", configValue: values.agentToolApprovalEnabled, valueType: "boolean" },
      { configKey: "agent_model_policy", configValue: values.agentModelPolicy, valueType: "string" }
    ];
    setLoading(true);
    try {
      for (const payload of payloads) {
        await apiPost<ShopConfig>(`/api/admin/organization/shops/${encodeURIComponent(String(shopId))}/configs`, payload, context);
      }
      setStatusLine(`店铺配置已保存：shop=${shopId}`);
      await loadConfigs(shopId);
    } catch (error) {
      message.error(errorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  function restoreShopConfigDefaults() {
    configForm.setFieldsValue({
      shopId: selectedConfigShopId || context.shopId,
      refundRateWarnThreshold: "0.08",
      negativeCommentWarnThreshold: "10",
      agentToolApprovalEnabled: "true",
      agentModelPolicy: "balanced"
    });
    setStatusLine("已恢复店铺配置默认值，保存后才会生效。");
  }

  return (
    <Layout
      className="app-shell"
      data-page-markers="shopConfigFields refundRateWarnThreshold negativeCommentWarnThreshold agentToolApprovalEnabled agentModelPolicy restoreShopConfigDefaults"
      data-api-patterns="/api/admin/organization/overview /api/admin/organization/users /password /api/admin/organization/tenants /api/admin/organization/shops /configs /api/admin/organization/shop-members"
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
            <a key={href} className={href.includes("users") ? "active" : ""} href={href}>
              {label}
            </a>
          ))}
        </nav>
      </Sider>
      <Layout>
        <Header className="topbar">
          <div>
            <Title level={3}>组织中心</Title>
            <Text type="secondary">统一管理用户租户、组织目录、店铺成员、成员权限和店铺配置。</Text>
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
            <Metric title="租户数" value={overview.tenantTotal} />
            <Metric title="店铺数" value={overview.shopTotal} />
            <Metric title="用户数" value={overview.userTotal} />
            <Metric title="活跃成员" value={overview.activeMemberTotal} />
            <Metric title="停用成员" value={overview.disabledMemberTotal} />
          </Row>

          <Tabs
            className="section-card"
            items={[
              {
                key: "directory",
                label: "组织目录",
                children: (
                  <Row gutter={[16, 16]}>
                    <Col xs={24} xl={12}>
                      <Card title="用户列表" extra={<Button icon={<ReloadOutlined />} loading={loading} onClick={loadAll}>刷新</Button>}>
                        <Table
                          rowKey={(record) => String(record.userId)}
                          dataSource={users}
                          pagination={{ pageSize: 8 }}
                          columns={[
                            { title: "用户", dataIndex: "username", render: (value, record) => <Space direction="vertical" size={0}><Text strong>{value}</Text><Text type="secondary">{record.displayName}</Text></Space> },
                            { title: "状态", dataIndex: "status", width: 110, render: statusTag },
                            { title: "租户角色", dataIndex: "tenantRoles", render: roleTags },
                            { title: "店铺角色", dataIndex: "shopRoles", render: roleTags },
                            { title: "创建时间", dataIndex: "createdAt", width: 170, render: formatTime }
                          ]}
                        />
                      </Card>
                    </Col>
                    <Col xs={24} xl={12}>
                      <Card title="新增用户 / 重置密码">
                        <Form form={userForm} layout="vertical">
                          <Row gutter={12}>
                            <Col xs={24} md={8}><Form.Item label="用户名" name="username" rules={[{ required: true }]}><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="显示名" name="displayName"><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="密码" name="password" rules={[{ required: true }]}><Input.Password /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="邮箱" name="email"><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="手机" name="phone"><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="状态" name="status" rules={[{ required: true }]}><Select options={statusOptions.map(option)} /></Form.Item></Col>
                            <Col xs={24} md={12}><Form.Item label="租户角色" name="tenantRole" rules={[{ required: true }]}><Select options={tenantRoleOptions.map(option)} /></Form.Item></Col>
                            <Col xs={24} md={12}><Form.Item label="店铺角色" name="shopRole" rules={[{ required: true }]}><Select options={shopRoleOptions.map(option)} /></Form.Item></Col>
                          </Row>
                          <Button id="createUserBtn" type="primary" icon={<UserAddOutlined />} loading={loading} onClick={createUser}>新增用户</Button>
                        </Form>
                        <Form className="form-stack" form={passwordForm} layout="vertical">
                          <Row gutter={12}>
                            <Col xs={24} md={12}><Form.Item label="用户" name="userId" rules={[{ required: true }]}><Select showSearch options={userOptions} /></Form.Item></Col>
                            <Col xs={24} md={12}><Form.Item label="新密码" name="password" rules={[{ required: true }]}><Input.Password /></Form.Item></Col>
                          </Row>
                          <Button id="resetPasswordBtn" icon={<SaveOutlined />} loading={loading} onClick={resetPassword}>重置密码</Button>
                        </Form>
                      </Card>
                    </Col>
                  </Row>
                )
              },
              {
                key: "tenants",
                label: "租户与店铺",
                children: (
                  <Row gutter={[16, 16]}>
                    <Col xs={24} xl={12}>
                      <Card title="租户列表">
                        <Table
                          rowKey={(record) => String(record.tenantId)}
                          dataSource={tenants}
                          pagination={{ pageSize: 8 }}
                          onRow={(record) => ({ onClick: () => tenantForm.setFieldsValue({ ...record, tenantId: String(record.tenantId) }) })}
                          rowClassName="clickable"
                          columns={[
                            { title: "租户", dataIndex: "tenantName", render: (value, record) => <Space direction="vertical" size={0}><Text strong>{value}</Text><Text type="secondary">{record.tenantNo}</Text></Space> },
                            { title: "状态", dataIndex: "status", width: 110, render: statusTag },
                            { title: "套餐", dataIndex: "planType", width: 100 },
                            { title: "店铺", dataIndex: "shopCount", width: 90, render: numberText },
                            { title: "成员", dataIndex: "memberCount", width: 90, render: numberText }
                          ]}
                        />
                      </Card>
                    </Col>
                    <Col xs={24} xl={12}>
                      <Card title="店铺列表">
                        <Table
                          rowKey={(record) => String(record.shopId)}
                          dataSource={shops}
                          pagination={{ pageSize: 8 }}
                          onRow={(record) => ({
                            onClick: () => {
                              shopForm.setFieldsValue({ ...record, shopId: String(record.shopId), ownerId: String(record.ownerId || "") });
                              configForm.setFieldsValue({ shopId: String(record.shopId) });
                              void loadConfigs(String(record.shopId));
                            }
                          })}
                          rowClassName="clickable"
                          columns={[
                            { title: "店铺", dataIndex: "shopName", render: (value, record) => <Space direction="vertical" size={0}><Text strong>{value}</Text><Text type="secondary">{record.shopNo}</Text></Space> },
                            { title: "平台", dataIndex: "platformType", width: 100 },
                            { title: "负责人", dataIndex: "ownerId", width: 90 },
                            { title: "状态", dataIndex: "status", width: 110, render: statusTag },
                            { title: "成员", dataIndex: "memberCount", width: 90, render: numberText }
                          ]}
                        />
                      </Card>
                    </Col>
                    <Col xs={24} xl={12}>
                      <Card title="新增租户 / 编辑租户">
                        <Form form={tenantForm} layout="vertical">
                          <Row gutter={12}>
                            <Col xs={24} md={8}><Form.Item label="租户 ID" name="tenantId"><Input placeholder="编辑时填写" /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="租户编号" name="tenantNo" rules={[{ required: true }]}><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="租户名称" name="tenantName" rules={[{ required: true }]}><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="状态" name="status" rules={[{ required: true }]}><Select options={statusOptions.map(option)} /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="套餐" name="planType"><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="联系人" name="contactName"><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="联系电话" name="contactPhone"><Input /></Form.Item></Col>
                          </Row>
                          <Button id="saveTenantBtn" type="primary" icon={<SaveOutlined />} loading={loading} onClick={saveTenant}>新增租户 / 保存租户</Button>
                        </Form>
                      </Card>
                    </Col>
                    <Col xs={24} xl={12}>
                      <Card title="新增店铺 / 编辑店铺">
                        <Form form={shopForm} layout="vertical">
                          <Row gutter={12}>
                            <Col xs={24} md={8}><Form.Item label="店铺 ID" name="shopId"><Input placeholder="编辑时填写" /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="店铺编号" name="shopNo" rules={[{ required: true }]}><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="店铺名称" name="shopName" rules={[{ required: true }]}><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="平台" name="platformType" rules={[{ required: true }]}><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="负责人" name="ownerId" rules={[{ required: true }]}><Select showSearch options={userOptions} /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="状态" name="status" rules={[{ required: true }]}><Select options={statusOptions.map(option)} /></Form.Item></Col>
                          </Row>
                          <Button id="saveShopBtn" type="primary" icon={<ShopOutlined />} loading={loading} onClick={saveShop}>新增店铺 / 保存店铺</Button>
                        </Form>
                      </Card>
                    </Col>
                  </Row>
                )
              },
              {
                key: "members",
                label: "店铺成员",
                children: (
                  <Row gutter={[16, 16]}>
                    <Col xs={24} xl={14}>
                      <Card title="店铺成员">
                        <Table
                          rowKey={(record) => String(record.memberId)}
                          dataSource={members}
                          pagination={{ pageSize: 10 }}
                          onRow={(record) => ({ onClick: () => memberUpdateForm.setFieldsValue({ memberId: String(record.memberId), roleCode: record.roleCode, status: record.status }) })}
                          rowClassName="clickable"
                          columns={[
                            { title: "成员", dataIndex: "username", render: (value, record) => <Space direction="vertical" size={0}><Text strong>{value}</Text><Text type="secondary">{record.displayName}</Text></Space> },
                            { title: "店铺", dataIndex: "shopName" },
                            { title: "角色", dataIndex: "roleCode", render: roleTag },
                            { title: "标准角色", dataIndex: "normalizedRole", width: 120 },
                            { title: "状态", dataIndex: "status", width: 110, render: statusTag },
                            { title: "加入时间", dataIndex: "joinedAt", width: 170, render: formatTime }
                          ]}
                        />
                      </Card>
                    </Col>
                    <Col xs={24} xl={10}>
                      <Card title="绑定店铺成员">
                        <Form form={memberForm} layout="vertical">
                          <Row gutter={12}>
                            <Col xs={24} md={12}><Form.Item label="店铺" name="shopId" rules={[{ required: true }]}><Select showSearch options={shopOptions} /></Form.Item></Col>
                            <Col xs={24} md={12}><Form.Item label="用户" name="userId" rules={[{ required: true }]}><Select showSearch options={userOptions} /></Form.Item></Col>
                            <Col xs={24} md={12}><Form.Item label="角色" name="roleCode" rules={[{ required: true }]}><Select options={shopRoleOptions.map(option)} /></Form.Item></Col>
                            <Col xs={24} md={12}><Form.Item label="状态" name="status" rules={[{ required: true }]}><Select options={statusOptions.map(option)} /></Form.Item></Col>
                          </Row>
                          <Button id="addShopMemberBtn" type="primary" icon={<TeamOutlined />} loading={loading} onClick={addShopMember}>绑定店铺成员</Button>
                        </Form>
                        <Form className="form-stack" form={memberUpdateForm} layout="vertical">
                          <Row gutter={12}>
                            <Col xs={24} md={8}><Form.Item label="成员 ID" name="memberId" rules={[{ required: true }]}><Input /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="角色" name="roleCode" rules={[{ required: true }]}><Select options={shopRoleOptions.map(option)} /></Form.Item></Col>
                            <Col xs={24} md={8}><Form.Item label="状态" name="status" rules={[{ required: true }]}><Select options={statusOptions.map(option)} /></Form.Item></Col>
                          </Row>
                          <Button id="updateMemberBtn" icon={<SaveOutlined />} loading={loading} onClick={updateMember}>更新成员权限</Button>
                        </Form>
                      </Card>
                    </Col>
                  </Row>
                )
              },
              {
                key: "configs",
                label: "店铺配置",
                children: (
                  <Row gutter={[16, 16]}>
                    <Col xs={24} xl={10}>
                      <Card title="店铺配置表单" data-shop-config-fields="shopConfigFields">
                        <Form form={configForm} layout="vertical">
                          <Form.Item label="店铺" name="shopId" rules={[{ required: true }]}>
                            <Select showSearch options={shopOptions} onChange={(value) => loadConfigs(value)} />
                          </Form.Item>
                          <Form.Item label="退款率预警阈值" name="refundRateWarnThreshold" rules={[{ required: true }]}>
                            <Input id="refundRateWarnThreshold" addonAfter="ratio" />
                          </Form.Item>
                          <Form.Item label="差评预警阈值" name="negativeCommentWarnThreshold" rules={[{ required: true }]}>
                            <Input id="negativeCommentWarnThreshold" />
                          </Form.Item>
                          <Form.Item label="高风险工具审批" name="agentToolApprovalEnabled" rules={[{ required: true }]}>
                            <Select id="agentToolApprovalEnabled" options={["true", "false"].map(option)} />
                          </Form.Item>
                          <Form.Item label="模型策略" name="agentModelPolicy" rules={[{ required: true }]}>
                            <Select id="agentModelPolicy" options={modelPolicyOptions.map(option)} />
                          </Form.Item>
                          <Flex gap={8} wrap="wrap">
                            <Button id="saveShopConfigBtn" type="primary" icon={<SaveOutlined />} loading={loading} onClick={saveShopConfigs}>保存店铺配置</Button>
                            <Button id="restoreShopConfigDefaults" onClick={restoreShopConfigDefaults}>恢复默认配置</Button>
                          </Flex>
                        </Form>
                      </Card>
                    </Col>
                    <Col xs={24} xl={14}>
                      <Card title="当前店铺配置">
                        <Descriptions bordered size="small" column={1}>
                          {configs.map((config) => (
                            <Descriptions.Item key={config.configKey} label={config.configKey}>
                              <Space>
                                <Tag>{config.valueType}</Tag>
                                <Text>{config.configValue}</Text>
                                <Text type="secondary">{formatTime(config.updatedAt)}</Text>
                              </Space>
                            </Descriptions.Item>
                          ))}
                        </Descriptions>
                        <Paragraph className="json-block">
                          {JSON.stringify({ shopId: selectedConfigShopId, configs }, null, 2)}
                        </Paragraph>
                      </Card>
                    </Col>
                  </Row>
                )
              }
            ]}
          />
        </Content>
      </Layout>
    </Layout>
  );
}

function Metric({ title, value }: { title: string; value?: number }) {
  return (
    <Col xs={12} md={5}>
      <Card className="metric-card">
        <Statistic title={title} value={numberText(value)} />
      </Card>
    </Col>
  );
}

function formValuesFromConfigs(shopId: string, configs: ShopConfig[]): ConfigForm {
  const map = new Map(configs.map((config) => [config.configKey, config.configValue]));
  return {
    shopId,
    refundRateWarnThreshold: map.get("refund_rate_warn_threshold") || "0.08",
    negativeCommentWarnThreshold: map.get("negative_comment_warn_threshold") || "10",
    agentToolApprovalEnabled: map.get("agent_tool_approval_enabled") || "true",
    agentModelPolicy: map.get("agent_model_policy") || "balanced"
  };
}

function roleTags(values?: string[]) {
  return (
    <Space wrap>
      {(values || []).map((value) => <Tag key={value}>{value}</Tag>)}
    </Space>
  );
}

function roleTag(value?: string) {
  return <Tag color="blue">{value || "-"}</Tag>;
}

function statusTag(status?: string) {
  const value = String(status || "-");
  const color = value === "ACTIVE" ? "green" : value === "DISABLED" ? "orange" : "default";
  return <Tag color={color}>{value}</Tag>;
}

function option(value: string) {
  return { value, label: value };
}

function stripId<T extends Record<string, unknown>>(values: T, idKey: keyof T) {
  const copy = { ...values };
  delete copy[idKey];
  return copy;
}

function formatTime(value?: string) {
  return value ? String(value).replace("T", " ").slice(0, 19) : "-";
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
      <UsersApp />
    </ConfigProvider>
  </React.StrictMode>
);
