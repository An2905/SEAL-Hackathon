import { createContext, useContext, useState, useCallback } from 'react'
import { parseJwt } from '../utils/jwt'
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

export function AuthProvider({ children }) {
  const [auth, setAuthState] = useState(() => ({
    token: localStorage.getItem(STORAGE_KEYS.token) || '',
    email: localStorage.getItem(STORAGE_KEYS.email) || '',
    role: localStorage.getItem(STORAGE_KEYS.role) || '',
    fullName: localStorage.getItem(STORAGE_KEYS.fullName) || ''
  }))

  const saveAuth = useCallback((patch = {}) => {
    const { token, email, role, fullName } = patch

    let derivedFullName = fullName
    let derivedEmail = email
    if (token) {
      const claims = parseJwt(token)
      if (claims) {
        if (derivedFullName == null && claims.fullName) derivedFullName = claims.fullName
        if (derivedEmail == null && claims.sub) derivedEmail = claims.sub
      }
    }

    if (token != null) localStorage.setItem(STORAGE_KEYS.token, token)
    if (derivedEmail != null) localStorage.setItem(STORAGE_KEYS.email, derivedEmail)
    if (role != null) localStorage.setItem(STORAGE_KEYS.role, role)
    if (derivedFullName != null) localStorage.setItem(STORAGE_KEYS.fullName, derivedFullName)

    setAuthState((prev) => ({
      token: token != null ? token : prev.token,
      email: derivedEmail != null ? derivedEmail : prev.email,
      role: role != null ? role : prev.role,
      fullName: derivedFullName != null ? derivedFullName : prev.fullName
    }))
  }, [])

  const clearAuth = useCallback(() => {
    Object.values(STORAGE_KEYS).forEach((k) => localStorage.removeItem(k))
    setAuthState({ token: '', email: '', role: '', fullName: '' })
  }, [])

  const isLoggedIn = !!auth.token

  const pathForRole = (role) => ROLE_PATHS[role] || '/'

  const labelForRole = (role) => ROLE_DISPLAY_LABELS[role] || 'Vào dashboard'

  const pillLabelForRole = (role) => (role && role in ROLE_UI_LABELS ? ROLE_UI_LABELS[role] : null)

  return (
    <AuthContext.Provider value={{ auth, saveAuth, clearAuth, isLoggedIn, pathForRole, labelForRole, pillLabelForRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
