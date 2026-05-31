import { useEffect, useState } from "react";
import DashboardShell from "./DashboardShell";
import { getAssignedEvents } from "../../api/mentor";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { localizeError } from "../../utils/errors";

export default function MentorDashboard() {
	const { auth } = useAuth();
	const { showToast } = useToast();
	const [events, setEvents] = useState([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(null);

	useEffect(() => {
		let cancelled = false;
		(async () => {
			setLoading(true);
			setError(null);
			try {
				const data = await getAssignedEvents();
				if (!cancelled) setEvents(data);
			} catch (err) {
				if (!cancelled) {
					setError(localizeError(err.message));
					showToast("Không tải được sự kiện được phân công", "error");
				}
			} finally {
				if (!cancelled) setLoading(false);
			}
		})();
		return () => {
			cancelled = true;
		};
	}, [showToast]);

	return (
		<DashboardShell
			roleLabel="Mentor"
			title="Tài khoản Mentor"
			subtitle="Bảng điều khiển dành cho Mentor — đồng hành cùng các đội thí sinh trong hackathon."
			role="MENTOR"
		>
			<div className="section-title">
				<h2>Sự kiện được phân công</h2>
				<span className="hint">Các track / sự kiện bạn được Coordinator gán</span>
			</div>

			<div className="card">
				{loading && <div className="empty-state">Đang tải danh sách…</div>}
				{!loading && error && <div className="empty-state">{error}</div>}
				{!loading && !error && events.length === 0 && (
					<div className="empty-state">Bạn chưa được phân công sự kiện nào.</div>
				)}
				{!loading && events.length > 0 && (
					<div className="kv-list">
						{events.map((ev) => (
							<div className="kv" key={ev.eventId}>
								<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
									<div style={{ fontWeight: 600, color: "var(--text)" }}>{ev.title || "—"}</div>
									<div style={{ fontSize: 11, color: "var(--text-mute)", marginTop: 4 }}>ID: {ev.eventId}</div>
								</span>
								<span className="card-badge">{ev.status}</span>
							</div>
						))}
					</div>
				)}
			</div>

			<div className="section-title"><h2>Thông tin tài khoản</h2></div>
			<div className="card">
				<div className="kv-list">
					<div className="kv"><span>Họ tên</span><span>{auth.fullName || "—"}</span></div>
					<div className="kv"><span>Email</span><span>{auth.email}</span></div>
					<div className="kv"><span>Vai trò</span><span>Mentor</span></div>
					<div className="kv"><span>Trạng thái phiên</span><span>Đã đăng nhập</span></div>
				</div>
			</div>
		</DashboardShell>
	);
}
