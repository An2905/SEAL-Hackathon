import { parseJwt, isTokenExpired } from '../utils/jwt'

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
    // Tránh gửi "Bearer null" khi chưa đăng nhập hoặc token bị lưu sai.
    if (token && token !== 'null' && token !== 'undefined') {
      if (isTokenExpired(token)) {
        window.dispatchEvent(new Event('auth:token-expired'))
        throw new Error('TOKEN_EXPIRED')
      }
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
  if (!response.ok) {
    // BE errors come back as JSON ({status, message, timestamp, errors}) via
    // GlobalExceptionHandler — extract the plain message so localizeError() can
    // match it against ERROR_MAP instead of showing the raw JSON blob.
    let message = text
    if (text) {
      try {
        const parsed = JSON.parse(text)
        if (parsed && typeof parsed.message === 'string' && parsed.message) {
          message = parsed.message
        }
      } catch {
        // Not JSON (e.g. plain-text error) — keep raw text.
      }
    }
    throw new Error(message || `HTTP_${response.status}`)
  }
  return text
}

/** Extract `message` from MessageResponse JSON or return plain text. */
export function parseMessageResponse(text) {
  if (!text) return ''
  try {
    const parsed = JSON.parse(text)
    if (parsed && typeof parsed.message === 'string') return parsed.message
  } catch {
    // Plain-text response — keep raw text.
  }
  return text
}

/** Binary download with the same auth/base URL rules as apiFetch. */
export async function apiFetchBlob(path, { method = 'GET' } = {}) {
  const headers = {}
  const token = localStorage.getItem('hh_token')
  if (token && token !== 'null' && token !== 'undefined') {
    if (isTokenExpired(token)) {
      window.dispatchEvent(new Event('auth:token-expired'))
      throw new Error('TOKEN_EXPIRED')
    }
    headers['Authorization'] = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      credentials: 'include'
    })
  } catch {
    throw new Error('NETWORK')
  }

  if (!response.ok) {
    const text = await response.text()
    let message = text
    if (text) {
      try {
        const parsed = JSON.parse(text)
        if (parsed && typeof parsed.message === 'string' && parsed.message) {
          message = parsed.message
        }
      } catch {
        // keep raw text
      }
    }
    throw new Error(message || `HTTP_${response.status}`)
  }

  return response.blob()
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
