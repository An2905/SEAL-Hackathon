import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import DashboardShell from "./DashboardShell";
import FormMessage from "../../components/common/FormMessage";
import PendingTeamsBadge from "../../components/common/PendingTeamsBadge";
import { getEventDetail, countPendingTeams } from "../../api/event";
import { changeTeamRegistrationStatus } from "../../api/staff";
import { useToast } from "../../context/ToastContext";
import { localizeError } from "../../utils/errors";

const REGISTRATION_STATUSES = ["PENDING", "APPROVED", "REJECTED"];

function eventStatusPillClass(status) {
	const key = (status || "").toUpperCase();
	if (key === "UPCOMING") return "status-pending";
	if (key === "ONGOING") return "status-active";
	if (key === "COMPLETED") return "status-default";
	if (key === "CANCELLED") return "status-rejected";
	return "status-default";
}

function formatEventDateTime(value) {
	if (!value) return "—";
	const d = new Date(value);
	if (Number.isNaN(d.getTime())) return String(value);
	return d.toLocaleString("vi-VN", {
		day: "2-digit",
		month: "2-digit",
		year: "numeric",
		hour: "2-digit",
		minute: "2-digit",
	});
}

function StatDropdown({ label, count, badgeCount, children }) {
	const [open, setOpen] = useState(false);

	return (
		<div className={`event-stat-dropdown${open ? " is-open" : ""}`}>
			<PendingTeamsBadge count={badgeCount} />
			<button
				type="button"
				className="event-stat-dropdown-trigger"
				onClick={() => setOpen((v) => !v)}
				aria-expanded={open}
			>
				<span className="event-stat-dropdown-meta">
					<div className="event-stat-dropdown-label">{label}</div>
					<div className="event-stat-dropdown-count">{count ?? "0"}</div>
				</span>
				<span className="event-stat-dropdown-chevron" aria-hidden="true">
					▼
				</span>
			</button>
			<div className="event-stat-dropdown-body" hidden={!open}>
				{children}
			</div>
		</div>
	);
}

function registrationStatusPillClass(status) {
	const key = (status || "").toLowerCase();
	if (key === "approved") return "status-active";
	if (key === "pending") return "status-pending";
	if (key === "rejected") return "status-rejected";
	return "status-default";
}

function teamStatusLabel(status) {
	const key = String(status ?? "").trim().toUpperCase();
	if (key === "APPROVED") return "Đã duyệt";
	if (key === "PENDING") return "Chờ duyệt";
	if (key === "REJECTED") return "Từ chối";
	return status || "—";
}

function TeamRegistrationStatusPicker({ team, onUpdated }) {
	const { showToast } = useToast();
	const [open, setOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const registrationId = team?.registrationId ?? "";

	const handleSelect = async (e) => {
		const next = e.target.value;
		setOpen(false);
		const currentStatus = String(team.status ?? "").trim().toUpperCase();
		if (next === currentStatus) return;

		if (!registrationId) {
			showToast("Thiếu Registration ID — vui lòng tải lại trang", "error");
			return;
		}

		setSaving(true);
		try {
			await changeTeamRegistrationStatus({ registrationId, status: next });
			onUpdated(registrationId, next);
			showToast(`Đã cập nhật trạng thái → ${next}`, "success");
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setSaving(false);
		}
	};

	if (!registrationId) {
		return (
			<span style={{ fontSize: 11, color: "var(--text-mute)" }}>
				{teamStatusLabel(team.status)}
			</span>
		);
	}

	if (open) {
		return (
			<div className="status-picker">
				<select
					className="status-picker-select"
					value={team.status}
					onChange={handleSelect}
					onBlur={() => setOpen(false)}
					disabled={saving}
					autoFocus
				>
					{REGISTRATION_STATUSES.map((s) => (
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
				className={`status-pill ${registrationStatusPillClass(team.status)}`}
				onClick={() => !saving && setOpen(true)}
				disabled={saving}
				title="Nhấn để đổi trạng thái đăng ký"
			>
				{team.status}
			</button>
		</div>
	);
}

function TeamsDropdownContent({ teams, onUpdated }) {
	if (!teams.length) {
		return (
			<div className="event-stat-dropdown-empty">Chưa có đội nào tham gia.</div>
		);
	}
	return (
		<div className="kv-list">
			{teams.map((team) => (
				<div className="kv" key={team.registrationId || team.teamId}>
					<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
						<div style={{ fontWeight: 600 }}>{team.teamName || "—"}</div>
						<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
							Team ID: {team.teamId}
							{team.registrationId
								? ` · Reg ID: ${team.registrationId}`
								: ""}
						</div>
					</span>
					<TeamRegistrationStatusPicker
						team={team}
						onUpdated={onUpdated}
					/>
				</div>
			))}
		</div>
	);
}

function CategoriesDropdownContent({ categories }) {
	if (!categories.length) {
		return (
			<div className="event-stat-dropdown-empty">Chưa có category nào.</div>
		);
	}
	return (
		<div className="kv-list">
			{categories.map((cat) => (
				<div className="kv" key={cat.categoryId}>
					<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
						<div style={{ fontWeight: 600 }}>{cat.name || "—"}</div>
						{cat.description ? (
							<div style={{ fontSize: 11, color: "var(--text-dim)" }}>
								{cat.description}
							</div>
						) : null}
						<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
							ID: {cat.categoryId}
						</div>
					</span>
				</div>
			))}
		</div>
	);
}

function RoundsDropdownContent({ rounds }) {
	if (!rounds.length) {
		return (
			<div className="event-stat-dropdown-empty">Chưa có vòng thi nào.</div>
		);
	}
	return (
		<div className="kv-list">
			{rounds.map((round) => (
				<div className="kv" key={round.roundId}>
					<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
						<div style={{ fontWeight: 600 }}>{round.name || "—"}</div>
						<div style={{ fontSize: 11, color: "var(--text-dim)" }}>
							{formatEventDateTime(round.startDate)} →{" "}
							{formatEventDateTime(round.endDate)}
						</div>
						<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
							Deadline: {formatEventDateTime(round.submissionDeadline)}
						</div>
					</span>
				</div>
			))}
		</div>
	);
}

function AwardsDropdownContent({ awards }) {
	if (!awards.length) {
		return (
			<div className="event-stat-dropdown-empty">Chưa có giải thưởng nào.</div>
		);
	}
	return (
		<div className="kv-list">
			{awards.map((award) => (
				<div className="kv" key={award.awardId}>
					<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
						<div style={{ fontWeight: 600 }}>{award.title || "—"}</div>
						<div style={{ fontSize: 11, color: "var(--text-dim)" }}>
							{award.teamName || "—"}
						</div>
						<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
							Hạng #{award.rank || "—"}
						</div>
					</span>
				</div>
			))}
		</div>
	);
}

export default function EventDetailsPage() {
	const { eventId } = useParams();
	const { showToast } = useToast();
	const [event, setEvent] = useState(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(null);

	const loadEvent = useCallback(async () => {
		setLoading(true);
		setError(null);
		try {
			setEvent(await getEventDetail(eventId));
		} catch (err) {
			setError(localizeError(err.message));
			showToast("Không tải được chi tiết sự kiện", "error");
		} finally {
			setLoading(false);
		}
	}, [eventId, showToast]);

	useEffect(() => {
		loadEvent();
	}, [loadEvent]);

	const handleTeamRegistrationUpdated = (registrationId, newStatus) => {
		setEvent((prev) => {
			if (!prev) return prev;
			return {
				...prev,
				teams: prev.teams.map((team) =>
					team.registrationId === registrationId
						? { ...team, status: newStatus }
						: team
				),
			};
		});
	};

	const pendingTeamsCount = countPendingTeams(event?.teams);

	return (
		<DashboardShell
			roleLabel="Staff"
			title="Chi tiết sự kiện"
			subtitle="Thông tin đầy đủ của hackathon."
			role="COORDINATOR"
		>
			<div className="action-row" style={{ marginBottom: 16 }}>
				<Link to="/staff" className="btn btn-ghost">
					← Quay lại danh sách
				</Link>
			</div>

			{loading && (
				<div className="empty-state">Đang tải chi tiết sự kiện…</div>
			)}

			{error && !loading && <FormMessage message={error} type="error" />}

			{!loading && event && (
				<div className="card">
					<div className="card-head">
						<div>
							<div className="card-title">{event.title || "—"}</div>
							<div className="card-sub" style={{ margin: 0 }}>
								Event ID: {event.eventId}
							</div>
						</div>
						<span
							className={`status-pill ${eventStatusPillClass(event.status)}`}
							style={{ cursor: "default" }}
						>
							{event.status || "—"}
						</span>
					</div>

					{event.description ? (
						<p className="card-sub" style={{ marginTop: 12 }}>
							{event.description}
						</p>
					) : null}

					<div className="event-stat-grid">
						<StatDropdown
							label="Đội tham gia"
							count={event.totalTeams}
							badgeCount={pendingTeamsCount}
						>
							<TeamsDropdownContent
								teams={event.teams}
								onUpdated={handleTeamRegistrationUpdated}
							/>
						</StatDropdown>
						<StatDropdown label="Categories" count={event.totalCategories}>
							<CategoriesDropdownContent categories={event.categories} />
						</StatDropdown>
						<StatDropdown label="Vòng thi" count={event.totalRounds}>
							<RoundsDropdownContent rounds={event.rounds} />
						</StatDropdown>
						<StatDropdown label="Giải thưởng" count={event.totalAwards}>
							<AwardsDropdownContent awards={event.awards} />
						</StatDropdown>
					</div>

					<div className="kv-list" style={{ marginTop: 16 }}>
						<div className="kv">
							<span>Event ID</span>
							<span>{event.eventId}</span>
						</div>
						<div className="kv">
							<span>Tên sự kiện</span>
							<span>{event.title || "—"}</span>
						</div>
						<div className="kv">
							<span>Mô tả</span>
							<span style={{ textAlign: "right", maxWidth: "70%" }}>
								{event.description || "—"}
							</span>
						</div>
						<div className="kv">
							<span>Ngày bắt đầu</span>
							<span>{formatEventDateTime(event.startDate)}</span>
						</div>
						<div className="kv">
							<span>Ngày kết thúc</span>
							<span>{formatEventDateTime(event.endDate)}</span>
						</div>
						<div className="kv">
							<span>Trạng thái</span>
							<span>{event.status || "—"}</span>
						</div>
						<div className="kv">
							<span>Ngày tạo</span>
							<span>{formatEventDateTime(event.createdAt)}</span>
						</div>
					</div>
				</div>
			)}
		</DashboardShell>
	);
}
