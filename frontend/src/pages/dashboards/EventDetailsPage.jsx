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
	deleteEventCategory,
	deleteEventRound,
	getEventRoundDetail,
	updateEvent,
	updateEventCategory,
	updateEventRound,
} from "../../api/staffEventSetup";
import FormField from "../../components/common/FormField";
import LoadingButton from "../../components/common/LoadingButton";
import { useToast } from "../../context/ToastContext";
import { localizeError } from "../../utils/errors";

const REGISTRATION_STATUSES = ["PENDING", "APPROVED", "REJECTED"];
const EVENT_STATUSES = ["UPCOMING", "ONGOING", "COMPLETED"];

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

function StatRow({ label, count, badgeCount, onOpen }) {
	return (
		<div className="event-stat-row">
			<PendingTeamsBadge count={badgeCount} />
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

function CategoryStatItem({ eventId, cat, onUpdated, onDeleted }) {
	const { showToast } = useToast();
	const [editOpen, setEditOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const [form, setForm] = useState({
		name: cat.name || "",
		description: cat.description || "",
	});

	useEffect(() => {
		if (!editOpen) {
			setForm({ name: cat.name || "", description: cat.description || "" });
		}
	}, [cat.name, cat.description, editOpen]);

	const handleChange = (e) =>
		setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

	const handleSave = async (e) => {
		e.preventDefault();
		setSaving(true);
		try {
			const updated = await updateEventCategory({
				eventId,
				categoryId: cat.categoryId,
				name: form.name,
				description: form.description,
			});
			onUpdated?.(updated);
			showToast("Đã cập nhật track", "success");
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
					<div style={{ fontWeight: 600 }}>{cat.name || "—"}</div>
					{cat.description ? (
						<div style={{ fontSize: 11, color: "var(--text-dim)" }}>
							{cat.description}
						</div>
					) : null}
					<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
						ID: {cat.categoryId} (không đổi)
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
						itemLabel={cat.name || "track"}
						onDelete={async () => {
							await deleteEventCategory({
								eventId,
								categoryId: cat.categoryId,
							});
							onDeleted?.(cat.categoryId);
						}}
					/>
				</div>
			</div>
			{editOpen ? (
				<form className="event-stat-item-edit" onSubmit={handleSave}>
					<FormField label="Tên track *">
						<input
							name="name"
							value={form.name}
							onChange={handleChange}
							maxLength={100}
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
						{formatEventDateTime(round.startDate)} →{" "}
						{formatEventDateTime(round.endDate)}
					</div>
					<div style={{ fontSize: 11, color: "var(--text-mute)" }}>
						Deadline: {formatEventDateTime(round.submissionDeadline)}
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
							<p className="event-stat-edit-hint">
								ID vòng: {round.roundId} (IDENTITY — không đổi)
							</p>
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

function CategoriesDropdownContent({
	eventId,
	categories,
	onCategoryDeleted,
	onCategoryUpdated,
}) {
	return (
		<>
			{!categories.length ? (
				<div className="event-stat-dropdown-empty">Chưa có category nào.</div>
			) : (
				<div className="kv-list">
					{categories.map((cat) => (
						<CategoryStatItem
							key={cat.categoryId}
							eventId={eventId}
							cat={cat}
							onUpdated={onCategoryUpdated}
							onDeleted={onCategoryDeleted}
						/>
					))}
				</div>
			)}
			<SetupPanelLink eventId={eventId} focus="category">
				+ Thêm category
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
	categories,
	onUpdated,
	onDeleted,
}) {
	const { showToast } = useToast();
	const [editOpen, setEditOpen] = useState(false);
	const [saving, setSaving] = useState(false);
	const [loadingAccounts, setLoadingAccounts] = useState(false);
	const [mentorAccounts, setMentorAccounts] = useState([]);
	const [form, setForm] = useState({
		categoryId: assignment.categoryId || "",
		mentorId: assignment.mentorId || "",
	});

	const handleStartEdit = async () => {
		setForm({
			categoryId: assignment.categoryId || "",
			mentorId: assignment.mentorId || "",
		});
		setEditOpen(true);
		setLoadingAccounts(true);
		try {
			setMentorAccounts(await getAllAccounts("MENTOR"));
		} catch (err) {
			showToast(localizeError(err.message), "error");
			setEditOpen(false);
		} finally {
			setLoadingAccounts(false);
		}
	};

	const handleCancel = () => {
		setForm({
			categoryId: assignment.categoryId || "",
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
				categoryId: assignment.categoryId,
				mentorId: assignment.mentorId,
				newCategoryId: form.categoryId,
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
						Track: {assignment.categoryName || "—"}
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
								categoryId: assignment.categoryId,
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
							<FormField label="Track *">
								<select
									name="categoryId"
									value={form.categoryId}
									onChange={(e) =>
										setForm((f) => ({
											...f,
											categoryId: e.target.value,
										}))
									}
									disabled={saving || !categories.length}
									required
								>
									<option value="">— Chọn track —</option>
									{categories.map((c) => (
										<option key={c.categoryId} value={c.categoryId}>
											{c.name} (#{c.categoryId})
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
											{m.fullName || m.email} (#{m.userId})
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
	categories,
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
		categoryId: assignment.categoryId || "",
	});

	const handleStartEdit = async () => {
		setForm({
			judgeId: assignment.judgeId || "",
			roundId: assignment.roundId || "",
			categoryId: assignment.categoryId || "",
		});
		setEditOpen(true);
		setLoadingAccounts(true);
		try {
			setJudgeAccounts(await getAllAccounts("JUDGE_INTERNAL"));
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
			categoryId: assignment.categoryId || "",
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
				categoryId: assignment.categoryId,
				newJudgeId: form.judgeId,
				newRoundId: form.roundId,
				newCategoryId: form.categoryId,
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
						Vòng: {assignment.roundName || "—"} · Track:{" "}
						{assignment.categoryName || "—"}
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
								categoryId: assignment.categoryId,
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
										setForm((f) => ({ ...f, roundId: e.target.value }))
									}
									disabled={saving || !rounds.length}
									required
								>
									<option value="">— Chọn vòng —</option>
									{rounds.map((r) => (
										<option key={r.roundId} value={r.roundId}>
											{r.name} (#{r.roundId})
										</option>
									))}
								</select>
							</FormField>
							<FormField label="Track *">
								<select
									name="categoryId"
									value={form.categoryId}
									onChange={(e) =>
										setForm((f) => ({
											...f,
											categoryId: e.target.value,
										}))
									}
									disabled={saving || !categories.length}
									required
								>
									<option value="">— Chọn track —</option>
									{categories.map((c) => (
										<option key={c.categoryId} value={c.categoryId}>
											{c.name} (#{c.categoryId})
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
											{j.fullName || j.email} (#{j.userId})
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
	categories = [],
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
							key={`${m.categoryId}-${m.mentorId}`}
							eventId={eventId}
							assignment={m}
							categories={categories}
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
	categories = [],
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
							key={`${j.roundId}-${j.categoryId}-${j.judgeId}`}
							eventId={eventId}
							assignment={j}
							rounds={rounds}
							categories={categories}
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
		status: (event.status || "UPCOMING").toUpperCase(),
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
				startDate: form.startDate,
				endDate: form.endDate,
				status: form.status,
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
				) : (
					<form
						id={editFormId}
						className="event-stat-item-edit event-detail-info-edit"
						onSubmit={handleConfirm}
					>
						<div className="kv-list event-detail-info-readonly">
							<div className="kv">
								<span>Event ID</span>
								<span>{event.eventId}</span>
							</div>
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
						<FormField label="Ngày bắt đầu *">
							<input
								type="datetime-local"
								name="startDate"
								value={form.startDate}
								onChange={handleChange}
								disabled={saving}
								required
							/>
						</FormField>
						<FormField label="Ngày kết thúc *">
							<input
								type="datetime-local"
								name="endDate"
								value={form.endDate}
								onChange={handleChange}
								disabled={saving}
								required
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
				createdAt: updated.createdAt || prev.createdAt,
			};
		});
	};

	const handleCategoryUpdated = (updated) => {
		setEvent((prev) => {
			if (!prev) return prev;
			return {
				...prev,
				categories: prev.categories.map((c) =>
					c.categoryId === updated.categoryId
						? {
								...c,
								name: updated.name,
								description: updated.description ?? "",
							}
						: c
				),
				assignedMentors: (prev.assignedMentors ?? []).map((m) =>
					m.categoryId === updated.categoryId
						? { ...m, categoryName: updated.name }
						: m
				),
				assignedJudges: (prev.assignedJudges ?? []).map((j) =>
					j.categoryId === updated.categoryId
						? { ...j, categoryName: updated.name }
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
						m.categoryId === assignment.categoryId &&
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
					m.categoryId === oldAssignment.categoryId &&
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
						j.categoryId === assignment.categoryId &&
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
					j.categoryId === oldAssignment.categoryId &&
					j.judgeId === oldAssignment.judgeId
						? { ...j, ...updated }
						: j
				),
			};
		});
	};

	const handleCategoryDeleted = (categoryId) => {
		setEvent((prev) => {
			if (!prev) return prev;
			const nextCategories = prev.categories.filter(
				(c) => c.categoryId !== categoryId
			);
			return {
				...prev,
				categories: nextCategories,
				totalCategories: String(nextCategories.length),
				assignedMentors: (prev.assignedMentors ?? []).filter(
					(m) => m.categoryId !== categoryId
				),
				assignedJudges: (prev.assignedJudges ?? []).filter(
					(j) => j.categoryId !== categoryId
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

	const statPopupMeta = event
		? {
				teams: {
					title: "Đội tham gia",
					count: event.totalTeams,
				},
				categories: {
					title: "Categories",
					count: event.totalCategories,
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
			case "categories":
				return (
					<CategoriesDropdownContent
						eventId={event.eventId}
						categories={event.categories}
						onCategoryDeleted={handleCategoryDeleted}
						onCategoryUpdated={handleCategoryUpdated}
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
						categories={event.categories}
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
						categories={event.categories}
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
						<StatRow
							label="Đội tham gia"
							count={event.totalTeams}
							badgeCount={pendingTeamsCount}
							onOpen={() => setStatPopup("teams")}
						/>
						<StatRow
							label="Categories"
							count={event.totalCategories}
							onOpen={() => setStatPopup("categories")}
						/>
						<StatRow
							label="Vòng thi"
							count={event.totalRounds}
							onOpen={() => setStatPopup("rounds")}
						/>
						<StatRow
							label="Mentor"
							count={String(event.assignedMentors?.length ?? 0)}
							onOpen={() => setStatPopup("mentors")}
						/>
						<StatRow
							label="Judge"
							count={String(event.assignedJudges?.length ?? 0)}
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
