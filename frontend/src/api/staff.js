import { apiFetch } from "./client";

/** user_id DB là bigint — chuẩn hóa trước khi gửi BE */
export function normalizeAccountUserId(value) {
	if (value == null || value === "") return "";
	const num = Number(value);
	if (Number.isFinite(num)) return String(Math.trunc(num));
	const s = String(value).trim();
	const dot = s.indexOf(".");
	if (dot > 0 && /^\d+\.0+$/.test(s)) return s.slice(0, dot);
	return s;
}

function mapAccountRow(row) {
	return {
		userId: normalizeAccountUserId(row.userId ?? row.user_id),
		email: row.email ?? "",
		fullName: row.fullName ?? row.full_name ?? "",
		role: row.role ?? "",
		status: row.status ?? "",
	};
}

// POST /api/staff/register
// Body: { email, fullName, role } — role ∈ { JUDGE, MENTOR }
// Requires Authorization Bearer of a COORDINATOR.
// Backend returns plain text: success line or one of the validation errors.
export async function createStaffAccount({ email, fullName, role }) {
	const text = await apiFetch("/api/staff/register", {
		method: "POST",
		body: { email, fullName, role },
	});
	if (!/account created.*email sent successfully/i.test(text))
		throw new Error(text);
	return true;
}

// PUT /api/staff/events/status
// Body: { eventId, newStatus }
// Requires Authorization Bearer of a COORDINATOR.
export async function changeEventStatus({ eventId, newStatus }) {
	const text = await apiFetch("/api/staff/events/status", {
		method: "PUT",
		body: { eventId, newStatus },
	});
	if (!/event status updated successfully/i.test(text))
		throw new Error(text);
	return true;
}

// GET /api/staff/accounts?role=...&input=...
// role ∈ { ALL, JUDGE_INTERNAL, MENTOR, STUDENT_FPT, STUDENT_EXTERNAL } — defaults to ALL when omitted.
// input searches by the backend-supported account fields.
// Requires Authorization Bearer of a COORDINATOR.
// Returns: [{ userId, email, fullName, role, status }, ...]
// PUT /api/staff/change-status
// Body: { userId, status } — status ∈ { PENDING, APPROVED, REJECTED }
// Requires Authorization Bearer of a COORDINATOR.
export async function changeAccountStatus({ userId, status }) {
	const id = normalizeAccountUserId(userId);
	if (!id || !/^\d+$/.test(id)) {
		throw new Error("User ID không hợp lệ — vui lòng tải lại danh sách tài khoản");
	}
	const nextStatus = String(status ?? "").trim().toUpperCase();
	const text = await apiFetch("/api/staff/change-status", {
		method: "PUT",
		body: { userId: id, status: nextStatus },
	});
	if (!/account status updated successfully/i.test(text))
		throw new Error(text);
	return true;
}

export async function getAllAccounts(role = "ALL", input = "") {
	const params = new URLSearchParams();
	if (role && role !== "ALL") params.set("role", role);
	const normalizedInput = input.trim();
	if (normalizedInput) params.set("input", normalizedInput);

	const query = params.toString() ? `?${params.toString()}` : "";
	const text = await apiFetch(`/api/staff/accounts${query}`, { method: "GET" });
	try {
		const data = JSON.parse(text);
		if (!Array.isArray(data)) return [];
		return data.map(mapAccountRow);
	} catch {
		throw new Error(text || "Không thể tải danh sách tài khoản");
	}
}
