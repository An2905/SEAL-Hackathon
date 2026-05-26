import { apiFetch } from "./client";

// GET /api/universities/all -> [{ universityId, universityName }, ...]
export async function getAllUniversities() {
	const text = await apiFetch("/api/universities/all", {
		method: "GET",
		auth: false,
	});
	try {
		const data = JSON.parse(text);
		return Array.isArray(data) ? data : [];
	} catch {
		throw new Error(text || "Cannot load universities");
	}
}
