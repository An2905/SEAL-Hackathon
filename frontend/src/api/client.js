import { parseJwt } from '../utils/jwt'

// Dev: empty => Vite proxies /api to localhost:8080 (vite.config.js).
// Vercel / prod: set VITE_API_BASE to your backend URL (no trailing slash).
export const API_BASE = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')

export function resolveAssetUrl(path) {
  if (!path) return path
  if (/^https?:\/\//i.test(path)) return path
  return `${API_BASE}${path}`
}

export async function apiFetch(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = localStorage.getItem('hh_token')
    // FIX: Tránh gửi "Bearer null" khi chưa đăng nhập hoặc token bị lưu sai.
    if (token && token !== 'null' && token !== 'undefined') {
      headers['Authorization'] = `Bearer ${token}`
    }
  }

  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body == null ? undefined : JSON.stringify(body),
      // Keep cookies (JSESSIONID) across the two-step OTP flow.
      credentials: 'include'
    })
  } catch {
    throw new Error('NETWORK')
  }

  const text = await response.text()
  if (!response.ok) throw new Error(text || `HTTP_${response.status}`)
  return text
}

// Parse the /api/auth/login response. The BE returns JSON
// ({ "message": "Login success", "token": "<jwt>" }); we also tolerate the
// legacy plain-text form ("Login success ... Token: x Role: y"). The role is
// always read from the JWT claim to stay consistent regardless of BE shape.
export function parseLoginResponse(text) {
  const trimmed = (text || '').trim()
  let token = null

  if (trimmed.startsWith('{')) {
    // New JSON shape.
    let obj
    try {
      obj = JSON.parse(trimmed)
    } catch {
      return { ok: false, message: trimmed || 'Đăng nhập thất bại' }
    }
    token = obj.token || null
    if (!token) {
      return { ok: false, message: obj.message || 'Đăng nhập thất bại' }
    }
  } else {
    // Legacy plain-text shape.
    if (!trimmed.toLowerCase().startsWith('login success')) {
      return { ok: false, message: trimmed || 'Đăng nhập thất bại' }
    }
    const tokenMatch = trimmed.match(/Token:\s*([^\s]+)/i)
    token = tokenMatch ? tokenMatch[1].trim() : null
  }

  if (!token) {
    return { ok: false, message: 'Đăng nhập thất bại' }
  }

  const claims = parseJwt(token)
  const role = claims?.role || null

  return { ok: true, token, role, message: null }
}
