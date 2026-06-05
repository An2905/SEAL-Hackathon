import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import DashboardShell from "./DashboardShell";
import FormMessage from "../../components/common/FormMessage";
import Modal from "../../components/common/Modal";
import PendingTeamsBadge from "../../components/common/PendingTeamsBadge";
import { getEventDetail, countPendingTeams } from "../../api/event";
import { changeTeamRegistrationStatus, getAllAccounts } from "../../api/staff";
import {
	deleteJudgeAssignment,
	deleteMentorAssignment,
	updateJudgeAssignment,
	updateMentorAssignment,
} from "../../api/staffAssignment";
import {
	deleteEventGroup,
	deleteEventRound,
	getEventRoundDetail,
	updateEvent,
	updateEventGroup,
	updateEventRound,
} from "../../api/eventService";
import FormField from "../../components/common/FormField";
import LoadingButton from "../../components/common/LoadingButton";
import { useToast } from "../../context/ToastContext";
import { localizeError } from "../../utils/errors";

const REGISTRATION_STATUSES = ["PENDING", "APPROVED", "REJECTED"];
const EVENT_STATUSES = ["BUILDING", "UPCOMING", "ONGOING", "COMPLETED"];

function eventStatusPillClass(status) {
	const key = (status || "").toUpperCase();
	if (key === "BUILDING") return "status-pending";
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

function formatMaxTeams(value) {
	if (value == null || value === "") return "Không giới hạn";
	return String(value);
}

function eventStatusLabel(status) {
	const key = String(status ?? "").trim().toUpperCase();
	if (key === "BUILDING") return "Đang thiết lập (BUILDING)";
	if (key === "UPCOMING") return "Sắp diễn ra (UPCOMING)";
	if (key === "ONGOING") return "Đang diễn ra (ONGOING)";
	if (key === "COMPLETED") return "Đã kết thúc (COMPLETED)";
	return status || "—";
}

function toDatetimeLocalValue(value) {
	if (!value) return "";
	const d = new Date(value);
	if (Number.isNaN(d.getTime())) {
		const s = String(value).trim();
		if (s.length >= 16) return s.slice(0, 16);
		return "";
	}
	const pad = (n) => String(n).padStart(2, "0");
	return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function SetupWarningBadge({ title }) {
	if (!title) return null;
	return (
		<span
			className="event-setup-warning-badge"
			title={title}
			aria-label={title}
		>
			!
		</span>
	);
}

function buildSetupWarnings(event) {
	if (String(event?.status ?? "").toUpperCase() !== "BUILDING") {
		return {};
	}
	const actualRounds = Number(event.totalRounds) || 0;
	const plannedRounds = Number(event.numRounds) || 1;
	const groups = Number(event.totalGroups) || 0;
	const mentors = event.assignedMentors?.length ?? 0;
	const judges = event.assignedJudges?.length ?? 0;
	const warnings = {};

	if (actualRounds < plannedRounds) {
		warnings.rounds = `Cần tạo đủ ${plannedRounds} vòng thi (hiện có ${actualRounds})`;
	}
	if (groups === 0) {
		warnings.groups = "Cần tạo ít nhất một bảng thi";
	}
	if (mentors === 0) {
		warnings.mentors = "Cần phân công mentor";
	}
	if (judges === 0) {
		warnings.judges = "Cần phân công judge";
	}

	return warnings;
}

function StatRow({ label, count, badgeCount, setupWarning, onOpen }) {
	return (
		<div className="event-stat-row">
			<PendingTeamsBadge count={badgeCount} />
			<SetupWarningBadge title={setupWarning} />
			<button
				type="button"
				className="event-stat-row-trigger"
				onClick={onOpen}
				aria-haspopup="dialog"
			>
				<span className="event-stat-row-label">{label}</span>
				<span className="event-stat-row-value">
					<span className="event-stat-row-count">{count ?? "0"}</span>
					<span className="event-stat-row-action" aria-hidden="true">
						›
					</span>
				</span>
			</button>
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
			showToast("Không xác định được đăng ký — vui lòng tải lại trang", "error");
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

function StaffActionLink({ eventId, path, focus, children }) {
	const qs = new URLSearchParams({ eventId: String(eventId) });
	if (focus) qs.set("focus", focus);
	return (
		<div
			style={{
				marginTop: 10,
				paddingTop: 10,
				borderTop: "1px solid var(--border)",
			}}
		>
			<Link
				to={`${path}?${qs.toString()}`}
				className="btn btn-outline"
				style={{ width: "100%", fontSize: 12, justifyContent: "center" }}
			>
				{children}
			</Link>
		</div>
	);
}

function AssignPanelLink({ eventId, focus, children }) {
	return (
		<StaffActionLink eventId={eventId} path="/staff/assign" focus={focus}>
			{children}
		</StaffActionLink>
	);
}

function SetupPanelLink({ eventId, focus, children }) {
	return (
		<StaffActionLink eventId={eventId} path="/staff/setup" focus={focus}>
			{children}
		</StaffActionLink>
	);
}

function StatItemDeleteButton({ itemLabel, onDelete }) {
	const { showToast } = useToast();
	const [loading, setLoading] = useState(false);

	const handleClick = async () => {
		if (
			!window.confirm(
				`Xóa "${itemLabel}"? Phân công mentor/judge liên quan cũng sẽ bị gỡ.`
			)
		) {
			return;
		}
		setLoading(true);
		try {
			await onDelete();
			showToast("Đã xóa thành công", "success");
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setLoading(false);
		}
	};

	return (
		<button
			type="button"
			className="btn btn-danger btn-sm event-stat-item-delete"
			onClick={handleClick}
			disabled={loading}
		>
			{loading ? "…" : "Xóa"}
		</button>
	);
}

function GroupStatItem({ eventId, group, onUpdated, onDeleted }) {
	const { showToast } = useToast();
	const [editOpen, setEditOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const [form, setForm] = useState({
		name: group.name || "",
		maxTeams: group.maxTeams == null ? "" : String(group.maxTeams),
	});

	useEffect(() => {
		if (!editOpen) {
			setForm({
				name: group.name || "",
				maxTeams: group.maxTeams == null ? "" : String(group.maxTeams),
			});
		}
	}, [group.name, group.maxTeams, editOpen]);

	const handleChange = (e) =>
		setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

	const handleSave = async (e) => {
		e.preventDefault();
		setSaving(true);
		try {
			const updated = await updateEventGroup({
				eventId,
				roundId: group.roundId,
				groupId: group.groupId,
				name: form.name,
				maxTeams: form.maxTeams,
			});
			onUpdated?.(updated);
			showToast("Đã cập nhật bảng thi", "success");
			setEditOpen(false);
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setSaving(false);
		}
	};

	return (
		<div className={`event-stat-item-card${editOpen ? " is-edit-open" : ""}`}>
			<div className="kv event-stat-item">
				<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
					<div style={{ fontWeight: 600 }}>{group.name || "—"}</div>
					<div style={{ fontSize: 11, color: "var(--text-dim)" }}>
						Vòng: {group.roundName || "—"}
						{group.maxTeams != null
							? ` · Tối đa ${group.maxTeams} đội`
							: ""}
					</div>
				</span>
				<div className="event-stat-item-actions">
					<button
						type="button"
						className="btn btn-outline btn-sm"
						onClick={() => setEditOpen((v) => !v)}
						aria-expanded={editOpen}
					>
						{editOpen ? "Thu gọn" : "Sửa"}
					</button>
					<StatItemDeleteButton
						itemLabel={group.name || "bảng"}
						onDelete={async () => {
							await deleteEventGroup({
								eventId,
								roundId: group.roundId,
								groupId: group.groupId,
							});
							onDeleted?.(group.groupId);
						}}
					/>
				</div>
			</div>
			{editOpen ? (
				<form className="event-stat-item-edit" onSubmit={handleSave}>
					<FormField label="Tên bảng *">
						<input
							name="name"
							value={form.name}
							onChange={handleChange}
							maxLength={100}
							disabled={saving}
							required
						/>
					</FormField>
					<FormField label="Số đội tối đa">
						<input
							type="number"
							name="maxTeams"
							value={form.maxTeams}
							onChange={handleChange}
							min={1}
							disabled={saving}
							placeholder="Để trống = không giới hạn"
						/>
					</FormField>
					<div className="event-stat-item-edit-foot">
						<LoadingButton loading={saving} type="submit">
							Lưu thay đổi
						</LoadingButton>
						<button
							type="button"
							className="btn btn-ghost btn-sm"
							onClick={() => setEditOpen(false)}
							disabled={saving}
						>
							Hủy
						</button>
					</div>
				</form>
			) : null}
		</div>
	);
}

function RoundStatItem({ eventId, round, onUpdated, onDeleted }) {
	const { showToast } = useToast();
	const [editOpen, setEditOpen] = useState(false);
	const [loadingDetail, setLoadingDetail] = useState(false);
	const [saving, setSaving] = useState(false);
	const [form, setForm] = useState({
		name: round.name || "",
		roundOrder: "1",
		startDate: toDatetimeLocalValue(round.startDate),
		endDate: toDatetimeLocalValue(round.endDate),
		submissionDeadline: toDatetimeLocalValue(round.submissionDeadline),
	});

	const loadDetail = useCallback(async () => {
		setLoadingDetail(true);
		try {
			const d = await getEventRoundDetail({
				eventId,
				roundId: round.roundId,
			});
			setForm({
				name: d.name || "",
				roundOrder: d.roundOrder || "1",
				startDate: toDatetimeLocalValue(d.startDate),
				endDate: toDatetimeLocalValue(d.endDate),
				submissionDeadline: toDatetimeLocalValue(d.submissionDeadline),
			});
		} catch (err) {
			showToast(localizeError(err.message), "error");
			setEditOpen(false);
		} finally {
			setLoadingDetail(false);
		}
	}, [eventId, round.roundId, showToast]);

	useEffect(() => {
		if (editOpen) loadDetail();
	}, [editOpen, loadDetail]);

	const handleChange = (e) =>
		setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

	const handleSave = async (e) => {
		e.preventDefault();
		setSaving(true);
		try {
			const updated = await updateEventRound({
				eventId,
				roundId: round.roundId,
				name: form.name,
				roundOrder: form.roundOrder,
				startDate: form.startDate,
				endDate: form.endDate,
				submissionDeadline: form.submissionDeadline,
			});
			onUpdated?.(updated);
			showToast("Đã cập nhật vòng thi", "success");
			setEditOpen(false);
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setSaving(false);
		}
	};

	return (
		<div className={`event-stat-item-card${editOpen ? " is-edit-open" : ""}`}>
			<div className="kv event-stat-item">
				<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
					<div style={{ fontWeight: 600 }}>{round.name || "—"}</div>
					<div style={{ fontSize: 11, color: "var(--text-dim)" }}>
						Vòng {round.roundOrder || "—"} ·{" "}
						{formatEventDateTime(round.startDate)} →{" "}
						{formatEventDateTime(round.endDate)}
					</div>
					<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
						Deadline nộp bài: {formatEventDateTime(round.submissionDeadline)}
					</div>
				</span>
				<div className="event-stat-item-actions">
					<button
						type="button"
						className="btn btn-outline btn-sm"
						onClick={() => setEditOpen((v) => !v)}
						aria-expanded={editOpen}
					>
						{editOpen ? "Thu gọn" : "Sửa"}
					</button>
					<StatItemDeleteButton
						itemLabel={round.name || "vòng thi"}
						onDelete={async () => {
							await deleteEventRound({
								eventId,
								roundId: round.roundId,
							});
							onDeleted?.(round.roundId);
						}}
					/>
				</div>
			</div>
			{editOpen ? (
				<form className="event-stat-item-edit" onSubmit={handleSave}>
					{loadingDetail ? (
						<div className="event-stat-dropdown-empty">Đang tải…</div>
					) : (
						<>
							<FormField label="Tên vòng *">
								<input
									name="name"
									value={form.name}
									onChange={handleChange}
									maxLength={100}
									disabled={saving}
									required
								/>
							</FormField>
							<FormField label="Thứ tự vòng *">
								<input
									type="number"
									name="roundOrder"
									min={1}
									step={1}
									value={form.roundOrder}
									onChange={handleChange}
									disabled={saving}
									required
								/>
							</FormField>
							<FormField label="Bắt đầu *">
								<input
									type="datetime-local"
									name="startDate"
									value={form.startDate}
									onChange={handleChange}
									disabled={saving}
									required
								/>
							</FormField>
							<FormField label="Kết thúc *">
								<input
									type="datetime-local"
									name="endDate"
									value={form.endDate}
									onChange={handleChange}
									disabled={saving}
									required
								/>
							</FormField>
							<FormField label="Deadline nộp bài *">
								<input
									type="datetime-local"
									name="submissionDeadline"
									value={form.submissionDeadline}
									onChange={handleChange}
									disabled={saving}
									required
								/>
							</FormField>
							<div className="event-stat-item-edit-foot">
								<LoadingButton loading={saving} type="submit">
									Lưu thay đổi
								</LoadingButton>
								<button
									type="button"
									className="btn btn-ghost btn-sm"
									onClick={() => setEditOpen(false)}
									disabled={saving}
								>
									Hủy
								</button>
							</div>
						</>
					)}
				</form>
			) : null}
		</div>
	);
}

function GroupsDropdownContent({
	eventId,
	groups,
	onGroupDeleted,
	onGroupUpdated,
}) {
	return (
		<>
			{!groups.length ? (
				<div className="event-stat-dropdown-empty">Chưa có bảng thi nào.</div>
			) : (
				<div className="kv-list">
					{groups.map((group) => (
						<GroupStatItem
							key={group.groupId}
							eventId={eventId}
							group={group}
							onUpdated={onGroupUpdated}
							onDeleted={onGroupDeleted}
						/>
					))}
				</div>
			)}
			<SetupPanelLink eventId={eventId} focus="group">
				+ Thêm bảng thi
			</SetupPanelLink>
		</>
	);
}

function RoundsDropdownContent({
	eventId,
	rounds,
	onRoundDeleted,
	onRoundUpdated,
}) {
	return (
		<>
			{!rounds.length ? (
				<div className="event-stat-dropdown-empty">Chưa có vòng thi nào.</div>
			) : (
				<div className="kv-list">
					{rounds.map((round) => (
						<RoundStatItem
							key={round.roundId}
							eventId={eventId}
							round={round}
							onUpdated={onRoundUpdated}
							onDeleted={onRoundDeleted}
						/>
					))}
				</div>
			)}
			<SetupPanelLink eventId={eventId} focus="round">
				+ Thêm vòng thi
			</SetupPanelLink>
		</>
	);
}

function MentorStatItem({
	eventId,
	assignment,
	rounds,
	groups,
	onUpdated,
	onDeleted,
}) {
	const { showToast } = useToast();
	const [editOpen, setEditOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const [loadingAccounts, setLoadingAccounts] = useState(false);
	const [mentorAccounts, setMentorAccounts] = useState([]);
	const [form, setForm] = useState({
		roundId: assignment.roundId || "",
		groupId: assignment.groupId || "",
		mentorId: assignment.mentorId || "",
	});

	const filteredGroups = groups.filter(
		(g) => !form.roundId || g.roundId === form.roundId
	);

	const handleStartEdit = async () => {
		setForm({
			roundId: assignment.roundId || "",
			groupId: assignment.groupId || "",
			mentorId: assignment.mentorId || "",
		});
		setEditOpen(true);
		setLoadingAccounts(true);
		try {
			setMentorAccounts(await getAllAccounts("EXPERT"));
		} catch (err) {
			showToast(localizeError(err.message), "error");
			setEditOpen(false);
		} finally {
			setLoadingAccounts(false);
		}
	};

	const handleCancel = () => {
		setForm({
			roundId: assignment.roundId || "",
			groupId: assignment.groupId || "",
			mentorId: assignment.mentorId || "",
		});
		setEditOpen(false);
	};

	const handleSave = async (e) => {
		e.preventDefault();
		setSaving(true);
		try {
			const updated = await updateMentorAssignment({
				eventId,
				roundId: assignment.roundId,
				groupId: assignment.groupId,
				mentorId: assignment.mentorId,
				newRoundId: form.roundId,
				newGroupId: form.groupId,
				newMentorId: form.mentorId,
			});
			onUpdated?.(assignment, updated);
			showToast("Đã cập nhật phân công mentor", "success");
			setEditOpen(false);
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setSaving(false);
		}
	};

	return (
		<div className={`event-stat-item-card${editOpen ? " is-edit-open" : ""}`}>
			<div className="kv event-stat-item">
				<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
					<div style={{ fontWeight: 600 }}>{assignment.mentorName || "—"}</div>
					<div style={{ fontSize: 11, color: "var(--text-dim)" }}>
						{assignment.roundName || "—"} · {assignment.groupName || "—"}
					</div>
					<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
						{assignment.mentorEmail || "—"}
					</div>
				</span>
				<div className="event-stat-item-actions">
					<button
						type="button"
						className="btn btn-outline btn-sm"
						onClick={() => (editOpen ? handleCancel() : handleStartEdit())}
					>
						{editOpen ? "Thu gọn" : "Sửa"}
					</button>
					<StatItemDeleteButton
						itemLabel={assignment.mentorName || "mentor"}
						onDelete={async () => {
							await deleteMentorAssignment({
								eventId,
								roundId: assignment.roundId,
								groupId: assignment.groupId,
								mentorId: assignment.mentorId,
							});
							onDeleted?.(assignment);
						}}
					/>
				</div>
			</div>
			{editOpen ? (
				<form className="event-stat-item-edit" onSubmit={handleSave}>
					{loadingAccounts ? (
						<div className="event-stat-dropdown-empty">Đang tải…</div>
					) : (
						<>
							<FormField label="Vòng *">
								<select
									name="roundId"
									value={form.roundId}
									onChange={(e) =>
										setForm((f) => ({
											...f,
											roundId: e.target.value,
											groupId: "",
										}))
									}
									disabled={saving || !rounds.length}
									required
								>
									<option value="">— Chọn vòng —</option>
									{rounds.map((r) => (
										<option key={r.roundId} value={r.roundId}>
											{r.name}
										</option>
									))}
								</select>
							</FormField>
							<FormField label="Bảng *">
								<select
									name="groupId"
									value={form.groupId}
									onChange={(e) =>
										setForm((f) => ({
											...f,
											groupId: e.target.value,
										}))
									}
									disabled={saving || !filteredGroups.length}
									required
								>
									<option value="">— Chọn bảng —</option>
									{filteredGroups.map((g) => (
										<option key={g.groupId} value={g.groupId}>
											{g.name}
										</option>
									))}
								</select>
							</FormField>
							<FormField label="Mentor *">
								<select
									name="mentorId"
									value={form.mentorId}
									onChange={(e) =>
										setForm((f) => ({
											...f,
											mentorId: e.target.value,
										}))
									}
									disabled={saving || !mentorAccounts.length}
									required
								>
									<option value="">— Chọn mentor —</option>
									{mentorAccounts.map((m) => (
										<option key={m.userId} value={m.userId}>
											{m.fullName || m.email}
										</option>
									))}
								</select>
							</FormField>
							<div className="event-stat-item-edit-foot">
								<LoadingButton loading={saving} type="submit">
									Lưu
								</LoadingButton>
								<button
									type="button"
									className="btn btn-ghost btn-sm"
									onClick={handleCancel}
									disabled={saving}
								>
									Hủy
								</button>
							</div>
						</>
					)}
				</form>
			) : null}
		</div>
	);
}

function JudgeStatItem({
	eventId,
	assignment,
	rounds,
	groups,
	onUpdated,
	onDeleted,
}) {
	const { showToast } = useToast();
	const [editOpen, setEditOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const [loadingAccounts, setLoadingAccounts] = useState(false);
	const [judgeAccounts, setJudgeAccounts] = useState([]);
	const [form, setForm] = useState({
		judgeId: assignment.judgeId || "",
		roundId: assignment.roundId || "",
		groupId: assignment.groupId || "",
	});

	const filteredGroups = groups.filter(
		(g) => !form.roundId || g.roundId === form.roundId
	);

	const handleStartEdit = async () => {
		setForm({
			judgeId: assignment.judgeId || "",
			roundId: assignment.roundId || "",
			groupId: assignment.groupId || "",
		});
		setEditOpen(true);
		setLoadingAccounts(true);
		try {
			setJudgeAccounts(await getAllAccounts("EXPERT"));
		} catch (err) {
			showToast(localizeError(err.message), "error");
			setEditOpen(false);
		} finally {
			setLoadingAccounts(false);
		}
	};

	const handleCancel = () => {
		setForm({
			judgeId: assignment.judgeId || "",
			roundId: assignment.roundId || "",
			groupId: assignment.groupId || "",
		});
		setEditOpen(false);
	};

	const handleSave = async (e) => {
		e.preventDefault();
		setSaving(true);
		try {
			const updated = await updateJudgeAssignment({
				eventId,
				judgeId: assignment.judgeId,
				roundId: assignment.roundId,
				groupId: assignment.groupId,
				newJudgeId: form.judgeId,
				newRoundId: form.roundId,
				newGroupId: form.groupId,
			});
			onUpdated?.(assignment, updated);
			showToast("Đã cập nhật phân công judge", "success");
			setEditOpen(false);
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setSaving(false);
		}
	};

	return (
		<div className={`event-stat-item-card${editOpen ? " is-edit-open" : ""}`}>
			<div className="kv event-stat-item">
				<span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
					<div style={{ fontWeight: 600 }}>{assignment.judgeName || "—"}</div>
					<div style={{ fontSize: 11, color: "var(--text-dim)" }}>
						{assignment.roundName || "—"} · {assignment.groupName || "—"}
					</div>
					<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
						{assignment.judgeEmail || "—"}
					</div>
				</span>
				<div className="event-stat-item-actions">
					<button
						type="button"
						className="btn btn-outline btn-sm"
						onClick={() => (editOpen ? handleCancel() : handleStartEdit())}
					>
						{editOpen ? "Thu gọn" : "Sửa"}
					</button>
					<StatItemDeleteButton
						itemLabel={assignment.judgeName || "judge"}
						onDelete={async () => {
							await deleteJudgeAssignment({
								eventId,
								judgeId: assignment.judgeId,
								roundId: assignment.roundId,
								groupId: assignment.groupId,
							});
							onDeleted?.(assignment);
						}}
					/>
				</div>
			</div>
			{editOpen ? (
				<form className="event-stat-item-edit" onSubmit={handleSave}>
					{loadingAccounts ? (
						<div className="event-stat-dropdown-empty">Đang tải…</div>
					) : (
						<>
							<FormField label="Vòng *">
								<select
									name="roundId"
									value={form.roundId}
									onChange={(e) =>
										setForm((f) => ({
											...f,
											roundId: e.target.value,
											groupId: "",
										}))
									}
									disabled={saving || !rounds.length}
									required
								>
									<option value="">— Chọn vòng —</option>
									{rounds.map((r) => (
										<option key={r.roundId} value={r.roundId}>
											{r.name}
										</option>
									))}
								</select>
							</FormField>
							<FormField label="Bảng *">
								<select
									name="groupId"
									value={form.groupId}
									onChange={(e) =>
										setForm((f) => ({
											...f,
											groupId: e.target.value,
										}))
									}
									disabled={saving || !filteredGroups.length}
									required
								>
									<option value="">— Chọn bảng —</option>
									{filteredGroups.map((g) => (
										<option key={g.groupId} value={g.groupId}>
											{g.name}
										</option>
									))}
								</select>
							</FormField>
							<FormField label="Judge *">
								<select
									name="judgeId"
									value={form.judgeId}
									onChange={(e) =>
										setForm((f) => ({ ...f, judgeId: e.target.value }))
									}
									disabled={saving || !judgeAccounts.length}
									required
								>
									<option value="">— Chọn judge —</option>
									{judgeAccounts.map((j) => (
										<option key={j.userId} value={j.userId}>
											{j.fullName || j.email}
										</option>
									))}
								</select>
							</FormField>
							<div className="event-stat-item-edit-foot">
								<LoadingButton loading={saving} type="submit">
									Lưu
								</LoadingButton>
								<button
									type="button"
									className="btn btn-ghost btn-sm"
									onClick={handleCancel}
									disabled={saving}
								>
									Hủy
								</button>
							</div>
						</>
					)}
				</form>
			) : null}
		</div>
	);
}

function MentorsDropdownContent({
	eventId,
	assignedMentors = [],
	rounds = [],
	groups = [],
	onUpdated,
	onDeleted,
}) {
	return (
		<>
			{!assignedMentors.length ? (
				<div className="event-stat-dropdown-empty">
					Chưa phân công mentor nào.
				</div>
			) : (
				<div className="kv-list">
					{assignedMentors.map((m) => (
						<MentorStatItem
							key={`${m.roundId}-${m.groupId}-${m.mentorId}`}
							eventId={eventId}
							assignment={m}
							rounds={rounds}
							groups={groups}
							onUpdated={onUpdated}
							onDeleted={onDeleted}
						/>
					))}
				</div>
			)}
			<AssignPanelLink eventId={eventId} focus="mentor">
				+ Thêm mentor
			</AssignPanelLink>
		</>
	);
}

function JudgesDropdownContent({
	eventId,
	assignedJudges = [],
	rounds = [],
	groups = [],
	onUpdated,
	onDeleted,
}) {
	return (
		<>
			{!assignedJudges.length ? (
				<div className="event-stat-dropdown-empty">
					Chưa phân công judge nào.
				</div>
			) : (
				<div className="kv-list">
					{assignedJudges.map((j) => (
						<JudgeStatItem
							key={`${j.roundId}-${j.groupId}-${j.judgeId}`}
							eventId={eventId}
							assignment={j}
							rounds={rounds}
							groups={groups}
							onUpdated={onUpdated}
							onDeleted={onDeleted}
						/>
					))}
				</div>
			)}
			<AssignPanelLink eventId={eventId} focus="judge">
				+ Thêm judge
			</AssignPanelLink>
		</>
	);
}

function buildEventInfoForm(event) {
	return {
		title: event.title || "",
		description: event.description || "",
		startDate: toDatetimeLocalValue(event.startDate),
		endDate: toDatetimeLocalValue(event.endDate),
		status: (event.status || "BUILDING").toUpperCase(),
		maxTeams: event.maxTeams == null ? "" : String(event.maxTeams),
		numRounds: event.numRounds == null ? "1" : String(event.numRounds),
	};
}

function EventDetailInfoPanel({ event, onUpdated }) {
	const { showToast } = useToast();
	const [editOpen, setEditOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const [form, setForm] = useState(() => buildEventInfoForm(event));

	const handleChange = (e) =>
		setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

	const handleStartEdit = () => {
		setForm(buildEventInfoForm(event));
		setEditOpen(true);
	};

	const handleCancel = () => {
		setForm(buildEventInfoForm(event));
		setEditOpen(false);
	};

	const handleConfirm = async (e) => {
		e.preventDefault();
		setSaving(true);
		try {
			const updated = await updateEvent({
				eventId: event.eventId,
				title: form.title,
				description: form.description,
				startDate: form.startDate || null,
				endDate: form.endDate || null,
				status: form.status,
				maxTeams: form.maxTeams,
				numRounds: form.numRounds,
			});
			onUpdated?.(updated);
			showToast("Đã cập nhật sự kiện", "success");
			setEditOpen(false);
		} catch (err) {
			showToast(localizeError(err.message), "error");
		} finally {
			setSaving(false);
		}
	};

	const statusLocked =
		String(event.status ?? "").toUpperCase() === "COMPLETED";

	const editFormId = `event-info-edit-${event.eventId}`;

	return (
		<div className="event-detail-info-wrap" style={{ marginTop: 16 }}>
			<div
				className={`event-detail-info${editOpen ? " is-edit-open" : ""}`}
			>
				<div className="event-detail-info-head">
					<span className="event-detail-info-title">Thông tin sự kiện</span>
				</div>

				{!editOpen ? (
					<div className="kv-list">
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
							<span>{eventStatusLabel(event.status)}</span>
						</div>
						<div className="kv">
							<span>Số vòng dự kiến</span>
							<span>{event.numRounds ?? "—"}</span>
						</div>
						<div className="kv">
							<span>Giới hạn đội</span>
							<span>{formatMaxTeams(event.maxTeams)}</span>
						</div>
						<div className="kv">
							<span>Đội đã đăng ký</span>
							<span>{event.totalTeams ?? "0"}</span>
						</div>
						<div className="kv">
							<span>Ngày tạo</span>
							<span>{formatEventDateTime(event.createdAt)}</span>
						</div>
					</div>
				) : (
					<form
						id={editFormId}
						className="event-stat-item-edit event-detail-info-edit"
						onSubmit={handleConfirm}
					>
						<div className="kv-list event-detail-info-readonly">
							<div className="kv">
								<span>Ngày tạo</span>
								<span>{formatEventDateTime(event.createdAt)}</span>
							</div>
						</div>
						<FormField label="Tên sự kiện *">
							<input
								name="title"
								value={form.title}
								onChange={handleChange}
								maxLength={200}
								disabled={saving}
								required
							/>
						</FormField>
						<FormField label="Mô tả">
							<textarea
								name="description"
								value={form.description}
								onChange={handleChange}
								rows={3}
								disabled={saving}
							/>
						</FormField>
						<FormField label="Ngày bắt đầu">
							<input
								type="datetime-local"
								name="startDate"
								value={form.startDate}
								onChange={handleChange}
								disabled={saving}
							/>
						</FormField>
						<FormField label="Ngày kết thúc">
							<input
								type="datetime-local"
								name="endDate"
								value={form.endDate}
								onChange={handleChange}
								disabled={saving}
							/>
						</FormField>
						<FormField label="Số vòng dự kiến *">
							<input
								type="number"
								name="numRounds"
								min={1}
								value={form.numRounds}
								onChange={handleChange}
								disabled={saving}
								required
							/>
						</FormField>
						<FormField label="Giới hạn đội">
							<input
								type="number"
								name="maxTeams"
								min={1}
								value={form.maxTeams}
								onChange={handleChange}
								disabled={saving}
								placeholder="Để trống = không giới hạn"
							/>
						</FormField>
						<FormField label="Trạng thái *">
							<select
								name="status"
								value={form.status}
								onChange={handleChange}
								disabled={saving || statusLocked}
								required
							>
								{EVENT_STATUSES.map((s) => (
									<option key={s} value={s}>
										{s}
									</option>
								))}
							</select>
							{statusLocked ? (
								<p
									className="event-stat-edit-hint"
									style={{ marginTop: 6 }}
								>
									Sự kiện COMPLETED — không đổi trạng thái (theo ràng
									buộc DB).
								</p>
							) : null}
						</FormField>
					</form>
				)}
			</div>

			<div className="event-detail-info-actions-outside">
				{!editOpen ? (
					<button
						type="button"
						className="btn btn-outline"
						onClick={handleStartEdit}
					>
						Sửa
					</button>
				) : (
					<>
						<LoadingButton
							loading={saving}
							type="submit"
							form={editFormId}
							className="btn btn-primary"
						>
							Xác nhận
						</LoadingButton>
						<button
							type="button"
							className="btn btn-outline"
							onClick={handleCancel}
							disabled={saving}
						>
							Hủy
						</button>
					</>
				)}
			</div>
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
	const [statPopup, setStatPopup] = useState(null);

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

	const handleEventUpdated = (updated) => {
		setEvent((prev) => {
			if (!prev) return prev;
			return {
				...prev,
				title: updated.title,
				description: updated.description ?? "",
				startDate: updated.startDate,
				endDate: updated.endDate,
				status: updated.status,
				maxTeams: updated.maxTeams ?? null,
				numRounds: updated.numRounds ?? prev.numRounds,
				createdAt: updated.createdAt || prev.createdAt,
			};
		});
	};

	const handleGroupUpdated = (updated) => {
		setEvent((prev) => {
			if (!prev) return prev;
			return {
				...prev,
				groups: prev.groups.map((g) =>
					g.groupId === updated.groupId
						? {
								...g,
								name: updated.name,
								maxTeams: updated.maxTeams ?? null,
								roundName: updated.roundName ?? g.roundName,
							}
						: g
				),
				assignedMentors: (prev.assignedMentors ?? []).map((m) =>
					m.groupId === updated.groupId
						? { ...m, groupName: updated.name }
						: m
				),
				assignedJudges: (prev.assignedJudges ?? []).map((j) =>
					j.groupId === updated.groupId
						? { ...j, groupName: updated.name }
						: j
				),
			};
		});
	};

	const handleRoundUpdated = (updated) => {
		setEvent((prev) => {
			if (!prev) return prev;
			const nextRounds = prev.rounds
				.map((r) =>
					r.roundId === updated.roundId
						? {
								...r,
								name: updated.name,
								startDate: updated.startDate,
								endDate: updated.endDate,
								submissionDeadline: updated.submissionDeadline,
								roundOrder: updated.roundOrder,
							}
						: r
				)
				.sort(
					(a, b) =>
						Number(a.roundOrder ?? 0) - Number(b.roundOrder ?? 0)
				);
			return {
				...prev,
				rounds: nextRounds,
				assignedJudges: (prev.assignedJudges ?? []).map((j) =>
					j.roundId === updated.roundId
						? { ...j, roundName: updated.name }
						: j
				),
			};
		});
	};

	const handleMentorDeleted = (assignment) => {
		setEvent((prev) => {
			if (!prev) return prev;
			const next = (prev.assignedMentors ?? []).filter(
				(m) =>
					!(
						m.roundId === assignment.roundId &&
						m.groupId === assignment.groupId &&
						m.mentorId === assignment.mentorId
					)
			);
			return { ...prev, assignedMentors: next };
		});
	};

	const handleMentorUpdated = (oldAssignment, updated) => {
		setEvent((prev) => {
			if (!prev) return prev;
			return {
				...prev,
				assignedMentors: (prev.assignedMentors ?? []).map((m) =>
					m.roundId === oldAssignment.roundId &&
					m.groupId === oldAssignment.groupId &&
					m.mentorId === oldAssignment.mentorId
						? { ...m, ...updated }
						: m
				),
			};
		});
	};

	const handleJudgeDeleted = (assignment) => {
		setEvent((prev) => {
			if (!prev) return prev;
			const next = (prev.assignedJudges ?? []).filter(
				(j) =>
					!(
						j.roundId === assignment.roundId &&
						j.groupId === assignment.groupId &&
						j.judgeId === assignment.judgeId
					)
			);
			return { ...prev, assignedJudges: next };
		});
	};

	const handleJudgeUpdated = (oldAssignment, updated) => {
		setEvent((prev) => {
			if (!prev) return prev;
			return {
				...prev,
				assignedJudges: (prev.assignedJudges ?? []).map((j) =>
					j.roundId === oldAssignment.roundId &&
					j.groupId === oldAssignment.groupId &&
					j.judgeId === oldAssignment.judgeId
						? { ...j, ...updated }
						: j
				),
			};
		});
	};

	const handleGroupDeleted = (groupId) => {
		setEvent((prev) => {
			if (!prev) return prev;
			const nextGroups = prev.groups.filter((g) => g.groupId !== groupId);
			return {
				...prev,
				groups: nextGroups,
				totalGroups: String(nextGroups.length),
				assignedMentors: (prev.assignedMentors ?? []).filter(
					(m) => m.groupId !== groupId
				),
				assignedJudges: (prev.assignedJudges ?? []).filter(
					(j) => j.groupId !== groupId
				),
			};
		});
	};

	const handleRoundDeleted = (roundId) => {
		setEvent((prev) => {
			if (!prev) return prev;
			const nextRounds = prev.rounds.filter((r) => r.roundId !== roundId);
			return {
				...prev,
				rounds: nextRounds,
				totalRounds: String(nextRounds.length),
				assignedJudges: (prev.assignedJudges ?? []).filter(
					(j) => j.roundId !== roundId
				),
			};
		});
	};

	const pendingTeamsCount = countPendingTeams(event?.teams);
	const setupWarnings = event ? buildSetupWarnings(event) : {};

	const statPopupMeta = event
		? {
				teams: {
					title: "Đội tham gia",
					count: event.totalTeams,
				},
				groups: {
					title: "Bảng thi",
					count: event.totalGroups,
				},
				rounds: {
					title: "Vòng thi",
					count: event.totalRounds,
				},
				mentors: {
					title: "Mentor",
					count: String(event.assignedMentors?.length ?? 0),
				},
				judges: {
					title: "Judge",
					count: String(event.assignedJudges?.length ?? 0),
				},
				awards: {
					title: "Giải thưởng",
					count: event.totalAwards,
				},
			}
		: {};

	function renderStatPopupBody() {
		if (!event || !statPopup) return null;
		switch (statPopup) {
			case "teams":
				return (
					<TeamsDropdownContent
						teams={event.teams}
						onUpdated={handleTeamRegistrationUpdated}
					/>
				);
			case "groups":
				return (
					<GroupsDropdownContent
						eventId={event.eventId}
						groups={event.groups}
						onGroupDeleted={handleGroupDeleted}
						onGroupUpdated={handleGroupUpdated}
					/>
				);
			case "rounds":
				return (
					<RoundsDropdownContent
						eventId={event.eventId}
						rounds={event.rounds}
						onRoundDeleted={handleRoundDeleted}
						onRoundUpdated={handleRoundUpdated}
					/>
				);
			case "mentors":
				return (
					<MentorsDropdownContent
						eventId={event.eventId}
						assignedMentors={event.assignedMentors}
						rounds={event.rounds}
						groups={event.groups}
						onUpdated={handleMentorUpdated}
						onDeleted={handleMentorDeleted}
					/>
				);
			case "judges":
				return (
					<JudgesDropdownContent
						eventId={event.eventId}
						assignedJudges={event.assignedJudges}
						rounds={event.rounds}
						groups={event.groups}
						onUpdated={handleJudgeUpdated}
						onDeleted={handleJudgeDeleted}
					/>
				);
			case "awards":
				return <AwardsDropdownContent awards={event.awards} />;
			default:
				return null;
		}
	}

	const activeStat = statPopup ? statPopupMeta[statPopup] : null;

	return (
		<DashboardShell
			roleLabel="Staff"
			title="Chi tiết sự kiện"
			subtitle="Thông tin đầy đủ của hackathon."
			role="Staff"
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
								{event.numRounds ?? 1} vòng dự kiến ·{" "}
								{formatMaxTeams(event.maxTeams)} ·{" "}
								{event.totalTeams ?? 0} đội đăng ký
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
						<StatRow
							label="Đội tham gia"
							count={event.totalTeams}
							badgeCount={pendingTeamsCount}
							onOpen={() => setStatPopup("teams")}
						/>
						<StatRow
							label="Bảng thi"
							count={event.totalGroups}
							setupWarning={setupWarnings.groups}
							onOpen={() => setStatPopup("groups")}
						/>
						<StatRow
							label="Vòng thi"
							count={event.totalRounds}
							setupWarning={setupWarnings.rounds}
							onOpen={() => setStatPopup("rounds")}
						/>
						<StatRow
							label="Mentor"
							count={String(event.assignedMentors?.length ?? 0)}
							setupWarning={setupWarnings.mentors}
							onOpen={() => setStatPopup("mentors")}
						/>
						<StatRow
							label="Judge"
							count={String(event.assignedJudges?.length ?? 0)}
							setupWarning={setupWarnings.judges}
							onOpen={() => setStatPopup("judges")}
						/>
						<StatRow
							label="Giải thưởng"
							count={event.totalAwards}
							onOpen={() => setStatPopup("awards")}
						/>
					</div>

					<Modal
						isOpen={!!statPopup}
						onClose={() => setStatPopup(null)}
						title={activeStat?.title ?? ""}
						subtitle={
							activeStat
								? `Tổng: ${activeStat.count ?? "0"}`
								: undefined
						}
						className="event-stat-modal"
					>
						<div className="event-stat-modal-body">
							{renderStatPopupBody()}
						</div>
					</Modal>

					<EventDetailInfoPanel
						event={event}
						onUpdated={handleEventUpdated}
					/>
				</div>
			)}
		</DashboardShell>
	);
}
