import { createContext, useContext, useState, useCallback } from "react";

const AuthContext = createContext(null);

const STORAGE_KEYS = {
	token: "hh_token",
	email: "hh_email",
	role: "hh_role",
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
	}));

	// FIX: use functional update to merge with current state, avoid stale localStorage reads
	const saveAuth = useCallback(({ token, email, role } = {}) => {
		if (token != null) localStorage.setItem(STORAGE_KEYS.token, token);
		if (email != null) localStorage.setItem(STORAGE_KEYS.email, email);
		if (role != null) localStorage.setItem(STORAGE_KEYS.role, role);
		setAuthState((prev) => ({
			token: token != null ? token : prev.token,
			email: email != null ? email : prev.email,
			role: role != null ? role : prev.role,
		}));
	}, []);

	const clearAuth = useCallback(() => {
		Object.values(STORAGE_KEYS).forEach((k) => localStorage.removeItem(k));
		setAuthState({ token: "", email: "", role: "" });
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
