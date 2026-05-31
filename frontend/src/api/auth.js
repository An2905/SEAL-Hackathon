import { apiFetch, parseLoginResponse } from "./client";

export async function login({ email, password }) {
	const text = await apiFetch("/api/auth/login", {
		method: "POST",
		body: { email, password },
		auth: false,
	});
	return parseLoginResponse(text);
}

// Step 1 of register: send registration form, BE emails an OTP and stores
// the pending data in HttpSession until verifyAndRegister is called.
export async function sendRegisterOtp({
	fullName,
	email,
	university,
	studentId,
	password,
}) {
	const text = await apiFetch("/api/auth/register/otp", {
		method: "POST",
		body: { fullName, email, university, studentId, password },
		auth: false,
	});
	if (!/otp sent/i.test(text)) throw new Error(text);
	return true;
}

// Step 2 of register: confirm the OTP. BE pulls the pending data from session
// and creates the account.
export async function verifyAndRegister({ email, otp }) {
	const text = await apiFetch("/api/auth/register", {
		method: "POST",
		body: { email, otp },
		auth: false,
	});
	if (!/registration successful/i.test(text)) throw new Error(text);
	return true;
}

// Step 1 of password reset: BE generates an OTP, emails it, and stores it
// in HttpSession with a 5-minute expiry.
export async function sendResetPasswordOtp({ email }) {
	const text = await apiFetch("/api/auth/password/reset-otp", {
		method: "POST",
		body: { email },
		auth: false,
	});
	if (!/otp sent/i.test(text)) throw new Error(text);
	return true;
}

// Step 2 of password reset: confirm the OTP and set a new password. BE
// invalidates the session on success.
export async function verifyAndResetPassword({ email, otp, newPassword }) {
	const text = await apiFetch("/api/auth/password/reset", {
		method: "POST",
		body: { email, otp, newPassword },
		auth: false,
	});
	if (!/password reset successfully/i.test(text)) throw new Error(text);
	return true;
}

export async function updateProfile({ fullName, email, university, studentId }) {
	const text = await apiFetch("/api/auth/profile", {
		method: "PUT",
		body: { fullName, email, university, studentId },
	});
	if (!/profile updated successfully/i.test(text)) throw new Error(text);
	const tokenMatch = text.match(/New Token:\s*([^\s]+)/i);
	return { newToken: tokenMatch ? tokenMatch[1].trim() : null };
}

export async function updatePassword({
	oldPassword,
	newPassword,
	confirmPassword,
}) {
	const text = await apiFetch("/api/auth/password", {
		method: "PUT",
		body: { oldPassword, newPassword, confirmPassword },
	});
	if (!/password updated successfully/i.test(text)) throw new Error(text);
	return true;
}
