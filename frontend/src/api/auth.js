import { apiFetch, parseLoginResponse } from './client'

export async function login({ email, password, captchaToken }) {
  const text = await apiFetch('/api/auth/login', {
    method: 'POST',
    body: { email, password, captchaToken },
    auth: false
  })
  return parseLoginResponse(text)
}

// Step 1 of register: send registration form, BE emails an OTP and stores
// the pending data in HttpSession until verifyAndRegister is called.
// Note: Requires JSESSIONID cookie to be preserved between step 1 and step 2.
// If user's browser blocks cookies, step 2 will fail with a session error.
export async function sendRegisterOtp({ fullName, email, university, studentId, password, captchaToken }) {
  const text = await apiFetch('/api/auth/register/otp', {
    method: 'POST',
    body: { fullName, email, university, studentId, password, captchaToken },
    auth: false
  })
  if (!/otp sent/i.test(text)) throw new Error(text)
  return true
}

// Step 2 of register: confirm the OTP. BE pulls the pending data from session
// and creates the account.
// Note: If this fails with a session/OTP error, user may need to restart from step 1.
export async function verifyAndRegister({ email, otp }) {
  const text = await apiFetch('/api/auth/register', {
    method: 'POST',
    body: { email, otp },
    auth: false
  })
  if (!/registration successful/i.test(text)) throw new Error(text)
  return true
}

// Step 1 of password reset: BE generates an OTP, emails it, and stores it
// in HttpSession with a 5-minute expiry.
export async function sendResetPasswordOtp({ email }) {
  const text = await apiFetch('/api/auth/password/reset-otp', {
    method: 'POST',
    body: { email },
    auth: false
  })
  if (!/otp sent/i.test(text)) throw new Error(text)
  return true
}

// Step 2 of password reset: confirm the OTP and set a new password. BE
// invalidates the session on success.
export async function verifyAndResetPassword({ email, otp, newPassword }) {
  const text = await apiFetch('/api/auth/password/reset', {
    method: 'POST',
    body: { email, otp, newPassword },
    auth: false
  })
  if (!/password reset successfully/i.test(text)) throw new Error(text)
  return true
}

export async function getProfile() {
  const text = await apiFetch('/api/auth/profile')
  try {
    return JSON.parse(text)
  } catch {
    throw new Error('Không đọc được thông tin hồ sơ từ máy chủ.')
  }
}

export async function updateProfile({ fullName, email, university, studentId, phone, avatarUrl }) {
  const body = { fullName, university, studentId, phone }
  const trimmedEmail = (email ?? '').trim()
  if (trimmedEmail) {
    body.email = trimmedEmail
  }
  const trimmedAvatar = (avatarUrl ?? '').trim()
  if (trimmedAvatar) {
    body.avatarUrl = trimmedAvatar
  }

  const text = await apiFetch('/api/auth/profile', {
    method: 'PUT',
    body
  })

  // Backend trả JSON: {"message":"Profile updated successfully.","newToken":"eyJ..."}
  let parsed
  try {
    parsed = JSON.parse(text)
  } catch {
    parsed = null
  }

  const newToken = parsed?.newToken?.trim() || null
  if (!newToken) {
    throw new Error('Cập nhật thành công nhưng không nhận được token mới — vui lòng đăng nhập lại.')
  }

  return { newToken }
}

export async function updatePassword({ oldPassword, newPassword, confirmPassword }) {
  const text = await apiFetch('/api/auth/password', {
    method: 'PUT',
    body: { oldPassword, newPassword, confirmPassword }
  })
  if (!/password updated successfully/i.test(text)) throw new Error(text)
  return true
}

export async function getGithubLinkUrl(prompt) {
  const url = prompt ? `/api/auth/github/link-url?prompt=${encodeURIComponent(prompt)}` : '/api/auth/github/link-url'
  const text = await apiFetch(url)
  let parsed
  try {
    parsed = JSON.parse(text)
  } catch {
    throw new Error('Không đọc được URL GitHub OAuth từ máy chủ.')
  }
  const authorizeUrl = (parsed?.authorizeUrl || '').trim()
  if (!authorizeUrl) {
    throw new Error('Máy chủ chưa trả về URL GitHub OAuth hợp lệ.')
  }
  return authorizeUrl
}

export async function getGithubLinkStatus() {
  const text = await apiFetch('/api/auth/github/status', { method: 'GET' })
  let parsed
  try {
    parsed = JSON.parse(text)
  } catch {
    throw new Error('Không đọc được trạng thái GitHub từ máy chủ.')
  }
  return {
    githubLinked: Boolean(parsed.githubLinked ?? parsed.github_linked),
    githubUsername: String(parsed.githubUsername ?? parsed.github_username ?? '').trim()
  }
}
