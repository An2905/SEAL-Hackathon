import { apiFetch } from "./client";

export async function getMyTeam() {
	const text = (await apiFetch("/api/team/me", { method: "GET" })).trim();
	if (text.startsWith("{")) return { hasTeam: true, data: JSON.parse(text) };
	if (/^no team$/i.test(text) || text === "") return { hasTeam: false };
	throw new Error(text);
}

export async function createTeam({ teamName }) {
	const text = await apiFetch("/api/team/create", {
		method: "PUT",
		body: { teamName },
	});
	const enrollMatch = text.match(/enrollCode:\s*(\S+)/i);
	if (!/^Added Team /i.test(text) && !enrollMatch) throw new Error(text);
	return { enrollCode: enrollMatch ? enrollMatch[1] : null };
}

export async function joinTeam({ enrollCode }) {
	const text = await apiFetch("/api/team/join", {
		method: "PUT",
		body: { enrollCode },
	});
	if (!/join team successfully/i.test(text)) throw new Error(text);
	return true;
}

export async function joinEvent({ eventId, categoryId }) {
	const text = await apiFetch("/api/team/join-event", {
		method: "PUT",
		body: { eventId, categoryId },
	});
	if (!/join event successfully/i.test(text)) throw new Error(text);
	return true;
}

export async function deleteMember({ memberId }) {
	const text = await apiFetch("/api/team/delete-member", {
		method: "DELETE",
		body: { memberId },
	});
	if (!/delete team member successfully/i.test(text)) throw new Error(text);
	return true;
}
