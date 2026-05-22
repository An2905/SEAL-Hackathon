export function parseJwt(token) {
	if (!token || typeof token !== "string") return null;
	const parts = token.split(".");
	if (parts.length < 2) return null;
	try {
		const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
		const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
		const json = decodeURIComponent(
			atob(padded)
				.split("")
				.map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
				.join("")
		);
		return JSON.parse(json);
	} catch {
		return null;
	}
}
