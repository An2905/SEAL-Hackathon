import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { parseJwt, isTokenExpired } from '../utils/jwt'
import { ROLE_UI_LABELS } from '../utils/roleLabels'

const AuthContext = createContext(null)

const STORAGE_KEYS = {
  token: 'hh_token',
  email: 'hh_email',
  role: 'hh_role',
  fullName: 'hh_full_name'
}

const ROLE_PATHS = {
  COORDINATOR: '/staff',
  EXPERT_INTERNAL: '/mentor',
  EXPERT_EXTERNAL: '/mentor',
  STUDENT_FPT: '/student',
  STUDENT_EXTERNAL: '/student'
}

const ROLE_DISPLAY_LABELS = {
  COORDINATOR: 'Trang Staff',
  EXPERT_INTERNAL: 'Trang Khách (INTERNAL)',
  EXPERT_EXTERNAL: 'Trang Khách (EXTERNAL)',
  STUDENT_FPT: 'Trang Sinh viên',
  STUDENT_EXTERNAL: 'Trang Sinh viên'
}

function clearStorage() {
  Object.values(STORAGE_KEYS).forEach((k) => localStorage.removeItem(k))
}

function hydrateAuthFromToken(token, stored = {}) {
  const claims = token ? parseJwt(token) : null
  let role = stored.role || ''
  let email = stored.email || ''
  let fullName = stored.fullName || ''

  if (claims) {
    if (!role && claims.role) role = claims.role
    if (!email && claims.sub) email = claims.sub
    if (!fullName && claims.fullName) fullName = claims.fullName
  }

  if (role) localStorage.setItem(STORAGE_KEYS.role, role)
  if (email) localStorage.setItem(STORAGE_KEYS.email, email)
  if (fullName) localStorage.setItem(STORAGE_KEYS.fullName, fullName)

  return { token, email, role, fullName }
}

function readInitialAuth() {
  const token = localStorage.getItem(STORAGE_KEYS.token) || ''
  if (token && isTokenExpired(token)) {
    clearStorage()
    return { token: '', email: '', role: '', fullName: '', phone: '', avatarUrl: '' }
  }

  const hydrated = hydrateAuthFromToken(token, {
    email: localStorage.getItem(STORAGE_KEYS.email) || '',
    role: localStorage.getItem(STORAGE_KEYS.role) || '',
    fullName: localStorage.getItem(STORAGE_KEYS.fullName) || ''
  })

  return {
    token: hydrated.token,
    email: hydrated.email,
    role: hydrated.role,
    fullName: hydrated.fullName,
    phone: '',
    avatarUrl: ''
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuthState] = useState(readInitialAuth)

  const saveAuth = useCallback((patch = {}) => {
    const { token, email, role, fullName } = patch

    let derivedFullName = fullName
    let derivedEmail = email
    let derivedRole = role

    if (token) {
      const claims = parseJwt(token)
      if (claims) {
        if (derivedFullName == null && claims.fullName) derivedFullName = claims.fullName
        if (derivedEmail == null && claims.sub) derivedEmail = claims.sub
        if (derivedRole == null && claims.role) derivedRole = claims.role
      }
    }

    if (token != null) localStorage.setItem(STORAGE_KEYS.token, token)
    if (derivedEmail != null) localStorage.setItem(STORAGE_KEYS.email, derivedEmail)
    if (derivedRole != null) localStorage.setItem(STORAGE_KEYS.role, derivedRole)
    if (derivedFullName != null) localStorage.setItem(STORAGE_KEYS.fullName, derivedFullName)

    setAuthState((prev) => ({
      token: token != null ? token : prev.token,
      email: derivedEmail != null ? derivedEmail : prev.email,
      role: derivedRole != null ? derivedRole : prev.role,
      fullName: derivedFullName != null ? derivedFullName : prev.fullName
    }))
  }, [])

  const clearAuth = useCallback(() => {
    clearStorage()
    setAuthState({ token: '', email: '', role: '', fullName: '', phone: '', avatarUrl: '' })
  }, [])

  // Auto-logout khi apiFetch phát hiện token hết hạn giữa phiên
  useEffect(() => {
    const handler = () => clearAuth()
    window.addEventListener('auth:token-expired', handler)
    return () => window.removeEventListener('auth:token-expired', handler)
  }, [clearAuth])

  const isLoggedIn = !!auth.token && !isTokenExpired(auth.token)

  const pathForRole = (role) => ROLE_PATHS[role] || '/'

  const labelForRole = (role) => ROLE_DISPLAY_LABELS[role] || 'Vào dashboard'

  const pillLabelForRole = (role) => (role && role in ROLE_UI_LABELS ? ROLE_UI_LABELS[role] : null)

  return (
    <AuthContext.Provider
      value={{ auth, saveAuth, clearAuth, isLoggedIn, pathForRole, labelForRole, pillLabelForRole }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
