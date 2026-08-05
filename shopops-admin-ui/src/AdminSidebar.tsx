import type React from "react";
import { Layout } from "antd";
import {
  AppstoreOutlined,
  AuditOutlined,
  CheckSquareOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  ShopOutlined,
  TeamOutlined,
  ToolOutlined
} from "@ant-design/icons";
import { hasPermission } from "./session";

const { Sider } = Layout;

type NavItem = { href: string; label: string; active: string; permission?: string; icon: React.ReactNode };

const navItems: NavItem[] = [
  { href: "/admin/dashboard.html", label: "运营总览", active: "dashboard", permission: "dashboard:read", icon: <DashboardOutlined /> },
  { href: "/admin/tasks.html", label: "任务中心", active: "tasks", permission: "task:read", icon: <CheckSquareOutlined /> },
  { href: "/admin/approvals.html", label: "审批中心", active: "approvals", permission: "approval:read", icon: <SafetyCertificateOutlined /> },
  { href: "/admin/reports.html", label: "报告中心", active: "reports", permission: "report:generate", icon: <FileTextOutlined /> },
  { href: "/admin/connectors.html", label: "连接器", active: "connectors", permission: "connector:read", icon: <DatabaseOutlined /> },
  { href: "/admin/workbench.html", label: "自动化工作台", active: "workbench", permission: "agent:execute", icon: <RobotOutlined /> },
  { href: "/admin/tools.html", label: "工具治理", active: "tools", permission: "tool:read", icon: <ToolOutlined /> },
  { href: "/admin/audit.html", label: "审计中心", active: "audit", permission: "audit:read", icon: <AuditOutlined /> },
  { href: "/admin/users.html", label: "组织与店铺", active: "users", permission: "user:manage", icon: <TeamOutlined /> },
  { href: "/admin/prompts.html", label: "Prompt 配置", active: "prompts", permission: "agent:execute", icon: <SettingOutlined /> },
  { href: "/admin/auth.html", label: "身份与会话", active: "auth", icon: <ShopOutlined /> }
];

type AdminSidebarProps = { active: string };

export function AdminSidebar({ active }: AdminSidebarProps) {
  const visibleItems = navItems.filter((item) => hasPermission(item.permission));
  return (
    <Sider width={240} className="sidebar" breakpoint="lg" collapsedWidth="0">
      <div className="brand">
        <AppstoreOutlined />
        <div><strong>ShopOps</strong><span>多店铺运营管理平台</span></div>
      </div>
      <div className="nav-section-label">运营工作台</div>
      <nav className="nav" aria-label="后台导航">
        {visibleItems.map((item) => (
          <a className={active === item.active ? "active" : ""} href={item.href} key={item.href}>
            <span className="nav-icon">{item.icon}</span><span>{item.label}</span>
          </a>
        ))}
      </nav>
    </Sider>
  );
}
