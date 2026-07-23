import { Layout } from "antd";
import { RobotOutlined } from "@ant-design/icons";

const { Sider } = Layout;

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

type AdminSidebarProps = {
  active: string;
};

export function AdminSidebar({ active }: AdminSidebarProps) {
  return (
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
          <a className={href.includes(active) ? "active" : ""} href={href} key={href}>
            {label}
          </a>
        ))}
      </nav>
    </Sider>
  );
}
