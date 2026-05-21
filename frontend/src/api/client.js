// Empty base => requests go to FE origin (e.g. http://localhost:5174)
// and Vite dev server proxies "/api/*" to http://localhost:8080.
// This makes the call same-origin from the browser's point of view,
// so the JSESSIONID cookie used by the OTP flow is preserved automatically.
const API_BASE = "";

export async function apiFetch(
	path,
	{ method = "GET", body, auth = true } = {}
) {
	const headers = { "Content-Type": "application/json" };
	if (auth) {
		const token = localStorage.getItem("hh_token");
		if (token) headers["Authorization"] = `Bearer ${token}`;
	}

	let response;
	try {
		response = await fetch(`${API_BASE}${path}`, {
			method,
			headers,
			body: body == null ? undefined : JSON.stringify(body),
			// Keep cookies (JSESSIONID) across the two-step OTP flow.
			credentials: "include",
		});
	} catch {
		throw new Error("NETWORK");
	}

	const text = await response.text();
	if (!response.ok) throw new Error(text || `HTTP_${response.status}`);
	return text;
}

export function parseLoginResponse(text) {
	const trimmed = (text || "").trim();
	if (!trimmed.toLowerCase().startsWith("login success")) {
		return { ok: false, message: trimmed || "Đăng nhập thất bại" };
	}
	const tokenMatch = trimmed.match(/Token:\s*([^\s]+)/i);
	const roleMatch = trimmed.match(/Role:\s*([^\n\r]+)/i);
	return {
		ok: true,
		token: tokenMatch ? tokenMatch[1].trim() : null,
		role: roleMatch ? roleMatch[1].trim() : null,
	};
}
