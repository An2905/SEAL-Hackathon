import { apiFetch } from "./client";

/** event_id DB là bigint — chuẩn hóa trước khi gửi BE */
export function normalizeEventId(value) {
	if (value == null || value === "") return "";
	const num = Number(value);
	if (Number.isFinite(num)) return String(Math.trunc(num));
	const s = String(value).trim();
	const dot = s.indexOf(".");
	if (dot > 0 && /^\d+\.0+$/.test(s)) return s.slice(0, dot);
	return s;
}

function mapEventRow(row) {
	const r = row && typeof row === "object" ? row : {};
	return {
		eventId: normalizeEventId(r.eventId ?? r.event_id),
		title: r.title ?? "",
		description: r.description ?? "",
		startDate: r.startDate ?? r.start_date ?? "",
		endDate: r.endDate ?? r.end_date ?? "",
		status: r.status ?? "",
		createdAt: r.createdAt ?? r.created_at ?? "",
	};
}

export function countPendingTeams(teams) {
	if (!Array.isArray(teams)) return "0";
	const n = teams.filter(
		(t) => String(t?.status ?? "").trim().toUpperCase() === "PENDING"
	).length;
	return String(n);
}

function mapCount(value) {
	if (value == null || value === "") return "0";
	const num = Number(value);
	return Number.isFinite(num) ? String(Math.trunc(num)) : String(value);
}

function mapCategoryRow(row) {
	return {
		categoryId: normalizeEventId(row.categoryId ?? row.category_id),
		name: row.name ?? "",
		description: row.description ?? "",
	};
}

function mapRoundRow(row) {
	return {
		roundId: normalizeEventId(row.roundId ?? row.round_id),
		name: row.name ?? "",
		startDate: row.startDate ?? row.start_date ?? "",
		endDate: row.endDate ?? row.end_date ?? "",
		submissionDeadline:
			row.submissionDeadline ?? row.submission_deadline ?? "",
	};
}

function mapTeamRow(row) {
	return {
		registrationId: normalizeEventId(row.registrationId ?? row.registration_id),
		teamId: normalizeEventId(row.teamId ?? row.team_id),
		teamName: row.teamName ?? row.team_name ?? "",
		status: row.status ?? "",
	};
}

function mapAwardRow(row) {
	return {
		awardId: normalizeEventId(row.awardId ?? row.award_id),
		title: row.title ?? "",
		rank: row.rank ?? "",
		teamName: row.teamName ?? row.team_name ?? "",
	};
}

function mapList(value, mapper) {
	return Array.isArray(value) ? value.map(mapper) : [];
}

function mapEventDetailRow(row) {
	const r = row && typeof row === "object" ? row : {};
	return {
		...mapEventRow(r),
		totalTeams: mapCount(r.totalTeams ?? r.total_teams),
		totalCategories: mapCount(r.totalCategories ?? r.total_categories),
		totalRounds: mapCount(r.totalRounds ?? r.total_rounds),
		totalAwards: mapCount(r.totalAwards ?? r.total_awards),
		teams: mapList(r.teams, mapTeamRow),
		categories: mapList(r.categories, mapCategoryRow),
		rounds: mapList(r.rounds, mapRoundRow),
		awards: mapList(r.awards, mapAwardRow),
	};
}

// GET /api/staff/events/detail?eventId=...
// Requires Authorization Bearer (any authenticated role per BE).
// Returns: { ...event fields, totalTeams, totalCategories, totalRounds, totalAwards,
//            teams[], categories[], rounds[], awards[] }
export async function getEventDetail(eventId) {
	const id = normalizeEventId(eventId);
	if (!id) throw new Error("Event ID không hợp lệ");

	const params = new URLSearchParams({ eventId: id });
	const text = await apiFetch(`/api/staff/events/detail?${params.toString()}`, {
		method: "GET",
	});
	try {
		const data = JSON.parse(text);
		const mapped = mapEventDetailRow(data);
		if (!mapped.eventId) throw new Error("Không tìm thấy sự kiện");
		return mapped;
	} catch (err) {
		if (err.message === "Không tìm thấy sự kiện") throw err;
		throw new Error(text || "Không thể tải chi tiết sự kiện");
	}
}

export async function attachPendingTeamsToEvents(events) {
	if (!Array.isArray(events) || events.length === 0) return [];

	return Promise.all(
		events.map(async (ev) => {
			try {
				const detail = await getEventDetail(ev.eventId);
				return { ...ev, pendingTeams: countPendingTeams(detail.teams) };
			} catch {
				return { ...ev, pendingTeams: "0" };
			}
		})
	);
}

// GET /api/staff/events?status=...
// status ∈ { ALL, UPCOMING, ONGOING, COMPLETED, CANCELLED } — defaults to ALL when omitted.
// Requires Authorization Bearer of a COORDINATOR.
// Returns: [{ eventId, title, description, startDate, endDate, status, createdAt? }, ...]
export async function getAllEvents(status = "ALL") {
	const params = new URLSearchParams();
	const normalizedStatus = String(status ?? "ALL").trim().toUpperCase();
	if (normalizedStatus && normalizedStatus !== "ALL") {
		params.set("status", normalizedStatus);
	}

	const query = params.toString() ? `?${params.toString()}` : "";
	const text = await apiFetch(`/api/staff/events${query}`, { method: "GET" });
	try {
		const data = JSON.parse(text);
		if (!Array.isArray(data)) return [];
		return data.map(mapEventRow);
	} catch {
		throw new Error(text || "Không thể tải danh sách sự kiện");
	}
}
