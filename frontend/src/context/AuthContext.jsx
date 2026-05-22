import { createContext, useContext, useState, useCallback } from "react";
import { parseJwt } from "../utils/jwt";

const AuthContext = createContext(null);

const STORAGE_KEYS = {
	token: "hh_token",
	email: "hh_email",
	role: "hh_role",
	fullName: "hh_full_name",
};

const ROLE_PATHS = {
	Student: "/student",
	Staff: "/staff",
	Mentor: "/mentor",
	Judge: "/judge",
};

export function AuthProvider({ children }) {
	const [auth, setAuthState] = useState(() => ({
		token: localStorage.getItem(STORAGE_KEYS.token) || "",
		email: localStorage.getItem(STORAGE_KEYS.email) || "",
		role: localStorage.getItem(STORAGE_KEYS.role) || "",
		fullName: localStorage.getItem(STORAGE_KEYS.fullName) || "",
	}));

	const saveAuth = useCallback((patch = {}) => {
		const { token, email, role, fullName } = patch;

		// When a new token comes in, prefer fullName/email decoded from token
		// (BE puts fullName claim in JWT — see JwtUtil.generateToken)
		let derivedFullName = fullName;
		let derivedEmail = email;
		if (token) {
			const claims = parseJwt(token);
			if (claims) {
				if (derivedFullName == null && claims.fullName)
					derivedFullName = claims.fullName;
				if (derivedEmail == null && claims.sub) derivedEmail = claims.sub;
			}
		}

		if (token != null) localStorage.setItem(STORAGE_KEYS.token, token);
		if (derivedEmail != null)
			localStorage.setItem(STORAGE_KEYS.email, derivedEmail);
		if (role != null) localStorage.setItem(STORAGE_KEYS.role, role);
		if (derivedFullName != null)
			localStorage.setItem(STORAGE_KEYS.fullName, derivedFullName);

		setAuthState((prev) => ({
			token: token != null ? token : prev.token,
			email: derivedEmail != null ? derivedEmail : prev.email,
			role: role != null ? role : prev.role,
			fullName: derivedFullName != null ? derivedFullName : prev.fullName,
		}));
	}, []);

	const clearAuth = useCallback(() => {
		Object.values(STORAGE_KEYS).forEach((k) => localStorage.removeItem(k));
		setAuthState({ token: "", email: "", role: "", fullName: "" });
	}, []);

	const isLoggedIn = !!auth.token;

	const pathForRole = (role) => ROLE_PATHS[role] || "/";

	return (
		<AuthContext.Provider
			value={{ auth, saveAuth, clearAuth, isLoggedIn, pathForRole }}
		>
			{children}
		</AuthContext.Provider>
	);
}

export function useAuth() {
	const ctx = useContext(AuthContext);
	if (!ctx) throw new Error("useAuth must be used within AuthProvider");
	return ctx;
}
