import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardShell from "./DashboardShell";
import FormField from "../../components/common/FormField";
import FormMessage from "../../components/common/FormMessage";
import PendingTeamsBadge from "../../components/common/PendingTeamsBadge";
import LoadingButton from "../../components/common/LoadingButton";
import {
	createStaffAccount,
	changeEventStatus,
	changeAccountStatus,
	getAllAccounts,
	normalizeAccountUserId,
} from "../../api/staff";
import { getAllEvents, attachPendingTeamsToEvents } from "../../api/event";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { localizeError } from "../../utils/errors";

const EVENT_STATUSES = [
	{ value: "UPCOMING", label: "Sắp diễn ra (UPCOMING)" },
	{ value: "ONGOING", label: "Đang diễn ra (ONGOING)" },
	{ value: "COMPLETED", label: "Đã kết thúc (COMPLETED)" },
];

// Dropdown options ↔ giá trị BE chấp nhận trong field `role`
const ACCOUNT_ROLE_FILTERS = [
	{ value: "ALL", label: "Tất cả" },
	{ value: "MENTOR", label: "Mentor" },
	{ value: "JUDGE_INTERNAL", label: "Judge" },
	{ value: "STUDENT_EXTERNAL", label: "Student" },
	{ value: "STUDENT_FPT", label: "FPT Student" },
];

const ROLE_LABELS = {
	MENTOR: "Mentor",
	JUDGE_INTERNAL: "Judge",
	STUDENT_FPT: "FPT Student",
	STUDENT_EXTERNAL: "Student",
	COORDINATOR: "Coordinator",
};

// Khớp CHECK constraint DB users.status: PENDING | APPROVED | REJECTED
const ACCOUNT_STATUSES = ["PENDING", "APPROVED", "REJECTED"];

function DashboardSection({
	title,
	hint,
	defaultOpen = false,
	badgeCount,
	children,
}) {
	const [open, setOpen] = useState(defaultOpen);
	const hasBadge = Number(badgeCount) > 0;

	return (
		<div
			className={`dashboard-section${open ? " is-open" : ""}${
				hasBadge ? " has-pending-badge" : ""
			}`}
		>
			<PendingTeamsBadge count={badgeCount} />
			<button
				type="button"
				className="dashboard-section-trigger"
				onClick={() => setOpen((v) => !v)}
				aria-expanded={open}
			>
				<span className="dashboard-section-heading">
					<h2>{title}</h2>
					{hint ? <span className="hint">{hint}</span> : null}
				</span>
				<span className="dashboard-section-chevron" aria-hidden="true">
					▼
				</span>
			</button>
			<div className="dashboard-section-body" hidden={!open}>
				{children}
			</div>
		</div>
	);
}

function statusPillClass(status) {
	const key = (status || "").toLowerCase();
	if (key === "approved") return "status-active";
	if (key === "pending") return "status-pending";
	if (key === "rejected") return "status-rejected";
	return "status-default";
}

function resolveAccountUserId(account) {
	return normalizeAccountUserId(account?.userId ?? account?.user_id);
}

function AccountStatusPicker({ account, onUpdated }) {
	const { showToast } = useToast();
	const [open, setOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const locked = account.role === "COORDINATOR";
	const userId = resolveAccountUserId(account);

	const handleSelect = async (e) => {
		const next = e.target.value;
		setOpen(false);
		const currentStatus = String(account.status ?? "").trim().toUpperCase();
		if (next === currentStatus) return;

		const resolvedId = resolveAccountUserId(account);
		if (!resolvedId) {
			showToast("Thiếu User ID — vui lòng tải lại danh sách", "error");
			return;
		}

		setSaving(true);
		try {
			await changeAccountStatus({ userId: resolvedId, status: next });
			onUpdated(resolvedId, next);
			showToast(`Đã cập nhật trạng thái → ${next}`, "success");
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setSaving(false);
		}
	};

	if (open && !locked) {
		return (
			<div className="status-picker">
				<select
					className="status-picker-select"
					value={account.status}
					onChange={handleSelect}
					onBlur={() => setOpen(false)}
					disabled={saving}
					autoFocus
				>
					{ACCOUNT_STATUSES.map((s) => (
						<option key={s} value={s}>
							{s}
						</option>
					))}
				</select>
			</div>
		);
	}

	return (
		<div className="status-picker">
			<button
				type="button"
				className={`status-pill ${statusPillClass(account.status)}`}
				onClick={() => !locked && !saving && setOpen(true)}
				disabled={locked || saving}
				title={
					locked
						? "Không thể đổi trạng thái Coordinator"
						: "Nhấn để đổi trạng thái"
				}
			>
				{account.status}
			</button>
		</div>
	);
}

// ─── Create Staff Account Form ────────────────────────────────────────────────
function CreateStaffAccountForm({ onSuccess }) {
	const { showToast } = useToast();
	const [loading, setLoading] = useState(false);
	const [message, setMessage] = useState(null);
	const [form, setForm] = useState({
		email: "",
		fullName: "",
		role: "JUDGE_INTERNAL",
	});

	const handle = (e) =>
		setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

	const handleSubmit = async (e) => {
		e.preventDefault();
		setMessage(null);

		const email = form.email.trim();
		const fullName = form.fullName.trim();
		if (!email || !fullName) {
			setMessage({ text: "Vui lòng nhập đầy đủ thông tin", type: "error" });
			return;
		}

		setLoading(true);
		try {
			await createStaffAccount({ email, fullName, role: form.role });
			setMessage({
				text: `Đã tạo tài khoản ${form.role} cho ${email}. Mật khẩu tạm đã được gửi qua email.`,
				type: "success",
			});
			showToast("Đã tạo tài khoản & gửi email", "success");
			setForm({ email: "", fullName: "", role: form.role });
			onSuccess?.(`Tạo tài khoản ${form.role} — ${email}`);
		} catch (err) {
			setMessage({ text: localizeError(err.message), type: "error" });
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="card">
			<div className="card-head">
				<div className="card-title">Tạo tài khoản Judge / Mentor</div>
			</div>
			<p className="card-sub">
				Hệ thống sinh mật khẩu ngẫu nhiên và gửi email mời cho người được tạo
				tài khoản.
			</p>
			<form className="form" onSubmit={handleSubmit}>
				<FormField label="Họ và tên">
					<input
						name="fullName"
						value={form.fullName}
						onChange={handle}
						required
						placeholder="Nguyễn Văn A"
					/>
				</FormField>
				<FormField label="Email">
					<input
						name="email"
						type="email"
						value={form.email}
						onChange={handle}
						required
						placeholder="judge@fpt.edu.vn"
					/>
				</FormField>
				<FormField label="Vai trò">
					<select name="role" value={form.role} onChange={handle} required>
						<option value="JUDGE_INTERNAL">Judge — Giám khảo</option>
						<option value="MENTOR">Mentor — Người hướng dẫn</option>
					</select>
				</FormField>
				<LoadingButton loading={loading} type="submit">
					Tạo tài khoản &amp; gửi email
				</LoadingButton>
				<FormMessage message={message?.text} type={message?.type} />
			</form>
		</div>
	);
}

function eventStatusPillClass(status) {
	const key = (status || "").toUpperCase();
	if (key === "UPCOMING") return "status-pending";
	if (key === "ONGOING") return "status-active";
	if (key === "COMPLETED") return "status-default";
	if (key === "CANCELLED") return "status-rejected";
	return "status-default";
}

function EventStatusPicker({ event, onUpdated }) {
	const { showToast } = useToast();
	const [open, setOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const eventId = event?.eventId ?? "";

	const handleSelect = async (e) => {
		const next = e.target.value;
		setOpen(false);
		const currentStatus = String(event.status ?? "").trim().toUpperCase();
		if (next === currentStatus) return;

		if (!eventId) {
			showToast("Thiếu Event ID — vui lòng tải lại danh sách", "error");
			return;
		}

		setSaving(true);
		try {
			await changeEventStatus({ eventId, newStatus: next });
			onUpdated(eventId, next);
			showToast(`Đã cập nhật trạng thái → ${next}`, "success");
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setSaving(false);
		}
	};

	if (open) {
		return (
			<div className="status-picker">
				<select
					className="status-picker-select"
					value={event.status}
					onChange={handleSelect}
					onBlur={() => setOpen(false)}
					disabled={saving}
					autoFocus
				>
					{EVENT_STATUSES.map((s) => (
						<option key={s.value} value={s.value}>
							{s.value}
						</option>
					))}
				</select>
			</div>
		);
	}

	return (
		<div className="status-picker">
			<button
				type="button"
				className={`status-pill ${eventStatusPillClass(event.status)}`}
				onClick={() => !saving && setOpen(true)}
				disabled={saving}
				title="Nhấn để đổi trạng thái"
				style={{ alignSelf: "center", flexShrink: 0 }}
			>
				{event.status}
			</button>
		</div>
	);
}

function formatEventDate(value) {
	if (!value) return "—";
	const d = new Date(value);
	if (Number.isNaN(d.getTime())) return String(value);
	return d.toLocaleDateString("vi-VN", {
		day: "2-digit",
		month: "2-digit",
		year: "numeric",
	});
}

// ─── Events List Section ──────────────────────────────────────────────────────
function EventsListSection({
	refreshKey = 0,
	onStatusChanged,
	onPendingTotalChange,
}) {
	const { showToast } = useToast();
	const [status, setStatus] = useState("ALL");
	const [events, setEvents] = useState([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState(null);
	const [loaded, setLoaded] = useState(false);

	useEffect(() => {
		let cancelled = false;

		(async () => {
			setLoading(true);
			setError(null);
			try {
				const data = await getAllEvents(status);
				const enriched = await attachPendingTeamsToEvents(data);
				if (!cancelled) {
					setEvents(enriched);
					setLoaded(true);
				}
			} catch (err) {
				if (!cancelled) {
					setError(localizeError(err.message));
					setEvents([]);
					showToast("Không tải được danh sách sự kiện", "error");
				}
			} finally {
				if (!cancelled) setLoading(false);
			}
		})();

		return () => {
			cancelled = true;
		};
	}, [status, refreshKey, showToast]);

	const totalPending = events.reduce(
		(sum, ev) => sum + (Number(ev.pendingTeams) || 0),
		0
	);

	useEffect(() => {
		onPendingTotalChange?.(totalPending);
	}, [totalPending, onPendingTotalChange]);

	const handleStatusChange = (e) => setStatus(e.target.value);

	const handleEventStatusUpdated = (eventId, newStatus) => {
		setEvents((prev) =>
			prev.map((ev) =>
				ev.eventId === eventId ? { ...ev, status: newStatus } : ev
			)
		);
		onStatusChanged?.(eventId, newStatus);
	};

	return (
		<div className="card">
			<div className="card-head">
				<div className="card-title">Danh sách sự kiện</div>
			</div>
			<p className="card-sub">
				Xem tất cả hackathon trong hệ thống. Nhấn badge trạng thái để đổi — chỉ
				COORDINATOR có quyền thao tác.
			</p>

			<FormField label="Lọc theo trạng thái">
				<select
					name="status"
					value={status}
					onChange={handleStatusChange}
					disabled={loading}
				>
					<option value="ALL">Tất cả</option>
					{EVENT_STATUSES.map((s) => (
						<option key={s.value} value={s.value}>
							{s.label}
						</option>
					))}
				</select>
			</FormField>

			{error && <FormMessage message={error} type="error" />}

			{loading && (
				<div className="empty-state" style={{ marginTop: 12 }}>
					Đang tải danh sách…
				</div>
			)}

			{!loading && loaded && events.length === 0 && !error && (
				<div className="empty-state" style={{ marginTop: 12 }}>
					Chưa có sự kiện nào khớp với bộ lọc.
				</div>
			)}

			{!loading && events.length > 0 && (
				<>
					<div
						className="card-sub"
						style={{ marginTop: 12, marginBottom: 6 }}
					>
						Tổng cộng <strong>{events.length}</strong> sự kiện
					</div>
					<div className="kv-list">
						{events.map((ev) => (
							<div
								className={`kv event-list-item${
									Number(ev.pendingTeams) > 0
										? " has-pending-badge"
										: ""
								}`}
								key={ev.eventId}
							>
								<PendingTeamsBadge count={ev.pendingTeams} />
								<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
									<div style={{ fontWeight: 600, color: "var(--text)" }}>
										{ev.title || "—"}
									</div>
									{ev.description ? (
										<div
											style={{
												fontSize: 12,
												color: "var(--text-dim)",
												marginTop: 2,
											}}
										>
											{ev.description}
										</div>
									) : null}
									<div
										style={{
											fontSize: 11,
											color: "var(--text-mute)",
											marginTop: 4,
										}}
									>
										ID: {ev.eventId}
										{ev.startDate || ev.endDate
											? ` · ${formatEventDate(ev.startDate)} → ${formatEventDate(ev.endDate)}`
											: ""}
									</div>
								</span>
								<span
									style={{
										display: "flex",
										alignItems: "center",
										gap: 8,
										flexShrink: 0,
										alignSelf: "center",
									}}
								>
									<Link
										to={`/staff/events/${ev.eventId}`}
										className="btn btn-outline"
										style={{ fontSize: 12, padding: "4px 10px" }}
									>
										Chi tiết
									</Link>
									<EventStatusPicker
										event={ev}
										onUpdated={handleEventStatusUpdated}
									/>
								</span>
							</div>
						))}
					</div>
				</>
			)}
		</div>
	);
}

// ─── Accounts List Section ────────────────────────────────────────────────────
function AccountsListSection() {
	const { showToast } = useToast();
	const [role, setRole] = useState("ALL");
	const [search, setSearch] = useState("");
	const [accounts, setAccounts] = useState([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState(null);
	const [loaded, setLoaded] = useState(false);

	const fetchAccounts = useCallback(
		async (selectedRole, input = "") => {
			setLoading(true);
			setError(null);
			try {
				const data = await getAllAccounts(selectedRole, input);
				setAccounts(data);
				setLoaded(true);
			} catch (err) {
				setError(localizeError(err.message));
				setAccounts([]);
				showToast("Không tải được danh sách tài khoản", "error");
			} finally {
				setLoading(false);
			}
		},
		[showToast]
	);

	useEffect(() => {
		fetchAccounts("ALL", "");
	}, [fetchAccounts]);

	const handleRoleChange = (e) => {
		const next = e.target.value;
		setRole(next);
		fetchAccounts(next, search);
	};

	const handleSearchSubmit = (e) => {
		e.preventDefault();
		fetchAccounts(role, search);
	};

	const handleStatusUpdated = (userId, newStatus) => {
		setAccounts((prev) =>
			prev.map((a) =>
				a.userId === userId ? { ...a, status: newStatus } : a
			)
		);
	};

	return (
		<div className="card">
			<div className="card-head">
				<div className="card-title">Danh sách tài khoản</div>
			</div>
			<p className="card-sub">
				Lọc theo vai trò trong hệ thống. Chỉ COORDINATOR có quyền xem.
			</p>

			<form className="form" onSubmit={handleSearchSubmit}>
				<FormField label="Lọc theo vai trò">
					<select
						name="role"
						value={role}
						onChange={handleRoleChange}
						disabled={loading}
					>
						{ACCOUNT_ROLE_FILTERS.map((opt) => (
							<option key={opt.value} value={opt.value}>
								{opt.label}
							</option>
						))}
					</select>
				</FormField>
				<FormField label="Tìm kiếm">
					<input
						name="search"
						value={search}
						onChange={(e) => setSearch(e.target.value)}
						disabled={loading}
						placeholder="Nhập tên, email hoặc User ID"
					/>
				</FormField>
				<LoadingButton loading={loading} type="submit">
					Tìm kiếm
				</LoadingButton>
			</form>

			{error && <FormMessage message={error} type="error" />}

			{loading && (
				<div className="empty-state" style={{ marginTop: 12 }}>
					Đang tải danh sách…
				</div>
			)}

			{!loading && loaded && accounts.length === 0 && !error && (
				<div className="empty-state" style={{ marginTop: 12 }}>
					Không có tài khoản nào khớp với bộ lọc.
				</div>
			)}

			{!loading && accounts.length > 0 && (
				<>
					<div
						className="card-sub"
						style={{ marginTop: 12, marginBottom: 6 }}
					>
						Tổng cộng <strong>{accounts.length}</strong> tài khoản
					</div>
					<div className="kv-list">
						{accounts.map((a) => {
							const uid = resolveAccountUserId(a);
							return (
							<div className="kv" key={uid || a.email}>
								<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
									<div style={{ fontWeight: 600, color: "var(--text)" }}>
										{a.fullName || "—"}
									</div>
									<div style={{ fontSize: 12, color: "var(--text-dim)" }}>
										{a.email}
									</div>
									{uid ? (
										<div
											style={{
												fontSize: 11,
												color: "var(--text-mute)",
												marginTop: 2,
											}}
										>
											ID: {uid}
										</div>
									) : null}
								</span>
								<span
									style={{
										display: "flex",
										gap: 6,
										alignItems: "center",
										flexWrap: "wrap",
										justifyContent: "flex-end",
									}}
								>
									<span className="card-badge">
										{ROLE_LABELS[a.role] || a.role}
									</span>
									<AccountStatusPicker
										account={a}
										onUpdated={handleStatusUpdated}
									/>
								</span>
							</div>
							);
						})}
					</div>
				</>
			)}
		</div>
	);
}

// ─── Activity Log ─────────────────────────────────────────────────────────────
function ActivityLog({ activities }) {
	if (!activities.length) {
		return (
			<div className="empty-state">
				Chưa có hoạt động nào trong phiên này.
			</div>
		);
	}
	return (
		<div className="kv-list">
			{activities.map((a, i) => (
				<div className="kv" key={i}>
					<span>
						{a.at.toLocaleTimeString("vi-VN", {
							hour: "2-digit",
							minute: "2-digit",
						})}
					</span>
					<span>{a.text}</span>
				</div>
			))}
		</div>
	);
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function StaffDashboard() {
	const { auth } = useAuth();
	const [activities, setActivities] = useState([]);
	const [eventsRefresh, setEventsRefresh] = useState(0);
	const [eventsPendingTotal, setEventsPendingTotal] = useState(0);

	const logActivity = (text) =>
		setActivities((prev) => [{ text, at: new Date() }, ...prev]);

	const handleEventStatusChanged = (eventId, newStatus) => {
		logActivity(`Đổi trạng thái event ${eventId} → ${newStatus}`);
		setEventsRefresh((k) => k + 1);
	};

	return (
		<DashboardShell
			roleLabel="Staff"
			title="Tài khoản nhân viên"
			subtitle="Bảng điều khiển dành cho Coordinator — quản lý tài khoản và sự kiện."
			role="COORDINATOR"
		>
			<DashboardSection
				title="Quản trị tài khoản & sự kiện"
				hint="Tạo tài khoản Judge / Mentor"
			>
				<div className="cards">
					<CreateStaffAccountForm onSuccess={logActivity} />
				</div>
			</DashboardSection>

			<DashboardSection
				title="Sự kiện trong hệ thống"
				hint="Xem và đổi trạng thái sự kiện"
				badgeCount={eventsPendingTotal}
				defaultOpen
			>
				<EventsListSection
					refreshKey={eventsRefresh}
					onStatusChanged={handleEventStatusChanged}
					onPendingTotalChange={setEventsPendingTotal}
				/>
			</DashboardSection>

			<DashboardSection
				title="Tài khoản trong hệ thống"
				hint="Danh sách và phê duyệt tài khoản"
			>
				<AccountsListSection />
			</DashboardSection>

			<DashboardSection title="Hoạt động gần đây" hint="Nhật ký thao tác trong phiên">
				<ActivityLog activities={activities} />
			</DashboardSection>

			<DashboardSection title="Thông tin tài khoản" hint="Coordinator đang đăng nhập">
				<div className="card">
					<div className="kv-list">
						<div className="kv">
							<span>Họ tên</span>
							<span>{auth.fullName || "—"}</span>
						</div>
						<div className="kv">
							<span>Email</span>
							<span>{auth.email}</span>
						</div>
						<div className="kv">
							<span>Vai trò</span>
							<span>Staff (COORDINATOR)</span>
						</div>
						<div className="kv">
							<span>Trạng thái phiên</span>
							<span>Đã đăng nhập</span>
						</div>
					</div>
				</div>
			</DashboardSection>
		</DashboardShell>
	);
}
