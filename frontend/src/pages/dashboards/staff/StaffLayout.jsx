import { NavLink, Outlet } from "react-router-dom";
import DashboardShell from "../DashboardShell";

const STAFF_TABS = [
	{ to: "/staff", label: "Tổng quan", end: true },
	{ to: "/staff/accounts", label: "Tài khoản" },
	{ to: "/staff/events", label: "Sự kiện" },
	{ to: "/staff/assign", label: "Phân công Judge / Mentor" },
];

export default function StaffLayout() {
	return (
		<DashboardShell
			roleLabel="Staff"
			title="Bảng điều khiển Coordinator"
			subtitle="Quản lý tài khoản, sự kiện và phân công giám khảo / mentor."
			role="COORDINATOR"
		>
			<nav className="staff-subnav" style={subnavStyle}>
				{STAFF_TABS.map((t) => (
					<NavLink
						key={t.to}
						to={t.to}
						end={t.end}
						className="btn btn-outline"
						style={({ isActive }) => ({
							fontSize: 13,
							padding: "6px 14px",
							...(isActive ? activeStyle : null),
						})}
					>
						{t.label}
					</NavLink>
				))}
			</nav>

			<Outlet />
		</DashboardShell>
	);
}

const subnavStyle = {
	display: "flex",
	flexWrap: "wrap",
	gap: 8,
	marginBottom: 18,
};

const activeStyle = {
	background: "var(--accent, #2563eb)",
	color: "#fff",
	borderColor: "var(--accent, #2563eb)",
};
